package com.example.demo.portfolio.dto.request;

import lombok.Data;

/**
 * 포트폴리오 가이드 업데이트 요청 DTO
 */
@Data
public class UpdatePortfolioGuideRequest {
    private Integer guideId;              // 가이드 ID
    private String title;                 // 가이드 제목
    private String guideContent;          // 가이드 작성 내용 (JSONB)
    private Integer completionPercentage; // 진행률 (0-100)
    private Boolean isCompleted;          // 완료 여부
    private Integer currentStep;          // 현재 단계
    private String guideFeedback;         // AI 코칭 결과 (JSONB)
}
