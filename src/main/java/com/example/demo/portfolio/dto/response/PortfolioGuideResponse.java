package com.example.demo.portfolio.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/**
 * 포트폴리오 가이드 조회 응답 DTO
 */
@Data
@Builder
public class PortfolioGuideResponse {
    private Integer guideId;
    private Integer memberId;
    private Integer standardId;
    private String title;
    private String guideContent;         // JSONB
    private Integer completionPercentage;
    private Boolean isCompleted;
    private Integer currentStep;
    private Integer totalSteps;
    private String guideFeedback;        // JSONB
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 추가 정보 (Join 데이터)
    private String standardName;         // 평가 기준명
    private String jobGroup;             // 직군
    private String jobRole;              // 직무
}
