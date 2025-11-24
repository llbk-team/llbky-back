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
