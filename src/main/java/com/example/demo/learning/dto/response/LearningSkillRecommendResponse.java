package com.example.demo.learning.dto.response;

import java.util.List;

import lombok.Data;

@Data
// 분석한 피드백을 바탕으로 부족한 역량 추천 응답
public class LearningSkillRecommendResponse {
  private List<String> feedback; 
}
