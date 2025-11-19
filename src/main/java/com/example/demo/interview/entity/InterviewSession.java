package com.example.demo.interview.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class InterviewSession {
  private Integer sessionId;  //면접 ID
  private Integer memberId; //사용자 ID

  private String type;  //면접 유형
  private String targetCompany; //희망 기업
  private String reportFeedback;  //종합 피드백
  
  private LocalDateTime createdAt;  //생성일
  private LocalDateTime updatedAt;  //수정일
}
