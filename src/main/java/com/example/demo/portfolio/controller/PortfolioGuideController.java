package com.example.demo.portfolio.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.portfolio.dto.GuideResult;
import com.example.demo.portfolio.dto.request.GuideRequest;
import com.example.demo.portfolio.entity.PortfolioGuide;
import com.example.demo.portfolio.service.PortfolioGuideService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 가이드 코칭 컨트롤러
 * - 실시간 포트폴리오 작성 코칭 API
 * - 가이드 조회 및 관리 API
 */
@RestController
@RequestMapping("/portfolio-guide")
@RequiredArgsConstructor
@Slf4j
public class PortfolioGuideController {

    private final PortfolioGuideService portfolioGuideService;

    /**
     * 실시간 포트폴리오 코칭 API
     * POST http://localhost:8080/portfolio-guide/coaching
     * Request Body: { memberId, inputFieldType, userInput, currentStep, guideId }
     */
    @PostMapping("/coaching")
    public ResponseEntity<GuideResult> getRealtimeCoaching(@RequestBody GuideRequest request) throws Exception {
        log.info("코칭 요청 - memberId: {}, inputFieldType: {}", 
            request.getMemberId(), request.getInputFieldType());
        
        GuideResult result = portfolioGuideService.provideCoaching(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 가이드 조회 (단일)
     * GET http://localhost:8080/portfolio-guide/{guideId}
     */
    @GetMapping("/{guideId}")
    public ResponseEntity<PortfolioGuide> getGuide(@PathVariable Integer guideId) {
        log.info("가이드 조회 - guideId: {}", guideId);
        PortfolioGuide guide = portfolioGuideService.getGuideById(guideId);
        return ResponseEntity.ok(guide);
    }

    /**
     * 회원별 가이드 목록 조회
     * GET http://localhost:8080/portfolio-guide/member/{memberId}
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<PortfolioGuide>> getGuidesByMember(@PathVariable Integer memberId) {
        log.info("회원별 가이드 목록 조회 - memberId: {}", memberId);
        List<PortfolioGuide> guides = portfolioGuideService.getGuidesByMemberId(memberId);
        return ResponseEntity.ok(guides);
    }

    /**
     * 가이드의 AI 피드백 조회 (JSONB → GuideResult 변환)
     * GET http://localhost:8080/portfolio-guide/{guideId}/feedback
     */
    @GetMapping("/{guideId}/feedback")
    public ResponseEntity<GuideResult> getGuideFeedback(@PathVariable Integer guideId) throws Exception {
        log.info("피드백 조회 - guideId: {}", guideId);
        GuideResult feedback = portfolioGuideService.getGuideFeedback(guideId);
        return ResponseEntity.ok(feedback);
    }
}












