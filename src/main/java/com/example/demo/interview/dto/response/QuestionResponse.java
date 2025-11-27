package com.example.demo.interview.dto.response;

import lombok.Data;

// AI에게서 받은 면접 예상 질문
@Data
public class QuestionResponse {
  private Integer sessionId;
  private Integer questionId; 
  private String aiQuestion;
}
