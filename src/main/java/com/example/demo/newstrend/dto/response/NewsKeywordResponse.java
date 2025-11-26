package com.example.demo.newstrend.dto.response;

import lombok.Data;

@Data
public class NewsKeywordResponse {
    private String keyword;
    private String type;        // 직무, 기술, 산업, 기타
    private Double importanceScore;
}
