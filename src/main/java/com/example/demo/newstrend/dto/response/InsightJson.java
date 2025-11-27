package com.example.demo.newstrend.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class InsightJson {
  private SummaryCard summarycard; // UI 상단 요약 카드
  private List<String> keywordTrend; // 트렌드 키워드
  private List<IndustrySentiment> industrySentiment; // 시장 분위기
  private List<String> marketInsight;
  private String finalSummary;
}


