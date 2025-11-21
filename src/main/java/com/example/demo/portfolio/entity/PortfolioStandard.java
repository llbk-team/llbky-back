package com.example.demo.portfolio.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PortfolioStandard {
    private Integer standardId;      // 평가 기준 ID
    private String standardName;     // 평가 항목명
    private String standardDescription;  // 평가 기준 설명
    private String promptTemplate;   // AI 프롬프트 템플릿
    private Integer weightPercentage = 20; 
}
