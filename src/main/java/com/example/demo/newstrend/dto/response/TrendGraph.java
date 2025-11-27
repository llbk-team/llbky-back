package com.example.demo.newstrend.dto.response;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class TrendGraph {
  private List<String> keywords; // x축 (키워드)
  private List<Integer> counts; // y축 (점수)
  private Map<String, Object> rawTrendData; // 원본 데이터
}
