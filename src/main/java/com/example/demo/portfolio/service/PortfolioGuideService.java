package com.example.demo.portfolio.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ai.portfolioguide.PortfolioGuideAgent;
import com.example.demo.member.dao.MemberDao;
import com.example.demo.portfolio.dao.PortfolioGuideDao;
import com.example.demo.portfolio.dao.PortfolioStandardDao;
import com.example.demo.portfolio.dto.GuideContentData;
import com.example.demo.portfolio.dto.GuideItemData;
import com.example.demo.portfolio.dto.GuideResult;
import com.example.demo.portfolio.dto.GuideStepData;
import com.example.demo.portfolio.dto.request.GuideItemSaveRequest;
import com.example.demo.portfolio.dto.request.GuideProgressSaveRequest;
import com.example.demo.portfolio.dto.request.GuideRequest;
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
  public GuideResult provideCoaching(GuideRequest request) throws com.fasterxml.jackson.core.JsonProcessingException {
    LocalDateTime startTime= LocalDateTime.now();

    log.info("📋 포트폴리오 가이드 코칭 시작 - guideId: {}, memberId: {}, 단계: {}, 필드: {}, 입력: '{}'", 
        request.getGuideId(), 
        request.getMemberId(),
        request.getCurrentStep(), 
        request.getInputFieldType(),
        request.getUserInput() != null ? request.getUserInput().substring(0, Math.min(30, request.getUserInput().length())) : "null");
    
    // PortfolioGuideAgent가 DTO로 직접 반환
    GuideResult result = portfolioGuideAgent.evaluate(request);
    log.debug("AI 코칭 결과 생성 완료 - 점수: {}, 진행률: {}%", 
        result.getAppropriatenessScore(), result.getProgressPercentage());

    // 처리 시간 기록
    Duration duration = Duration.between(startTime, LocalDateTime.now());
    log.debug("처리 시간: {}ms", duration.toMillis());

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
        request.getGuideId(), result.getAppropriatenessScore(), duration.toMillis());
    return result;
    
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
     * 🔥 저장된 피드백 조회 (JSONB → GuideResult 변환)
     */
    public GuideResult getGuideFeedback(Integer guideId) throws com.fasterxml.jackson.core.JsonProcessingException {
        log.info("가이드 피드백 조회 - guideId: {}", guideId);
        
        // DB에서 가이드 조회
        PortfolioGuide guide = portfolioGuideDao.selectGuideById(guideId);
        
        if (guide == null) {
            log.warn("가이드를 찾을 수 없음 - guideId: {}", guideId);
            throw new NoSuchElementException("존재하지 않는 가이드입니다: " + guideId);
        }
        
        // JSONB 문자열을 GuideResult 객체로 변환
        String feedbackJson = guide.getGuideFeedback();
        if (feedbackJson == null || feedbackJson.trim().isEmpty()) {
            log.warn("저장된 피드백이 없음 - guideId: {}", guideId);
            throw new NoSuchElementException("저장된 피드백이 없습니다: " + guideId);
        }
        
        GuideResult feedback = objectMapper.readValue(
            feedbackJson, 
            GuideResult.class
        );
        
        log.info("피드백 조회 성공 - guideId: {}, 점수: {}", 
            guideId, feedback.getAppropriatenessScore());
        
        return feedback;
    }



    // ===== 🔥 NEW: 가이드 저장 관련 메서드들 =====

    /**
     * 🔥 개별 항목 저장
     * 사용자가 특정 항목을 완료했을 때 해당 내용을 가이드에 저장
     */
    @Transactional
    public GuideProgressResponse saveGuideItem(
            GuideItemSaveRequest request) throws com.fasterxml.jackson.core.JsonProcessingException {
        log.info("💾 개별 항목 저장 시작 - guideId: {}, 단계: {}, 항목: {}", 
            request.getGuideId(), 
            request.getStepNumber(), 
            request.getItemTitle());
        
        // 1. 기존 가이드 조회
        PortfolioGuide existingGuide = portfolioGuideDao.selectGuideById(request.getGuideId());
        if (existingGuide == null) {
            throw new NoSuchElementException("존재하지 않는 가이드입니다: " + request.getGuideId());
        }
        
        // 2. 기존 가이드 내용 파싱 (String → GuideContentData)
        GuideContentData guideContent = parseGuideContent(existingGuide.getGuideContent());
        
        // 3. 새 항목 추가/업데이트
        updateItemInGuideContent(guideContent, request);
        
        // 4. 진행률 계산
        int newCompletionPercentage = calculateCompletionPercentage(guideContent);
        
        // 5. DB 업데이트 (GuideContentData → JSON String)
        String updatedGuideContentJson = objectMapper.writeValueAsString(guideContent);
        
        java.util.Map<String, Object> updateParams = new java.util.HashMap<>();
        updateParams.put("guideId", request.getGuideId());
        updateParams.put("guideContent", updatedGuideContentJson);
        updateParams.put("completionPercentage", newCompletionPercentage);
        updateParams.put("currentStep", request.getStepNumber());
        
        int updatedRows = portfolioGuideDao.updateGuideContent(updateParams);
        
        if (updatedRows == 0) {
            throw new IllegalStateException("가이드 내용 업데이트에 실패했습니다");
        }
        
        log.info("✅ 개별 항목 저장 완료 - guideId: {}, 새 진행률: {}%", 
            request.getGuideId(), 
            newCompletionPercentage);
        
        GuideProgressResponse response = new GuideProgressResponse();
        response.setSuccess(true);
        response.setMessage("항목이 성공적으로 저장되었습니다");
        response.setGuideId(request.getGuideId());
        response.setCompletionPercentage(newCompletionPercentage);
        response.setCurrentStep(request.getStepNumber());
        response.setLastUpdated(LocalDateTime.now());
        
        return response;
    }

    /**
     * 🔥 전체 가이드 진행상황 저장
     * 사용자가 "진행상황 저장" 버튼을 클릭했을 때 모든 내용을 저장
     */
    @Transactional
    public GuideProgressResponse saveGuideProgress(
            GuideProgressSaveRequest request) throws com.fasterxml.jackson.core.JsonProcessingException {
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
            throw new IllegalStateException("가이드 진행상황 업데이트에 실패했습니다");
        }
        
        log.info("✅ 전체 가이드 저장 완료 - guideId: {}, 최종 진행률: {}%", 
            request.getGuideId(), 
            request.getCompletionPercentage());
        
        GuideProgressResponse response = new GuideProgressResponse();
        response.setSuccess(true);
        response.setMessage("가이드 진행상황이 성공적으로 저장되었습니다");
        response.setGuideId(request.getGuideId());
        response.setMemberId(request.getMemberId());
        response.setCompletionPercentage(request.getCompletionPercentage());
        response.setCurrentStep(request.getCurrentStep());
        response.setTotalSteps(request.getGuideContent().size());
        response.setGuideContent(request.getGuideContent());
        response.setLastUpdated(LocalDateTime.now());
        
        return response;
    }

    /**
     * 🔥 저장된 가이드 내용 조회
     * 사용자가 페이지를 다시 열었을 때 이전에 작성한 내용들을 복원
     */
    public GuideProgressResponse getGuideContent(Integer guideId) throws com.fasterxml.jackson.core.JsonProcessingException {
        log.info("📖 가이드 내용 조회 - guideId: {}", guideId);
        
        PortfolioGuide guide = portfolioGuideDao.selectGuideById(guideId);
        if (guide == null) {
            throw new NoSuchElementException("존재하지 않는 가이드입니다: " + guideId);
        }
        
        // JSON → DTO 변환
        GuideContentData guideContent = parseGuideContent(guide.getGuideContent());
        
        log.info("✅ 가이드 내용 조회 완료 - guideId: {}, 진행률: {}%", 
            guideId, 
            guide.getCompletionPercentage());
        
        GuideProgressResponse response = new GuideProgressResponse();
        response.setSuccess(true);
        response.setMessage("가이드 내용 조회 성공");
        response.setGuideId(guideId);
        response.setMemberId(guide.getMemberId());
        response.setTitle(guide.getTitle());
        response.setCompletionPercentage(guide.getCompletionPercentage());
        response.setIsCompleted(guide.getIsCompleted());
        response.setCurrentStep(guide.getCurrentStep());
        response.setTotalSteps(guide.getTotalSteps());
        response.setGuideContent(guideContent.getSteps());
        response.setLastUpdated(guide.getUpdatedAt());
        
        return response;
    }

    // ===== 🔥 Private 유틸리티 메서드들 =====

    /**
     * 가이드 내용 JSON 파싱
     * 용도: DB의 JSONB 컬럼(guide_content)에서 가져온 JSON 문자열을 Java Map으로 변환
     * 호출: saveGuideItem(), getGuideContent()에서 사용
     */
    private GuideContentData parseGuideContent(String guideContentJson) throws com.fasterxml.jackson.core.JsonProcessingException {
        if(guideContentJson==null || guideContentJson.trim().isEmpty()){
            return new GuideContentData();
        }
        return objectMapper.readValue(guideContentJson, GuideContentData.class);
    }

    /**
     * 가이드 내용에 새 항목 추가/업데이트
     * 용도: 사용자가 특정 항목을 완료했을 때 기존 JSONB 구조에 해당 항목만 추가/수정
     * 호출: saveGuideItem()에서 사용
     * 
     */

    private void updateItemInGuideContent(GuideContentData guideContent, GuideItemSaveRequest request) {
        
        GuideStepData targetStep = null;

        for(GuideStepData step : guideContent.getSteps()){
            if(request.getStepNumber().equals(step.getStepNumber())){
                targetStep = step;
                break;
            }
        }

        //못찾으면 새로 생성
        if(targetStep ==null){
            targetStep=new GuideStepData();
            targetStep.setStepNumber(request.getStepNumber());
            targetStep.setStepTitle(request.getStepTitle());
            guideContent.getSteps().add(targetStep);
        }

        //해당 항목 찾기
        GuideItemData targetItem = null;

        for(GuideItemData item : targetStep.getItems()){
            if(request.getItemTitle().equals(item.getTitle())){
                targetItem = item;
                break;
            }
        }

        //못찾으면 새로 생성
        if(targetItem==null){
            targetItem=new GuideItemData();
            targetItem.setTitle(request.getItemTitle());
            targetStep.getItems().add(targetItem);
        }

        //3단계 항목 내용 업데이트
        targetItem.setContent(request.getItemContent());
        targetItem.setStatus(request.getItemStatus());
        if(request.getFeedback()!=null){
            targetItem.setFeedback(request.getFeedback());
        }

        //4단계
        int completedCount=0;

        for(GuideItemData item : targetStep.getItems()){
            if("완료".equals(item.getStatus())){
                completedCount++;
            }
        }
        //진행률 계산
        int stepProgress = Math.round((float)completedCount/targetStep.getItems().size()*100);
        targetStep.setStepProgress(stepProgress);

    }

    /**
     * 전체 진행률 계산
     * 용도: 모든 단계의 모든 항목 중 "완료" 상태인 항목 비율 계산
     * 호출: saveGuideItem()에서 사용
     * 예: 총 10개 항목 중 7개 완료 → 70% 반환
     */
   
    private int calculateCompletionPercentage(GuideContentData guideContent) {
        //steps 가 비어있으면 0% 반환
        if(guideContent.getSteps().isEmpty()){
            return 0;
        }

        int totalItems =0;
        int completedItems=0;

        //외부 for문 : 모든 단계 순환
        for(GuideStepData step: guideContent.getSteps()){
            
            //각 단계의 전체 항목 수 누적
            totalItems += step.getItems().size();

            //내부 for문 : 각 단계의 항목들 순회
            for(GuideItemData item: step.getItems()){
                if("완료".equals(item.getStatus())){
                    completedItems++;
                }
            }
        }
        //진행률 계산
        //totalItems가 0보다 크면 계산 아니면 0% 반환
        if(totalItems>0){
            return Math.round((float)completedItems/totalItems*100);
        }else{
            return 0;
        }

    }


    /**
     * 🔥 AI 피드백을 DB에 JSONB로 저장
     * @throws com.fasterxml.jackson.core.JsonProcessingException JSON 변환 실패 시
     * @throws IllegalStateException DB 저장 실패 시
     */
    private void saveFeedbackToDatabase(Integer guideId, GuideResult feedback) 
            throws com.fasterxml.jackson.core.JsonProcessingException {
        String feedbackJson = objectMapper.writeValueAsString(feedback);

        Map<String,Object> updateParams = new HashMap<>();
        updateParams.put("guideId", guideId);
        updateParams.put("guideFeedback", feedbackJson);

        int updated = portfolioGuideDao.updateGuideProgress(updateParams);

        if(updated==0){
            throw new IllegalStateException("가이드를 찾을 수 없습니다: " + guideId);
        }
    }

    /**
     * 가이드 생성 또는 조회 (없으면 새로 생성)
     */
    public PortfolioGuide getOrCreateGuide(GuideRequest request) throws com.fasterxml.jackson.core.JsonProcessingException {
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
        String initialContent = objectMapper.writeValueAsString(
            java.util.Map.of(
                "fieldType", request.getInputFieldType(),
                "userInput", request.getUserInput() != null ? request.getUserInput() : "",
                "createdAt", LocalDateTime.now().toString()
            )
        );
        newGuide.setGuideContent(initialContent);

        portfolioGuideDao.insertGuide(newGuide);
        log.info("새 가이드 생성 완료 - guideId: {}, memberId: {}", 
            newGuide.getGuideId(), request.getMemberId());
        
        return newGuide;
    }
}
