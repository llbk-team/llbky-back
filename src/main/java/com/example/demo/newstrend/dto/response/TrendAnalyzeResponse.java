package com.example.demo.newstrend.dto.response;

import lombok.Data;

@Data
public class TrendAnalyzeResponse {
  private TrendGraph trendJson;
  private InsightJson insightJson;
}
