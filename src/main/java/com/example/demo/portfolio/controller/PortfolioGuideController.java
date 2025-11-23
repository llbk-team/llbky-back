// package com.example.demo.portfolio.controller;

<<<<<<< Updated upstream
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import com.example.demo.portfolio.dto.PortfolioGuideResult;
// import com.example.demo.portfolio.dto.request.PortfolioGuideRequest;
// import com.example.demo.portfolio.service.PortfolioGuideService;
=======
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.portfolio.dto.PortfolioGuideResult;
import com.example.demo.portfolio.dto.request.PortfolioGuideRequest;
import com.example.demo.portfolio.entity.PortfolioGuide;
import com.example.demo.portfolio.service.PortfolioGuideService;
>>>>>>> Stashed changes

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

<<<<<<< Updated upstream
// /**
//  * 포트폴리오 가이드 코칭 테스트 컨트롤러
//  * - 실시간 포트폴리오 작성 코칭 API
//  * - DB에서 평가 지침(promptTemplate)을 자동으로 조회하여 LLM 프롬프트 생성
//  */
// @RestController
// @RequestMapping("/api/portfolio-guide")
// @RequiredArgsConstructor
// @Slf4j
// public class PortfolioGuideController {

//     @Autowired
//     private  PortfolioGuideService portfolioGuideService;

//     /**
//      * 실시간 포트폴리오 코칭 API
//      * 
//      * @param request - memberId, userInput, inputFieldType 등 포함
//      * @return 코칭 결과 (점수, 제안사항, 예시, 다음 단계 가이드)
//      * 
//      * 테스트 예시:
//      * POST /api/portfolio-guide/coaching
//      * {
//      *   "memberId": 1,
//      *   "guideId": 1,
//      *   "currentStep": 1,
//      *   "inputFieldType": "프로젝트 제목",
//      *   "userInput": "사용자 맞춤형 여행 추천 앱"
//      * }
//      */
//     @PostMapping("/coaching")
//     public PortfolioGuideResult getRealtimeCoaching(@RequestBody PortfolioGuideRequest request) {
//         log.info("실시간 코칭 요청 - memberId: {}, 입력 필드: {}, 입력 내용: '{}'", 
//             request.getMemberId(), 
//             request.getInputFieldType(), 
//             request.getUserInput() != null ? 
//                 request.getUserInput().substring(0, Math.min(30, request.getUserInput().length())) : "null");
        
//         return portfolioGuideService.provideCoaching(request);
//     }

//     /**
//      * 빠른 코칭 테스트 API (memberId 1 고정)
//      * 
//      * 테스트 예시:
//      * POST /api/portfolio-guide/quick-test
//      * {
//      *   "inputFieldType": "프로젝트 목적",
//      *   "userInput": "여행지 추천 서비스의 개인화 알고리즘 개선"
//      * }
//      */
//     @PostMapping("/quick-test")
//     public PortfolioGuideResult quickTest(@RequestBody QuickTestRequest quickRequest) {
//         log.info("빠른 테스트 요청 - 입력 필드: {}, 입력 내용: '{}'", 
//             quickRequest.getInputFieldType(), 
//             quickRequest.getUserInput());
        
//         PortfolioGuideRequest request = PortfolioGuideRequest.builder()
//                 .memberId(1) // 테스트용 고정 memberId
//                 .guideId(1)
//                 .currentStep(1)
//                 .inputFieldType(quickRequest.getInputFieldType())
//                 .userInput(quickRequest.getUserInput())
//                 .build();
        
//         return portfolioGuideService.provideCoaching(request);
//     }

//     /**
//      * 빠른 테스트용 요청 DTO
//      */
//     public static class QuickTestRequest {
//         private String inputFieldType;
//         private String userInput;

//         public String getInputFieldType() {
//             return inputFieldType;
//         }

//         public void setInputFieldType(String inputFieldType) {
//             this.inputFieldType = inputFieldType;
//         }

//         public String getUserInput() {
//             return userInput;
//         }

//         public void setUserInput(String userInput) {
//             this.userInput = userInput;
//         }
//     }
// }
=======
import java.util.List;

/**
 * 포트폴리오 가이드 코칭 컨트롤러 - FinalFeedbackAgent 스타일
 */
@RestController
@RequestMapping("/api/portfolio-guide")
@RequiredArgsConstructor
@Slf4j
public class PortfolioGuideController {

    private final PortfolioGuideService portfolioGuideService;

    /**
     * 실시간 포트폴리오 코칭 - FinalFeedbackAgent 스타일
     * POST http://localhost:8081/api/portfolio-guide/coaching
     */
    @PostMapping("/coaching")
    public ResponseEntity<PortfolioGuideResult> getPortfolioCoaching(
            @RequestBody PortfolioGuideRequest request) {
        
        try {
            log.info("실시간 코칭 요청 - memberId: {}, 입력 필드: {}, 입력 내용: '{}'", 
                request.getMemberId(), 
                request.getInputFieldType(), 
                request.getUserInput() != null ? 
                    request.getUserInput().substring(0, Math.min(30, request.getUserInput().length())) 
                    : "null");
            
            // AI 코칭 생성 - FinalFeedbackAgent와 동일한 패턴
            PortfolioGuideResult response = portfolioGuideService.provideCoaching(request);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("코칭 생성 중 오류 발생", e);
            PortfolioGuideResult errorResponse = PortfolioGuideResult.builder()
                .success(false)
                .coachingMessage("코칭 생성 중 오류가 발생했습니다: " + e.getMessage())
                .build();
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 간단한 실시간 피드백 - 회원 정보 없이 직접 입력
     * POST http://localhost:8081/api/portfolio-guide/feedback
     */
    @PostMapping("/feedback")
    public ResponseEntity<PortfolioGuideResult> getRealtimeFeedback(
            @RequestBody RealtimeFeedbackRequest request) {
        
        try {
            log.info("실시간 피드백 요청 - 필드: {}, 직군: {}, 직무: {}", 
                request.getInputFieldType(), request.getJobGroup(), request.getJobRole());
            
            PortfolioGuideRequest fullRequest = PortfolioGuideRequest.builder()
                .inputFieldType(request.getInputFieldType())
                .userInput(request.getUserInput())
                .currentStep(request.getCurrentStep())
                .jobGroup(request.getJobGroup())
                .jobRole(request.getJobRole())
                .careerYears(request.getCareerYears())
                .build();
            
            PortfolioGuideResult response = portfolioGuideService.provideCoaching(fullRequest);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("피드백 생성 중 오류 발생", e);
            PortfolioGuideResult errorResponse = PortfolioGuideResult.builder()
                .success(false)
                .coachingMessage("피드백 생성 중 오류가 발생했습니다: " + e.getMessage())
                .build();
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 가이드 조회 (단일)
     * GET http://localhost:8081/api/portfolio-guide/{guideId}
     */
    @GetMapping("/{guideId}")
    public ResponseEntity<PortfolioGuide> getGuide(@PathVariable Integer guideId) {
        try {
            log.info("가이드 조회 요청 - guideId: {}", guideId);
            PortfolioGuide guide = portfolioGuideService.getGuideById(guideId);
            
            if (guide == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(guide);
        } catch (Exception e) {
            log.error("가이드 조회 중 오류 발생", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 회원별 가이드 목록 조회
     * GET http://localhost:8081/api/portfolio-guide/member/{memberId}
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<PortfolioGuide>> getGuidesByMember(@PathVariable Integer memberId) {
        try {
            log.info("회원별 가이드 목록 조회 - memberId: {}", memberId);
            List<PortfolioGuide> guides = portfolioGuideService.getGuidesByMemberId(memberId);
            return ResponseEntity.ok(guides);
        } catch (Exception e) {
            log.error("가이드 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 가이드의 AI 피드백 조회 (JSONB → Java 객체 변환)
     * GET http://localhost:8081/api/portfolio-guide/{guideId}/feedback
     */
    @GetMapping("/{guideId}/feedback")
    public ResponseEntity<PortfolioGuideResult> getGuideFeedback(@PathVariable Integer guideId) {
        try {
            log.info("가이드 피드백 조회 요청 - guideId: {}", guideId);
            PortfolioGuideResult feedback = portfolioGuideService.getGuideFeedback(guideId);
            
            if (feedback == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            log.error("피드백 조회 중 오류 발생", e);
            return ResponseEntity.status(500).build();
        }
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
}

//==============================================================================
// Postman 테스트 설정 - FinalFeedbackAgent 스타일
//==============================================================================

/*
1. 회원 기반 코칭 테스트
POST http://localhost:8081/api/portfolio-guide/coaching
Content-Type: application/json

{
  "memberId": 1,
  "inputFieldType": "project_description",
  "currentStep": 2,
  "userInput": "Vue.js와 Spring Boot를 이용해 쇼핑몰 웹사이트를 개발했습니다. 사용자 인증, 상품 관리, 결제 시스템을 구현했고 5명의 팀원과 협력하여 3개월간 진행했습니다."
}

2. 직접 입력 방식 실시간 피드백 테스트  
POST http://localhost:8081/api/portfolio-guide/feedback
Content-Type: application/json

{
  "inputFieldType": "project_description",
  "userInput": "React와 Node.js로 채팅 앱을 만들었습니다.",
  "currentStep": 1,
  "jobGroup": "개발",
  "jobRole": "프론트엔드 개발자",
  "careerYears": 2
}

예상 응답:
{
  "success": true,
  "coachingMessage": "프로젝트 경험이 잘 드러나네요! 기술 스택과 팀워크를 언급한 점이 좋습니다. 다만 구체적인 성과나 해결한 문제를 추가하면 더욱 인상적일 것 같아요.",
  "appropriatenessScore": 7,
  "suggestions": [
    "프로젝트 규모를 구체적으로 명시하세요 (예: 월 활성 사용자 수, 처리 성능)",
    "개발 과정에서 해결한 기술적 문제나 도전 과제를 설명하세요",
    "프로젝트의 비즈니스 임팩트나 성과를 수치로 표현하세요"
  ],
  "examples": [
    "Vue.js 3 + Spring Boot 2.7로 월 10만 방문자의 쇼핑몰 개발, 페이지 로딩 속도 40% 개선",
    "RESTful API 설계부터 배포까지 담당하여 팀 개발 효율성 30% 향상에 기여"
  ],
  "nextStepGuide": "이제 사용한 기술들에 대한 상세한 경험과 선택 이유를 작성해보세요. 각 기술로 해결한 구체적인 문제를 중심으로 설명하면 좋습니다.",
  "progressPercentage": 60
}
*/
>>>>>>> Stashed changes
