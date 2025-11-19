package com.example.demo.interview.entity;

import java.time.LocalDateTime;

import lombok.Data;

// 면접에 활용된 서류들
@Data
public class InterviewDocument {
  private Integer interviewDocId; //면접서류ID
  private Integer sessionId;  //면접ID
  
  private String documentType;  //서류 종류(이력서/자소서/포폴)
  private Integer documentId; //서류ID

  private LocalDateTime createdAt;  //생성일
}
