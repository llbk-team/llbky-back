package com.example.demo.learning.dto.request;

import java.util.List;

import lombok.Data;

@Data
// 학습 플랜 생성 요청
public class LearningRequest {
  private String job;
  private List<String> purposes;
  private List<String> Skills;
  private int weeklyHours;
}
