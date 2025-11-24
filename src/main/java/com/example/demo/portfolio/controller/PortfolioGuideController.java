package com.example.demo.portfolio.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.portfolio.dto.PortfolioGuideResult;
import com.example.demo.portfolio.dto.request.PortfolioGuideRequest;
import com.example.demo.portfolio.service.PortfolioGuideService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 가이드 코칭 테스트 컨트롤러
 * - 실시간 포트폴리오 작성 코칭 API
 * - DB에서 평가 지침(promptTemplate)을 자동으로 조회하여 LLM 프롬프트 생성
 */
@RestController
@RequestMapping("/api/portfolio-guide")
@RequiredArgsConstructor
@Slf4j
public class PortfolioGuideController {

    @Autowired
    private  PortfolioGuideService portfolioGuideService;

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
    public PortfolioGuideResult getRealtimeCoaching(@RequestBody PortfolioGuideRequest request) {
        log.info("실시간 코칭 요청 - memberId: {}, 입력 필드: {}, 입력 내용: '{}'", 
            request.getMemberId(), 
            request.getInputFieldType(), 
            request.getUserInput() != null ? 
            request.getUserInput().substring(0, Math.min(30, request.getUserInput().length())) : "null");
        
        return portfolioGuideService.provideCoaching(request);
    }

}
