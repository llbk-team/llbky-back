package com.example.demo.portfolio.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PortfolioImage {
  private Integer imageId; //이미지ID
  private Integer portfolioId; 
  private int pageNo; //페이지 번호
  private String filename; //파일 이름
  private Byte[] filedata; //데이터
  private String filetype; //파일 타입
  private LocalDateTime createdAt; //생성일
  private String pageFeedback; // 페이지별 AI 피드백
}
