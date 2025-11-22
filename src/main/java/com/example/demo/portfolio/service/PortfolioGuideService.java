package com.example.demo.portfolio.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.example.demo.ai.portfolioguide.PortfolioGuideAgent;
import com.example.demo.coverletter.controller.CoverLetterTestController;
import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.dto.Member;
import com.example.demo.portfolio.dao.PortfolioGuideDao;
import com.example.demo.portfolio.dao.PortfolioStandardDao;
import com.example.demo.portfolio.dto.PortfolioGuideResult;
import com.example.demo.portfolio.dto.request.PortfolioGuideRequest;
import com.example.demo.portfolio.entity.PortfolioStandard;
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
        PortfolioGuideAgent portfolioGuideAgent,
        CoverLetterTestController coverLetterTestController){
        this.portfolioGuideDao=portfolioGuideDao;
        this.portfolioStandardDao =portfolioStandardDao;
        this.memberDao=memberDao;
        this.objectMapper = objectMapper;
        this.portfolioGuideAgent = portfolioGuideAgent;
    }


  //-------------메인 코칭 메서드
  public PortfolioGuideResult provideCoaching(PortfolioGuideRequest request){
    LocalDateTime startTime= LocalDateTime.now();

    log.info("포트폴리오 가이드 코칭 시작 - 가이드ID: {}, 단계: {}, 필드: {}, 입력: '{}'", 
    request.getGuideId(), request.getCurrentStep(), request.getInputFieldType(),
    request.getUserInput() != null ? request.getUserInput().substring(0, Math.min(30, request.getUserInput().length())) : "null");
    
    // PortfolioGuideAgent가 모든 DAO 조회와 LLM 호출을 담당
    PortfolioGuideResult result = portfolioGuideAgent.evaluate(request);

    processCoachingResult(result, startTime);

    log.info("가이드 ID: {},점수: {}, 처리시간: {}ms",   request.getGuideId(), result.getAppropriatenessScore());
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
     * 실패 결과 생성
     */
    private PortfolioGuideResult createFailureResult(String errorMessage) {
        return PortfolioGuideResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .coachingAt(LocalDateTime.now())
                .appropriatenessScore(0)
                .build();
    }

    /**
     * 기본 결과 생성 (AI 호출 실패시)
     */
    private PortfolioGuideResult createDefaultResult() {
        return PortfolioGuideResult.builder()
                .success(true)
                .coachingMessage("입력해주신 내용을 확인했습니다. 계속 작성해주세요.")
                .appropriatenessScore(5)
                .coachingAt(LocalDateTime.now())
                .progressPercentage(10)
                .build();
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
}
