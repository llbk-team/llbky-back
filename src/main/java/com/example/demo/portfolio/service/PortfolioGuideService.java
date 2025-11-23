package com.example.demo.portfolio.service;

import java.time.LocalDateTime;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.example.demo.ai.portfolioguide.PortfolioGuideAgent;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.portfolio.dao.PortfolioGuideDao;
import com.example.demo.portfolio.dao.PortfolioStandardDao;
import com.example.demo.portfolio.dto.PortfolioGuideResult;
import com.example.demo.portfolio.dto.request.PortfolioGuideRequest;
import com.example.demo.portfolio.entity.PortfolioGuide;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

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
        saveFeedbackToDatabase(guideId, result);
    } else {
        log.warn("⚠️ guideId와 memberId가 모두 null이어서 피드백 저장 스킵");
    }

    log.info("✅ 코칭 완료 - guideId: {}, 점수: {}, 처리시간: {}ms", 
        request.getGuideId(), result.getAppropriatenessScore(), result.getProcessingTimeMs());
    return result;
    
  }

  public PortfolioGuideResult quickCoaching(Integer guideId, String userInput, String inputFieldType){
    log.info("빠른 코칭 요청 - 가이드ID: {}, 필드: {}", guideId, inputFieldType);
   
    PortfolioGuideRequest request = PortfolioGuideRequest.builder()
                .guideId(guideId)
                .userInput(userInput)
                .inputFieldType(inputFieldType)
                .currentStep(1)
                .jobGroup("개발자") // 기본값
                .jobRole("일반") // 기본값
                .careerYears(1) // 기본값
                .build();
                
        return provideCoaching(request);
  }


  //단계별 코칭

  public PortfolioGuideResult stepCoaching(Integer guideId, Integer step,String fieldType, String userInput, Integer memberId){
     log.info("단계별 코칭 요청 - 가이드ID: {}, 단계: {}, 필드: {}", guideId, step, fieldType);
   
    PortfolioGuideRequest request = PortfolioGuideRequest.builder()
                .guideId(guideId)
                .memberId(memberId)
                .currentStep(step)
                .inputFieldType(fieldType)
                .userInput(userInput)
                .build();
                
        return provideCoaching(request);
  }

  public PortfolioGuideResult realtimeCoaching(Integer guideId, String userInput, String inputFieldType, Integer memberId){
    if(userInput==null || userInput.trim().length()<3){
      return createTypingGuide(inputFieldType);
    }

    PortfolioGuideRequest request = PortfolioGuideRequest.builder()
    .guideId(guideId)
    .memberId(memberId)
    .userInput(userInput)
    .inputFieldType(inputFieldType)
    .currentStep(estimateStepFromField(inputFieldType))
    .build();

    return provideCoaching(request);
  }

  // getStandards와 performAICoaching은 PortfolioGuideAgent로 이동되었습니다.

 



    // buildCoachingPrompt와 헬퍼 메서드들은 PortfolioGuideAgent로 이동되었습니다.

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
     * 타이핑 중 기본 가이드 생성
     */
    private PortfolioGuideResult createTypingGuide(String inputFieldType) {
        String guideMessage = getFieldGuide(inputFieldType);
        
        return PortfolioGuideResult.builder()
                .success(true)
                .coachingMessage(guideMessage)
                .appropriatenessScore(5)
                .nextStepGuide("더 자세한 내용을 입력해주시면 구체적인 코칭을 받을 수 있습니다.")
                .progressPercentage(0)
                .coachingAt(LocalDateTime.now())
                .build();
    }

    /**
     * 필드별 기본 가이드 메시지
     */
    private String getFieldGuide(String inputFieldType) {
        if (inputFieldType == null) return "내용을 입력해주세요.";
        
        switch (inputFieldType) {
            case "프로젝트 제목":
                return "프로젝트의 핵심을 잘 나타내는 제목을 입력해주세요. 기술 스택이나 목적이 드러나면 더 좋습니다.";
            case "프로젝트 기간":
                return "프로젝트 진행 기간을 입력해주세요. (예: 2024.03 ~ 2024.06, 3개월)";
            case "프로젝트 목적":
                return "이 프로젝트를 진행한 목적이나 해결하고자 한 문제를 설명해주세요.";
            case "팀 구성":
                return "프로젝트에 참여한 팀원 구성과 본인의 역할을 입력해주세요.";
            case "핵심 기능":
                return "프로젝트의 주요 기능들을 구체적으로 설명해주세요.";
            case "사용 기술":
                return "프로젝트에서 사용한 기술 스택과 도구들을 입력해주세요.";
            default:
                return "해당 항목에 대해 구체적으로 작성해주세요. 더 자세할수록 좋은 코칭을 받을 수 있습니다.";
        }
    }

    /**
     * 필드 타입으로 단계 추정
     */
    private Integer estimateStepFromField(String inputFieldType) {
        if (inputFieldType == null) return 1;
        
        switch (inputFieldType) {
            case "프로젝트 제목":
            case "프로젝트 기간":
            case "프로젝트 목적":
            case "팀 구성":
                return 1; // 1단계: 프로젝트 개요
            case "핵심 기능":
            case "사용 기술":
                return 2; // 2단계: 기술 및 기능
            case "기술적 도전":
            case "문제 해결":
                return 3; // 3단계: 경험과 성과
            case "성과 지표":
            case "배운 점":
                return 4; // 4단계: 결과 및 성과
            case "발전 계획":
                return 5; // 5단계: 향후 계획
            default:
                return 1; // 기본값
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

    /**
     * AI 피드백을 DB에 저장
     */
    private void saveFeedbackToDatabase(Integer guideId, PortfolioGuideResult result) {
        try {
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
                log.warn("⚠️ AI 피드백 저장 실패 - guideId: {} (업데이트된 행 없음, 가이드가 존재하지 않을 수 있음)", guideId);
            }
        } catch (Exception e) {
            log.error("❌ AI 피드백 저장 중 오류 발생 - guideId: {}, 오류: {}", guideId, e.getMessage(), e);
        }
    }

    /**
     * 가이드 진행률 업데이트 (단계 진행 시)
     */
    public void updateGuideProgress(Integer guideId, Integer currentStep, Integer progressPercentage) {
        try {
            int totalSteps = 5; // 기본 5단계
            int calculatedProgress = (currentStep * 100) / totalSteps;
            
            // 파라미터로 받은 진행률이 있으면 우선 사용
            int finalProgress = progressPercentage != null ? progressPercentage : calculatedProgress;
            
            boolean isCompleted = finalProgress >= 100;
            
            portfolioGuideDao.updateGuideProgress(guideId, finalProgress, currentStep, isCompleted);
            log.info("가이드 진행률 업데이트 - guideId: {}, step: {}, progress: {}%", 
                guideId, currentStep, finalProgress);
        } catch (Exception e) {
            log.error("가이드 진행률 업데이트 중 오류 - guideId: {}", guideId, e);
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

    /**
     * 가이드 콘텐츠 및 피드백 업데이트
     */
    public void updateGuideWithFeedback(Integer guideId, PortfolioGuideRequest request, 
                                        PortfolioGuideResult feedback) {
        try {
            // 업데이트할 콘텐츠 구성
            String contentJson = objectMapper.writeValueAsString(
                java.util.Map.of(
                    "fieldType", request.getInputFieldType(),
                    "userInput", request.getUserInput() != null ? request.getUserInput() : "",
                    "currentStep", request.getCurrentStep(),
                    "updatedAt", LocalDateTime.now().toString()
                )
            );

            // 피드백 JSON
            String feedbackJson = objectMapper.writeValueAsString(feedback);

            // 진행률 계산 (입력 길이 기반)
            int progressPercentage = calculateProgress(request.getUserInput());
            int currentStep = getCurrentStep(progressPercentage);

            // DB 업데이트
            portfolioGuideDao.updateGuideContent(guideId, contentJson);
            portfolioGuideDao.updateGuideFeedback(guideId, feedbackJson);
            portfolioGuideDao.updateGuideProgress(guideId, progressPercentage, 
                currentStep, progressPercentage >= 100);

            log.info("가이드 업데이트 완료 - guideId: {}, progress: {}%", 
                guideId, progressPercentage);
                
        } catch (Exception e) {
            log.error("가이드 업데이트 중 오류 - guideId: {}", guideId, e);
        }
    }

    /**
     * 입력 내용 기반 진행률 계산
     */
    private int calculateProgress(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return 0;
        }
        
        int length = userInput.trim().length();
        if (length < 50) return 20;
        if (length < 150) return 40;  
        if (length < 300) return 60;
        if (length < 500) return 80;
        return 100;
    }

    /**
     * 진행률 기반 현재 단계 계산
     */
    private int getCurrentStep(int progressPercentage) {
        if (progressPercentage < 20) return 1;
        if (progressPercentage < 40) return 2;
        if (progressPercentage < 60) return 3;
        if (progressPercentage < 80) return 4;
        return 5;
    }
}
