package com.example.demo.portfolio.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PortfolioGuide {
   private Integer guideId;              // 가이드 ID (Primary Key)
    private Integer memberId;             // 회원 ID (Foreign Key)
    private String title;                 // 가이드 제목
    private Integer criteriaId;           // 평가 기준 ID (Foreign Key)
    private String guideContent;          // 가이드 내용 (JSONB → String)
    private Integer completionPercentage; // 완료율 (0-100)
    private Boolean isCompleted;          // 완료 여부
    private Integer currentStep;          // 현재 단계
    private Integer totalSteps;           // 전체 단계 수
    private String guideFeedback;         // AI 가이드 피드백 (JSONB → String)
    private LocalDateTime createdAt;      // 생성 시간
    private LocalDateTime updatedAt;      // 수정 시간
}
