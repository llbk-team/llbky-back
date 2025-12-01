package com.example.demo.newstrend.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewsAnalysisResult {
    private String originalContent;        // 웹스크래핑된 원문
    private NewsSummaryResponse analysis;  // AI 분석 결과
    private List<NewsKeywordResponse> keywords; // 추출된 키워드
    private String finalSummary;// 최종 요약 (중립화 적용됨)

}
