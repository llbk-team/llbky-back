package com.example.demo.portfolio.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PortfolioGuide {
   
    @JsonProperty("guide_id")
    private Integer guideId;        // 가이드 ID

    @JsonProperty("member_id")
    private Integer memberId;       // 회원 ID

    @JsonProperty("title")
    private String title = "새 포트폴리오 가이드";  // 가이드 제목 (기본값)

    @JsonProperty("standard_id")
    private Integer standardId;     // 평가 기준 ID (portfolio_standard 참조)

    @JsonProperty("guide_content")
    private String guideContent;    // 가이드 작성 내용 (JSONB)

    @JsonProperty("completion_percentage")
    private Integer completionPercentage = 0;  // 진행률 (0-100)

    @JsonProperty("is_completed")
    private Boolean isCompleted = false;       // 완료 여부

    @JsonProperty("current_step")
    private Integer currentStep = 1;           // 현재 작성 중인 단계

    @JsonProperty("total_steps")
    private Integer totalSteps = 5;            // 전체 단계 수

    @JsonProperty("guide_feedback")
    private String guideFeedback;   // AI 가이드 코칭 결과 (JSONB)

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
