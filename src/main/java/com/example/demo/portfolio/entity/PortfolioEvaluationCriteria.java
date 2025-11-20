package com.example.demo.portfolio.entity;

import lombok.Data;

@Data
public class PortfolioEvaluationCriteria {
    private Integer criteriaId;       // 평가 기준 ID (Primary Key)
    private String jobGroup;          // 직군 (예: IT, 디자인, 마케팅)
    private String jobRole;           // 직무 (예: 백엔드, 프론트엔드)
    private String careerLevel;       // 경력 수준 (예: 신입, 주니어, 시니어)
    private String criteriaName;      // 평가 항목명
    private String criteriaDescription; // 평가 항목 설명
    private String promptTemplate;    // AI 평가용 프롬프트 템플릿
    private Integer weightPercentage; // 가중치 (%)
    private Boolean isActive;         // 활성 상태
}
