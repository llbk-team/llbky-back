package com.example.demo.portfolio.entity;


import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Portfolio {
  private Integer portfolioId;//포트폴리오 ID
  private String title;//포트폴리오 제목
  private Byte[] file; //파일
  private String portfolioFeedback; //포트폴리오 피드백
  private LocalDateTime createdAt; //생성일
  private LocalDateTime updatedAt; // 수정일


}
