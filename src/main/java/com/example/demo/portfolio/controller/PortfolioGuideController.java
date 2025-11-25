package com.example.demo.portfolio.controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.portfolio.dto.GuideResult;
import com.example.demo.portfolio.dto.request.GuideRequest;
import com.example.demo.portfolio.dto.request.GuideItemSaveRequest;
import com.example.demo.portfolio.dto.request.GuideProgressSaveRequest;
import com.example.demo.portfolio.dto.response.GuideProgressResponse;
import com.example.demo.portfolio.entity.PortfolioGuide;
import com.example.demo.portfolio.service.PortfolioGuideService;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 가이드 코칭 컨트롤러
 * - 실시간 포트폴리오 작성 코칭 API
 * - 가이드 내용 저장 및 관리 API
 */
@RestController
@RequestMapping("/api/portfolio-guide")
@RequiredArgsConstructor
@Slf4j
public class PortfolioGuideController {

    @Autowired
    private PortfolioGuideService portfolioGuideService;

    /**
     * 실시간 포트폴리오 코칭 API
     * 
     * @param request - memberId, userInput, inputFieldType 등 포함
     * @return 코칭 결과 (점수, 제안사항, 예시, 다음 단계 가이드)
     * 
     * 테스트 예시:
     * POST /api/portfolio-guide/coaching
     * {
     *   "memberId": 1,
     *   "guideId": 1,
     *   "currentStep": 1,
     *   "inputFieldType": "프로젝트 제목",
     *   "userInput": "사용자 맞춤형 여행 추천 앱"
     * }
     */
    @PostMapping("/coaching")
    public GuideResult getRealtimeCoaching(@RequestBody GuideRequest request) throws com.fasterxml.jackson.core.JsonProcessingException {
        log.info("실시간 코칭 요청 - memberId: {}, 입력 필드: {}, 입력 내용: '{}'", 
            request.getMemberId(), 
            request.getInputFieldType(), 
            request.getUserInput() != null ? 
            request.getUserInput().substring(0, Math.min(30, request.getUserInput().length())) : "null");
        
        // GlobalExceptionHandler에서 예외 처리
        return portfolioGuideService.provideCoaching(request);
    }


    /**
     * 간단한 실시간 피드백 - 회원 정보 없이 직접 입력 (Postman 테스트용)
     * 
     * POST http://localhost:8081/api/portfolio-guide/feedback
     * 
     * 요청 예시:
     * {
     *   "inputFieldType": "프로젝트 제목",
     *   "userInput": "여행 추천 앱",
     *   "currentStep": 1,
     *   "jobGroup": "개발",
     *   "jobRole": "Backend Developer",
     *   "careerYears": 2
     * }
     * 
     * 응답 예시:
     * {
     *   "success": true,
     *   "appropriatenessScore": 65,
     *   "coachingMessage": "프로젝트 제목이 간결하게 잘 표현되었습니다...",
     *   "suggestions": [
     *     "프로젝트의 핵심 기술을 제목에 포함하세요",
     *     "해결하려는 문제를 명확히 드러내세요"
     *   ],
     *   "examples": [
     *     "개인 맞춤형 여행 추천 서비스 - AI 기반 사용자 선호도 분석",
     *     "여행지 추천 및 일정 자동 생성 플랫폼 (Spring Boot + ChatGPT API)",
     *     "빅데이터 기반 여행지 추천 앱 - 월 10만 사용자 대상"
     *   ],
     *   "nextStepGuide": "다음으로 프로젝트 기간과 팀 구성을 작성해주세요",
     *   "progressPercentage": 20
     * }
     */
    @PostMapping("/feedback")
    public GuideResult getRealtimeFeedback(@RequestBody RealtimeFeedbackRequest request) throws com.fasterxml.jackson.core.JsonProcessingException {
        log.info("실시간 피드백 요청 - 필드: {}, 직군: {}, 직무: {}", 
            request.getInputFieldType(), request.getJobGroup(), request.getJobRole());
        
        GuideRequest fullRequest = GuideRequest.builder()
            .inputFieldType(request.getInputFieldType())
            .userInput(request.getUserInput())
            .currentStep(request.getCurrentStep())
            .jobGroup(request.getJobGroup())
            .jobRole(request.getJobRole())
            .careerYears(request.getCareerYears())
            .build();
        
        // GlobalExceptionHandler에서 예외 처리
        return portfolioGuideService.provideCoaching(fullRequest);
    }

    /**
     * 🧪 Postman 테스트용 간편 API - 최소한의 정보만 입력
     * 
     * POST http://localhost:8081/api/portfolio-guide/test-example
     * 
     * 요청 예시 (간단):
     * {
     *   "userInput": "대용량 트래픽 처리를 위한 배달 플랫폼"
     * }
     * 
     * 요청 예시 (상세):
     * {
     *   "userInput": "여행 추천 앱",
     *   "inputFieldType": "프로젝트 제목"
     * }
     */
    @PostMapping("/test-example")
    public GuideResult testExample(@RequestBody SimpleTestRequest request) throws com.fasterxml.jackson.core.JsonProcessingException {
        log.info("🧪 예시 생성 테스트 - 입력: '{}'", request.getUserInput());
        
        // 기본값으로 요청 구성
        GuideRequest fullRequest = GuideRequest.builder()
            .inputFieldType(request.getInputFieldType() != null ? 
                request.getInputFieldType() : "프로젝트 제목")
            .userInput(request.getUserInput())
            .currentStep(1)
            .jobGroup("개발")
            .jobRole("백엔드")
            .careerYears(2)
            .build();
        
        GuideResult response = portfolioGuideService.provideCoaching(fullRequest);
        
        log.info("✅ 예시 생성 완료 - 점수: {}, 예시 개수: {}", 
            response.getAppropriatenessScore(),
            response.getExamples() != null ? response.getExamples().size() : 0);
        
        // 예시만 출력하는 버전도 추가
        if (response.getExamples() != null && !response.getExamples().isEmpty()) {
            log.info("📝 생성된 예시:");
            for (int i = 0; i < response.getExamples().size(); i++) {
                log.info("  예시 {}: {}", i + 1, response.getExamples().get(i));
            }
        }
        
        // GlobalExceptionHandler에서 예외 처리
        return response;
    }


    /**
     * 🔥 NEW: 개별 항목 저장 API
     * 사용자가 특정 항목을 완료했을 때 해당 내용을 가이드에 저장
     * 
     * POST http://localhost:8081/api/portfolio-guide/save-item
     * 
     * 요청 예시:
     * {
     *   "guideId": 2,
     *   "stepNumber": 1,
     *   "stepTitle": "프로젝트 개요",
     *   "itemTitle": "프로젝트 제목",
     *   "itemContent": "AI 기반 취업 컨설팅 서비스",
     *   "itemStatus": "완료",
     *   "feedback": {
     *     "appropriatenessScore": 85,
     *     "coachingMessage": "제목이 명확합니다...",
     *     "suggestions": ["기술 스택 추가", "규모 명시"],
     *     "examples": ["예시1", "예시2", "예시3"]
     *   }
     * }
     */
    @PostMapping("/save-item")
    public GuideProgressResponse saveGuideItem(@RequestBody GuideItemSaveRequest request) throws com.fasterxml.jackson.core.JsonProcessingException {
        log.info("💾 개별 항목 저장 요청 - guideId: {}, 단계: {}, 항목: '{}'", 
            request.getGuideId(), 
            request.getStepNumber(), 
            request.getItemTitle());
        
        GuideProgressResponse response = portfolioGuideService.saveGuideItem(request);
        
        log.info("✅ 개별 항목 저장 성공 - guideId: {}, 진행률: {}%", 
            response.getGuideId(), 
            response.getCompletionPercentage());
        
        // GlobalExceptionHandler에서 예외 처리
        return response;
    }

    /**
     * 🔥 NEW: 전체 가이드 진행상황 저장 API
     * 사용자가 "진행상황 저장" 버튼을 클릭했을 때 모든 내용을 저장
     * 
     * PUT http://localhost:8081/api/portfolio-guide/save-progress
     * 
     * 요청 예시:
     * {
     *   "guideId": 2,
     *   "memberId": 1,
     *   "completionPercentage": 35,
     *   "currentStep": 2,
     *   "guideContent": [
     *     {
     *       "stepNumber": 1,
     *       "stepTitle": "프로젝트 개요",
     *       "stepProgress": 75,
     *       "items": [
     *         {
     *           "title": "프로젝트 제목",
     *           "content": "AI 기반 취업 컨설팅 서비스",
     *           "status": "완료",
     *           "feedback": { ... }
     *         }
     *       ]
     *     }
     *   ]
     * }
     */
    @PutMapping("/save-progress")
    public GuideProgressResponse saveGuideProgress(
            @RequestBody GuideProgressSaveRequest request) throws com.fasterxml.jackson.core.JsonProcessingException {
        
        log.info("💾 전체 가이드 저장 요청 - guideId: {}, 진행률: {}%, 현재 단계: {}", 
            request.getGuideId(), 
            request.getCompletionPercentage(),
            request.getCurrentStep());
        
        GuideProgressResponse response = portfolioGuideService.saveGuideProgress(request);
        
        log.info("✅ 전체 가이드 저장 성공 - guideId: {}, 최종 진행률: {}%", 
            response.getGuideId(), 
            response.getCompletionPercentage());
        
        return response;
    }

    /**
     * 🔥 NEW: 저장된 가이드 내용 조회 API
     * 사용자가 페이지를 다시 열었을 때 이전에 작성한 내용들을 복원
     * 
     * GET http://localhost:8081/api/portfolio-guide/{guideId}/content
     */
    @GetMapping("/{guideId}/content")
    public GuideProgressResponse getGuideContent(@PathVariable Integer guideId) throws JsonProcessingException{
        log.info("📖 가이드 내용 조회 요청 - guideId: {}", guideId);
        
        GuideProgressResponse response = portfolioGuideService.getGuideContent(guideId);
        
        log.info("✅ 가이드 내용 조회 성공 - guideId: {}, 진행률: {}%", 
            guideId, 
            response.getCompletionPercentage());
        
        return response;
    }


    // ===== 기존 API들 =====

    /**
     * 가이드 조회 (단일)
     */
    @GetMapping("/{guideId}")
    public PortfolioGuide getGuide(@PathVariable Integer guideId) {
        log.info("가이드 조회 요청 - guideId: {}", guideId);
        PortfolioGuide guide = portfolioGuideService.getGuideById(guideId);
        
        if (guide == null) {
            throw new NoSuchElementException("존재하지 않는 가이드입니다: " + guideId);
        }
        
        return guide;
    }

    /**
     * 회원별 가이드 목록 조회
     * GET http://localhost:8081/api/portfolio-guide/member/{memberId}
     */
    @GetMapping("/member/{memberId}")
    public List<PortfolioGuide> getGuidesByMember(@PathVariable Integer memberId) {
        log.info("회원별 가이드 목록 조회 - memberId: {}", memberId);
        List<PortfolioGuide> guides = portfolioGuideService.getGuidesByMemberId(memberId);
        return guides;
    }

    /**
     * 가이드의 AI 피드백 조회 (JSONB → Java 객체 변환)
     * GET http://localhost:8081/api/portfolio-guide/{guideId}/feedback
     */
    @GetMapping("/{guideId}/feedback")
    public GuideResult getGuideFeedback(@PathVariable Integer guideId) throws com.fasterxml.jackson.core.JsonProcessingException {
        log.info("가이드 피드백 조회 요청 - guideId: {}", guideId);
        GuideResult feedback = portfolioGuideService.getGuideFeedback(guideId);
        
        return feedback;
    }



    /**
     * 실시간 피드백용 별도 DTO
     */
    public static class RealtimeFeedbackRequest {
        private String inputFieldType;
        private String userInput;
        private Integer currentStep;
        private String jobGroup;
        private String jobRole;
        private Integer careerYears;

        // Getters and Setters
        public String getInputFieldType() { return inputFieldType; }
        public void setInputFieldType(String inputFieldType) { 
            this.inputFieldType = inputFieldType; 
        }
        
        public String getUserInput() { return userInput; }
        public void setUserInput(String userInput) { this.userInput = userInput; }
        
        public Integer getCurrentStep() { return currentStep; }
        public void setCurrentStep(Integer currentStep) { 
            this.currentStep = currentStep; 
        }
        
        public String getJobGroup() { return jobGroup; }
        public void setJobGroup(String jobGroup) { this.jobGroup = jobGroup; }
        
        public String getJobRole() { return jobRole; }
        public void setJobRole(String jobRole) { this.jobRole = jobRole; }
        
        public Integer getCareerYears() { return careerYears; }
        public void setCareerYears(Integer careerYears) { 
            this.careerYears = careerYears; 
        }
    }

    /**
     * 간단한 테스트용 DTO
     */
    public static class SimpleTestRequest {
        private String userInput;
        private String inputFieldType;

        // Getters and Setters
        public String getUserInput() { return userInput; }
        public void setUserInput(String userInput) { this.userInput = userInput; }
        
        public String getInputFieldType() { return inputFieldType; }
        public void setInputFieldType(String inputFieldType) { 
            this.inputFieldType = inputFieldType; 
        }
    }
}



