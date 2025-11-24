package com.example.demo.portfolio.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ai.portfolioguide.PortfolioGuideAgent;
import com.example.demo.member.dao.MemberDao;
import com.example.demo.portfolio.dao.PortfolioGuideDao;
import com.example.demo.portfolio.dao.PortfolioStandardDao;
import com.example.demo.portfolio.dto.PortfolioGuideResult;
import com.example.demo.portfolio.dto.request.GuideItemSaveRequest;
import com.example.demo.portfolio.dto.request.GuideProgressSaveRequest;
import com.example.demo.portfolio.dto.request.PortfolioGuideRequest;
import com.example.demo.portfolio.dto.response.GuideProgressResponse;
import com.example.demo.portfolio.entity.PortfolioGuide;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PortfolioGuideService {

    
    private final PortfolioGuideAgent portfolioGuideAgent;
  
  
  
@Autowired
  private PortfolioGuideDao portfolioGuideDao;
  
  @Autowired
  private PortfolioStandardDao portfolioStandardDao;

  @Autowired
  private MemberDao memberDao;

  @Autowired
  private ObjectMapper objectMapper;
  

    public PortfolioGuideService(
        PortfolioGuideDao portfolioGuideDao,
        PortfolioStandardDao portfolioStandardDao,
        MemberDao memberDao,
        ObjectMapper objectMapper,
        PortfolioGuideAgent portfolioGuideAgent
       ){
        this.portfolioGuideDao=portfolioGuideDao;
        this.portfolioStandardDao =portfolioStandardDao;
        this.memberDao=memberDao;
        this.objectMapper = objectMapper;
        this.portfolioGuideAgent = portfolioGuideAgent;
    }


  //-------------메인 코칭 메서드
  public PortfolioGuideResult provideCoaching(PortfolioGuideRequest request){
    LocalDateTime startTime= LocalDateTime.now();

    log.info("📋 포트폴리오 가이드 코칭 시작 - guideId: {}, memberId: {}, 단계: {}, 필드: {}, 입력: '{}'", 
        request.getGuideId(), 
        request.getMemberId(),
        request.getCurrentStep(), 
        request.getInputFieldType(),
        request.getUserInput() != null ? request.getUserInput().substring(0, Math.min(30, request.getUserInput().length())) : "null");
    
    // PortfolioGuideAgent가 모든 DAO 조회와 LLM 호출을 담당
    PortfolioGuideResult result = portfolioGuideAgent.evaluate(request);
    log.debug("AI 코칭 결과 생성 완료 - 점수: {}, 성공: {}", 
        result.getAppropriatenessScore(), result.isSuccess());

    processCoachingResult(result, startTime);

    // 가이드 ID 확보 (없으면 자동 생성)
    Integer guideId = request.getGuideId();
    if (guideId == null && request.getMemberId() != null) {
        log.info("🆕 guideId가 없어서 새 가이드 자동 생성 - memberId: {}", request.getMemberId());
        PortfolioGuide newGuide = getOrCreateGuide(request);
        guideId = newGuide.getGuideId();
        log.info("✅ 새 가이드 생성 완료 - guideId: {}", guideId);
    }

    // AI 피드백 저장
    if (guideId != null) {
        log.info("💾 피드백 저장 시도 - guideId: {}", guideId);
        try {
            saveFeedbackToDatabase(guideId, result);
        } catch (Exception e) {
            log.error("❌ 피드백 저장 실패 - guideId: {}", guideId, e);
            throw new RuntimeException("피드백 저장 중 오류 발생: " + e.getMessage(), e);
        }
    } else {
        log.warn("⚠️ guideId와 memberId가 모두 null이어서 피드백 저장 스킵");
    }

    log.info("✅ 코칭 완료 - guideId: {}, 점수: {}, 처리시간: {}ms", 
        request.getGuideId(), result.getAppropriatenessScore(), result.getProcessingTimeMs());
    return result;
    
  }



    /**
     * 코칭 결과 후처리
     */
    private void processCoachingResult(PortfolioGuideResult result, LocalDateTime startTime) {
        Duration duration = Duration.between(startTime, LocalDateTime.now());
        result.setProcessingTimeMs(duration.toMillis());
        result.setCoachingAt(LocalDateTime.now());
        
        // 성공 여부 재확인
        if (result.getCoachingMessage() != null && !result.getCoachingMessage().trim().isEmpty()) {
            result.setSuccess(true);
        }
        
        // 진행률 기본값 설정
        if (result.getProgressPercentage() == null) {
            result.setProgressPercentage(0);
        }
    }

    /**
     * 가이드 ID로 가이드 조회
     */
    public PortfolioGuide getGuideById(Integer guideId) {
        log.info("가이드 조회 - guideId: {}", guideId);
        return portfolioGuideDao.selectGuideById(guideId);
    }

    /**
     * 회원별 가이드 목록 조회
     */
    public List<PortfolioGuide> getGuidesByMemberId(Integer memberId) {
        log.info("회원별 가이드 목록 조회 - memberId: {}", memberId);
        return portfolioGuideDao.selectGuidesByMemberId(memberId);
    }

    /**
     * 가이드의 AI 피드백 조회 (JSONB → Java 객체 변환)
     */
    public PortfolioGuideResult getGuideFeedback(Integer guideId) {
        try {
            log.info("가이드 피드백 조회 - guideId: {}", guideId);
            
            // DB에서 가이드 조회
            PortfolioGuide guide = portfolioGuideDao.selectGuideById(guideId);
            
            if (guide == null) {
                log.warn("가이드를 찾을 수 없음 - guideId: {}", guideId);
                return null;
            }
            
            // JSONB 문자열을 PortfolioGuideResult 객체로 변환
            String feedbackJson = guide.getGuideFeedback();
            if (feedbackJson == null || feedbackJson.trim().isEmpty()) {
                log.warn("저장된 피드백이 없음 - guideId: {}", guideId);
                return null;
            }
            
            PortfolioGuideResult result = objectMapper.readValue(
                feedbackJson, 
                PortfolioGuideResult.class
            );
            
            log.info("피드백 조회 성공 - guideId: {}, 점수: {}", 
                guideId, result.getAppropriatenessScore());
            
            return result;
            
        } catch (Exception e) {
            log.error("피드백 조회 중 오류 발생 - guideId: {}", guideId, e);
            return null;
        }
    }



    // ===== 🔥 NEW: 가이드 저장 관련 메서드들 =====

    /**
     * 🔥 개별 항목 저장
     * 사용자가 특정 항목을 완료했을 때 해당 내용을 가이드에 저장
     */
    @Transactional
    public GuideProgressResponse saveGuideItem(
            GuideItemSaveRequest request) {
        try {
            log.info("💾 개별 항목 저장 시작 - guideId: {}, 단계: {}, 항목: {}", 
                request.getGuideId(), 
                request.getStepNumber(), 
                request.getItemTitle());
            
            // 1. 기존 가이드 조회
            PortfolioGuide existingGuide = portfolioGuideDao.selectGuideById(request.getGuideId());
            if (existingGuide == null) {
                throw new IllegalArgumentException("존재하지 않는 가이드입니다: " + request.getGuideId());
            }
            
            // 2. 기존 가이드 내용 파싱
            java.util.Map<String, Object> guideContentMap = parseGuideContent(existingGuide.getGuideContent());
            
            // 3. 새 항목 추가/업데이트
            updateItemInGuideContent(guideContentMap, request);
            
            // 4. 진행률 계산
            int newCompletionPercentage = calculateCompletionPercentage(guideContentMap);
            
            // 5. DB 업데이트
            String updatedGuideContentJson = objectMapper.writeValueAsString(guideContentMap);
            
            java.util.Map<String, Object> updateParams = new java.util.HashMap<>();
            updateParams.put("guideId", request.getGuideId());
            updateParams.put("guideContent", updatedGuideContentJson);
            updateParams.put("completionPercentage", newCompletionPercentage);
            updateParams.put("currentStep", request.getStepNumber());
            
            int updatedRows = portfolioGuideDao.updateGuideContent(updateParams);
            
            if (updatedRows == 0) {
                throw new RuntimeException("가이드 내용 업데이트에 실패했습니다");
            }
            
            log.info("✅ 개별 항목 저장 완료 - guideId: {}, 새 진행률: {}%", 
                request.getGuideId(), 
                newCompletionPercentage);
            
            return com.example.demo.portfolio.dto.response.GuideProgressResponse.builder()
                .success(true)
                .message("항목이 성공적으로 저장되었습니다")
                .guideId(request.getGuideId())
                .completionPercentage(newCompletionPercentage)
                .currentStep(request.getStepNumber())
                .lastUpdated(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
                
        } catch (Exception e) {
            log.error("❌ 개별 항목 저장 중 오류 발생", e);
            throw new RuntimeException("개별 항목 저장에 실패했습니다", e);
        }
    }

    /**
     * 🔥 전체 가이드 진행상황 저장
     * 사용자가 "진행상황 저장" 버튼을 클릭했을 때 모든 내용을 저장
     */
    @org.springframework.transaction.annotation.Transactional
    public GuideProgressResponse saveGuideProgress(
            GuideProgressSaveRequest request) {
        try {
            log.info("💾 전체 가이드 저장 시작 - guideId: {}, 진행률: {}%", 
                request.getGuideId(), 
                request.getCompletionPercentage());
            
            // 1. 가이드 내용을 JSONB 형식으로 구성
            java.util.Map<String, Object> guideContentMap = new java.util.HashMap<>();
            guideContentMap.put("steps", request.getGuideContent());
            guideContentMap.put("lastUpdated", LocalDateTime.now().toString());
            guideContentMap.put("version", "1.0");
            
            String guideContentJson = objectMapper.writeValueAsString(guideContentMap);
            
            // 2. DB 업데이트
            java.util.Map<String, Object> updateParams = new java.util.HashMap<>();
            updateParams.put("guideId", request.getGuideId());
            updateParams.put("guideContent", guideContentJson);
            updateParams.put("completionPercentage", request.getCompletionPercentage());
            updateParams.put("currentStep", request.getCurrentStep());
            updateParams.put("isCompleted", request.getCompletionPercentage() >= 100);
            
            int updatedRows = portfolioGuideDao.updateGuideProgress(updateParams);
            
            if (updatedRows == 0) {
                throw new RuntimeException("가이드 진행상황 업데이트에 실패했습니다");
            }
            
            // 3. 단계별 진행상황 계산
            List<GuideProgressResponse.StepProgress> stepProgressList = 
                calculateStepProgress(request.getGuideContent());
            
            log.info("✅ 전체 가이드 저장 완료 - guideId: {}, 최종 진행률: {}%", 
                request.getGuideId(), 
                request.getCompletionPercentage());
            
            return GuideProgressResponse.builder()
                .success(true)
                .message("가이드 진행상황이 성공적으로 저장되었습니다")
                .guideId(request.getGuideId())
                .memberId(request.getMemberId())
                .completionPercentage(request.getCompletionPercentage())
                .currentStep(request.getCurrentStep())
                .totalSteps(request.getGuideContent().size())
                .guideContent(guideContentMap)
                .stepProgress(stepProgressList)
                .lastUpdated(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
                
        } catch (Exception e) {
            log.error("❌ 전체 가이드 저장 중 오류 발생", e);
            throw new RuntimeException("전체 가이드 저장에 실패했습니다", e);
        }
    }

    /**
     * 🔥 저장된 가이드 내용 조회
     * 사용자가 페이지를 다시 열었을 때 이전에 작성한 내용들을 복원
     */
    public GuideProgressResponse getGuideContent(Integer guideId) {
        try {
            log.info("📖 가이드 내용 조회 - guideId: {}", guideId);
            
            PortfolioGuide guide = portfolioGuideDao.selectGuideById(guideId);
            if (guide == null) {
                return GuideProgressResponse.builder()
                    .success(false)
                    .message("존재하지 않는 가이드입니다")
                    .build();
            }
            
            // 가이드 내용 파싱
            java.util.Map<String, Object> guideContentMap = parseGuideContent(guide.getGuideContent());
            
            // 단계별 진행상황 계산
            List<GuideProgressResponse.StepProgress> stepProgressList = 
                new java.util.ArrayList<>();
            if (guideContentMap.containsKey("steps")) {
                @SuppressWarnings("unchecked")
                List<java.util.Map<String, Object>> steps = 
                    (List<java.util.Map<String, Object>>) guideContentMap.get("steps");
                
                for (java.util.Map<String, Object> step : steps) {
                    @SuppressWarnings("unchecked")
                    List<java.util.Map<String, Object>> items = 
                        (List<java.util.Map<String, Object>>) step.get("items");
                    
                    int completedItems = 0;
                    if (items != null) {
                        completedItems = (int) items.stream()
                            .filter(item -> "완료".equals(item.get("status")))
                            .count();
                    }
                    
                    stepProgressList.add(com.example.demo.portfolio.dto.response.GuideProgressResponse.StepProgress.builder()
                        .stepNumber((Integer) step.get("stepNumber"))
                        .stepTitle((String) step.get("stepTitle"))
                        .progress((Integer) step.getOrDefault("stepProgress", 0))
                        .completedItems(completedItems)
                        .totalItems(items != null ? items.size() : 0)
                        .build());
                }
            }
            
            log.info("✅ 가이드 내용 조회 완료 - guideId: {}, 진행률: {}%", 
                guideId, 
                guide.getCompletionPercentage());
            
            return com.example.demo.portfolio.dto.response.GuideProgressResponse.builder()
                .success(true)
                .message("가이드 내용 조회 성공")
                .guideId(guideId)
                .memberId(guide.getMemberId())
                .completionPercentage(guide.getCompletionPercentage())
                .currentStep(guide.getCurrentStep())
                .totalSteps(guide.getTotalSteps())
                .guideContent(guideContentMap)
                .stepProgress(stepProgressList)
                .lastUpdated(guide.getUpdatedAt() != null ? 
                    guide.getUpdatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .build();
                
        } catch (Exception e) {
            log.error("❌ 가이드 내용 조회 중 오류 발생", e);
            return com.example.demo.portfolio.dto.response.GuideProgressResponse.builder()
                .success(false)
                .message("가이드 내용 조회에 실패했습니다")
                .build();
        }
    }

   

    // ===== 🔥 Private 유틸리티 메서드들 =====

    /**
     * 가이드 내용 JSON 파싱
     */
    private Map<String, Object> parseGuideContent(String guideContentJson) {
        try {
            if (guideContentJson == null || guideContentJson.trim().isEmpty()) {
                return new HashMap<>();
            }
            
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> contentMap = objectMapper.readValue(guideContentJson, java.util.Map.class);
            return contentMap;
            
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("가이드 내용 JSON 파싱 실패, 빈 맵 반환: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 가이드 내용에 새 항목 추가/업데이트
     */
    @SuppressWarnings("unchecked")
    private void updateItemInGuideContent(java.util.Map<String, Object> guideContentMap, 
            com.example.demo.portfolio.dto.request.GuideItemSaveRequest request) {
        // steps 배열 가져오기 또는 생성
        List<java.util.Map<String, Object>> steps = (List<java.util.Map<String, Object>>) 
            guideContentMap.computeIfAbsent("steps", k -> new java.util.ArrayList<>());
        
        // 해당 단계 찾기 또는 생성
        java.util.Map<String, Object> targetStep = steps.stream()
            .filter(step -> request.getStepNumber().equals(step.get("stepNumber")))
            .findFirst()
            .orElse(null);
        
        if (targetStep == null) {
            targetStep = new java.util.HashMap<>();
            targetStep.put("stepNumber", request.getStepNumber());
            targetStep.put("stepTitle", request.getStepTitle());
            targetStep.put("items", new java.util.ArrayList<>());
            steps.add(targetStep);
        }
        
        // 항목 배열 가져오기
        List<java.util.Map<String, Object>> items = (List<java.util.Map<String, Object>>) 
            targetStep.computeIfAbsent("items", k -> new java.util.ArrayList<>());
        
        // 해당 항목 찾기 또는 생성
        java.util.Map<String, Object> targetItem = items.stream()
            .filter(item -> request.getItemTitle().equals(item.get("title")))
            .findFirst()
            .orElse(null);
        
        if (targetItem == null) {
            targetItem = new java.util.HashMap<>();
            targetItem.put("title", request.getItemTitle());
            items.add(targetItem);
        }
        
        // 항목 내용 업데이트
        targetItem.put("content", request.getItemContent());
        targetItem.put("status", request.getItemStatus());
        if (request.getFeedback() != null) {
            targetItem.put("feedback", request.getFeedback());
        }
        
        // 단계별 진행률 계산 및 업데이트
        int completedItems = (int) items.stream()
            .filter(item -> "완료".equals(item.get("status")))
            .count();
        int stepProgress = Math.round((float) completedItems / items.size() * 100);
        targetStep.put("stepProgress", stepProgress);
    }

    /**
     * 전체 진행률 계산
     */
    @SuppressWarnings("unchecked")
    private int calculateCompletionPercentage(java.util.Map<String, Object> guideContentMap) {
        List<java.util.Map<String, Object>> steps = (List<java.util.Map<String, Object>>) 
            guideContentMap.get("steps");
        
        if (steps == null || steps.isEmpty()) {
            return 0;
        }
        
        int totalItems = 0;
        int completedItems = 0;
        
        for (java.util.Map<String, Object> step : steps) {
            List<java.util.Map<String, Object>> items = (List<java.util.Map<String, Object>>) step.get("items");
            if (items != null) {
                totalItems += items.size();
                completedItems += (int) items.stream()
                    .filter(item -> "완료".equals(item.get("status")))
                    .count();
            }
        }
        
        return totalItems > 0 ? Math.round((float) completedItems / totalItems * 100) : 0;
    }

    /**
     * 단계별 진행상황 계산
     */
    private List<GuideProgressResponse.StepProgress> calculateStepProgress(
            List<com.example.demo.portfolio.dto.GuideStepData> steps) {
        
        List<com.example.demo.portfolio.dto.response.GuideProgressResponse.StepProgress> stepProgressList = 
            new java.util.ArrayList<>();
        
        for (com.example.demo.portfolio.dto.GuideStepData step : steps) {
            int completedItems = (int) step.getItems().stream()
                .filter(item -> "완료".equals(item.getStatus()))
                .count();
            
            stepProgressList.add(com.example.demo.portfolio.dto.response.GuideProgressResponse.StepProgress.builder()
                .stepNumber(step.getStepNumber())
                .stepTitle(step.getStepTitle())
                .progress(step.getStepProgress())
                .completedItems(completedItems)
                .totalItems(step.getItems().size())
                .build());
        }
        
        return stepProgressList;
    }

    /**
     * AI 피드백을 DB에 저장
     * @throws Exception JSON 변환 실패 또는 DB 저장 실패 시
     */
    private void saveFeedbackToDatabase(Integer guideId, PortfolioGuideResult result) throws Exception {
        log.info("=== AI 피드백 저장 시작 - guideId: {} ===", guideId);
        
        // PortfolioGuideResult를 JSON으로 변환
        String feedbackJson = objectMapper.writeValueAsString(result);
        log.debug("피드백 JSON 변환 완료 - 길이: {} bytes", feedbackJson.length());
        log.trace("피드백 JSON 내용: {}", feedbackJson.substring(0, Math.min(200, feedbackJson.length())));
        
        // DB에 저장
        log.debug("DB 업데이트 시도 - guideId: {}", guideId);
        int updated = portfolioGuideDao.updateGuideFeedback(guideId, feedbackJson);
        log.debug("DB 업데이트 결과 - 영향받은 행 수: {}", updated);
        
        if (updated > 0) {
            log.info("✅ AI 피드백 저장 완료 - guideId: {}, 점수: {}, 제안수: {}", 
                guideId, 
                result.getAppropriatenessScore(),
                result.getSuggestions() != null ? result.getSuggestions().size() : 0);
        } else {
            log.warn("⚠️ AI 피드백 저장 실패 - guideId: {} (업데이트된 행 없음)", guideId);
            throw new IllegalStateException("가이드를 찾을 수 없습니다 - guideId: " + guideId);
        }
    }


    /**
     * 가이드 생성 또는 조회 (없으면 새로 생성)
     */
    public PortfolioGuide getOrCreateGuide(PortfolioGuideRequest request) {
        try {
            // 1. 기존 가이드 조회 시도 (guideId가 있는 경우)
            if (request.getGuideId() != null) {
                PortfolioGuide existing = portfolioGuideDao.selectGuideById(request.getGuideId());
                if (existing != null) {
                    return existing;
                }
            }

            // 2. 새 가이드 생성
            PortfolioGuide newGuide = new PortfolioGuide();
            newGuide.setMemberId(request.getMemberId());
            newGuide.setStandardId(request.getStandardId());
            newGuide.setTitle(request.getInputFieldType() + " 작성 가이드");
            newGuide.setCompletionPercentage(0);
            newGuide.setCurrentStep(request.getCurrentStep() != null ? request.getCurrentStep() : 1);
            newGuide.setTotalSteps(5);
            newGuide.setIsCompleted(false);

            // 초기 콘텐츠 설정
            try {
                String initialContent = objectMapper.writeValueAsString(
                    java.util.Map.of(
                        "fieldType", request.getInputFieldType(),
                        "userInput", request.getUserInput() != null ? request.getUserInput() : "",
                        "createdAt", LocalDateTime.now().toString()
                    )
                );
                newGuide.setGuideContent(initialContent);
            } catch (Exception e) {
                log.warn("초기 콘텐츠 생성 실패, null로 설정", e);
                newGuide.setGuideContent(null);
            }

            portfolioGuideDao.insertGuide(newGuide);
            log.info("새 가이드 생성 완료 - guideId: {}, memberId: {}", 
                newGuide.getGuideId(), request.getMemberId());
            
            return newGuide;
            
        } catch (Exception e) {
            log.error("가이드 생성/조회 중 오류", e);
            throw new RuntimeException("가이드 처리 실패: " + e.getMessage());
        }
    }
}
