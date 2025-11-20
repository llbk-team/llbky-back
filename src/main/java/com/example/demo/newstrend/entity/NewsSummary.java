package com.example.demo.newstrend.entity;


import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class NewsSummary {
  private Integer summaryId; // 뉴스 요약 ID(PK)
  private Integer memberId; // 멤버 ID
  private String sourceName; // 언론사 이름
  private String sourceUrl; // 원문 뉴스 URL
  private String title; // 뉴스 제목
  private LocalDate publishedAt; // 발행일
  private String summaryText; // 3줄 요약 내용
  private String detailSummary; // 긴 요약
  private String analysisJson; // 감정/편향/신뢰도 통합
  private String keywordsJson;
  private LocalDateTime createdAt; // 생성일
}
