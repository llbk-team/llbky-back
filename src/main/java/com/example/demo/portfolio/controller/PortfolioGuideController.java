// package com.example.demo.portfolio.controller;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import com.example.demo.portfolio.dto.PortfolioGuideResult;
// import com.example.demo.portfolio.dto.request.PortfolioGuideRequest;
// import com.example.demo.portfolio.service.PortfolioGuideService;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

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
