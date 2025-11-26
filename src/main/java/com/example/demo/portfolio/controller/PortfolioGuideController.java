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

    // 실시간 포트폴리오 코칭 API

    @PostMapping("/coaching")
    public GuideResult getRealtimeCoaching(@RequestBody GuideRequest request) throws JsonProcessingException {

        return portfolioGuideService.provideCoaching(request);
    }

    // 피드백 회원 정보 포함
    @PostMapping("/feedback")
    public GuideResult getRealtimeFeedback(@RequestBody RealtimeFeedbackRequest request)
            throws com.fasterxml.jackson.core.JsonProcessingException {

    //             Member member = memberService.getMemberId(request.getMemberId());
    //             if (member == null) {
    //             throw new NoSuchElementException("존재하지 않는 회원입니다: " + request.getMemberId());
    // }
        GuideRequest fullRequest = GuideRequest.builder()
                // .memberId(member.getMemberId())
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

    //🧪 Postman 테스트용 간편 API - 최소한의 정보만 입력

    @PostMapping("/test-example")
    public GuideResult testExample(@RequestBody SimpleTestRequest request)
            throws com.fasterxml.jackson.core.JsonProcessingException {

         // 🔥 수정: 프론트에서 받은 값 사용, 없으면 기본값
    GuideRequest fullRequest = GuideRequest.builder()
            .inputFieldType(request.getInputFieldType() != null ? request.getInputFieldType() : "프로젝트 제목")
            .userInput(request.getUserInput())
            .currentStep(request.getCurrentStep() != null ? request.getCurrentStep() : 1)
            .jobGroup(request.getJobGroup() != null ? request.getJobGroup() : "개발")
            .jobRole(request.getJobRole() != null ? request.getJobRole() : "백엔드")
            .careerYears(request.getCareerYears() != null ? request.getCareerYears() : 2)
            .build();

    GuideResult response = portfolioGuideService.provideCoaching(fullRequest);

        // 예시만 출력하는 버전도 추가
        if (response.getExamples() != null && !response.getExamples().isEmpty()) {
            for (int i = 0; i < response.getExamples().size(); i++) {
            }
        }
        // GlobalExceptionHandler에서 예외 처리
        return response;
    }

    
     // 🔥 NEW: 개별 항목 저장 API
    //사용자가 특정 항목을 완료했을 때 해당 내용을 가이드에 저장
     
    @PostMapping("/save-item")
    public GuideProgressResponse saveGuideItem(@RequestBody GuideItemSaveRequest request)
            throws com.fasterxml.jackson.core.JsonProcessingException {
       
        GuideProgressResponse response = portfolioGuideService.saveGuideItem(request);
        // GlobalExceptionHandler에서 예외 처리
        return response;
    }

    //    🔥 NEW: 전체 가이드 진행상황 저장 API
    //사용자가 "진행상황 저장" 버튼을 클릭했을 때 모든 내용을 저장
      
     
     
    @PutMapping("/save-progress")
    public GuideProgressResponse saveGuideProgress(
            @RequestBody GuideProgressSaveRequest request) throws com.fasterxml.jackson.core.JsonProcessingException {

        GuideProgressResponse response = portfolioGuideService.saveGuideProgress(request);

        return response;
    }

    
    // 🔥 NEW: 저장된 가이드 내용 조회 API
    // 사용자가 페이지를 다시 열었을 때 이전에 작성한 내용들을 복원
    
    @GetMapping("/{guideId}/content")
    public GuideProgressResponse getGuideContent(@PathVariable Integer guideId) throws JsonProcessingException {
       
        GuideProgressResponse response = portfolioGuideService.getGuideContent(guideId);
        return response;
    }

    // ===== 기존 API들 =====

    /**
     * 가이드 조회 (단일)
     */
    @GetMapping("/{guideId}")
    public PortfolioGuide getGuide(@PathVariable Integer guideId) {
        
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
       
        List<PortfolioGuide> guides = portfolioGuideService.getGuidesByMemberId(memberId);
        return guides;
    }

    /**
     * 가이드의 AI 피드백 조회 (JSONB → Java 객체 변환)
     * GET http://localhost:8081/api/portfolio-guide/{guideId}/feedback
     */
    @GetMapping("/{guideId}/feedback")
    public GuideResult getGuideFeedback(@PathVariable Integer guideId)
            throws com.fasterxml.jackson.core.JsonProcessingException {
     
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
        public String getInputFieldType() {
            return inputFieldType;
        }

        public void setInputFieldType(String inputFieldType) {
            this.inputFieldType = inputFieldType;
        }

        public String getUserInput() {
            return userInput;
        }

        public void setUserInput(String userInput) {
            this.userInput = userInput;
        }

        public Integer getCurrentStep() {
            return currentStep;
        }

        public void setCurrentStep(Integer currentStep) {
            this.currentStep = currentStep;
        }

        public String getJobGroup() {
            return jobGroup;
        }

        public void setJobGroup(String jobGroup) {
            this.jobGroup = jobGroup;
        }

        public String getJobRole() {
            return jobRole;
        }

        public void setJobRole(String jobRole) {
            this.jobRole = jobRole;
        }

        public Integer getCareerYears() {
            return careerYears;
        }

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
    
    // 🔥 추가: 하드코딩된 값들을 받을 수 있도록 필드 추가
    private String jobGroup;
    private String jobRole;
    private Integer careerYears;
    private Integer currentStep;

    // Getters and Setters
    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public String getInputFieldType() {
        return inputFieldType;
    }

    public void setInputFieldType(String inputFieldType) {
        this.inputFieldType = inputFieldType;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getJobRole() {
        return jobRole;
    }

    public void setJobRole(String jobRole) {
        this.jobRole = jobRole;
    }

    public Integer getCareerYears() {
        return careerYears;
    }

    public void setCareerYears(Integer careerYears) {
        this.careerYears = careerYears;
    }

    public Integer getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Integer currentStep) {
        this.currentStep = currentStep;
    }
    }
}
