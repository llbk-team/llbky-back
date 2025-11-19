package com.example.demo.learning.dto.response;

import java.util.List;

import lombok.Data;

@Data
// 학습 플랜 생성 응답
public class LearningResponse {
  private Integer memberId;
  private String title;
  private List<LearningWeekResponse> weeks;
}
