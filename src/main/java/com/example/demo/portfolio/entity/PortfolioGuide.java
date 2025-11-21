package com.example.demo.portfolio.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PortfolioGuide {
   
    private Integer guideId;        // 가이드 ID
    private Integer memberId;       // 회원 ID
    private Integer standardId;     // 평가 기준 ID (portfolio_standard 참조)

    private String title = "새 포트폴리오 가이드";  // 가이드 제목 (기본값)
    private String guideContent;    // 가이드 작성 내용 (JSONB)

    private Integer completionPercentage = 0;  // 진행률 (0-100)
    private Boolean isCompleted = false;       // 완료 여부
    private Integer currentStep = 1;           // 현재 작성 중인 단계
    private Integer totalSteps = 5;            // 전체 단계 수

    private String guideFeedback;   // AI 가이드 코칭 결과 (JSONB)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
