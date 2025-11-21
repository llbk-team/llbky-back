package com.example.demo.portfolio.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PortfolioStandard {
    @JsonProperty("standard_id")
    private Integer standardId;      // 평가 기준 ID

    @JsonProperty("standard_name")
    private String standardName;     // 평가 항목명

    @JsonProperty("standard_description")
    private String standardDescription;  // 평가 기준 설명

    @JsonProperty("prompt_template")
    private String promptTemplate;   // AI 프롬프트 템플릿

    @JsonProperty("weight_percentage")
    private Integer weightPercentage = 20; 
}
