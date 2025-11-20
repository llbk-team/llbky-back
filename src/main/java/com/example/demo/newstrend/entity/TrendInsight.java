package com.example.demo.newstrend.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TrendInsight {
  private Integer trendId; // 트렌드 ID(PK)
  private Integer memberId; // 멤버 ID
  private String baseJobTitle; // LLM 분석 기준 직무 -> member_id의 현재 직무가 바뀌어도 영향 없도록 하기위해서 별도 저장

  // 분석한 트렌드 기간
  private LocalDate startDate;
  private LocalDate endDate;

  private String insightJson; // LLM이 분석한 트렌드 페이지 전체 구성 UI 데이터 JSONB
  private LocalDateTime createdAt;
}
