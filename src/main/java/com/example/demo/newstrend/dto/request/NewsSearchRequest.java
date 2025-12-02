package com.example.demo.newstrend.dto.request;

import lombok.Data;

@Data
public class NewsSearchRequest {
    private String keyword;         // 검색 키워드
    
    private String category;        // IT, 경제, 사회, 정치
    private String sentiment;       // positive, neutral, negative
    private Integer minTrustScore;  // 최소 신뢰도
    private Boolean biasOnly;       // 편향된 뉴스만
    private Integer limit = 15;     // 기본 10개
}
