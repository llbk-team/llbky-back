package com.example.demo.newstrend.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class NewsAnalysisResponse {
    private Integer summaryId;
    private String title;
    private String sourceName;
    private String sourceUrl;
    private LocalDate publishedAt;
    
    // 요약 정보
    private String summaryText;
    private String detailSummary;
    
    // AI 분석 결과
    private String sentiment;
    private Integer trustScore;
    private Boolean biasDetected;
    private String biasType;
    private String category;
    
    // 키워드
    private List<NewsKeywordResponse> keywords; 
    
    private LocalDateTime createdAt;
    
    
}
