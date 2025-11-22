package com.example.demo.resume.dto.request;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class ResumeReportRequest {
  // 기본 정보
  private String name; // 이름
  private String email; // 이메일
  private String phone; // 연락처

  // 교육
  private List<Map<String, Object>> education;

  // 경력
  private List<Map<String, Object>> career;

  // 스킬
  private List<String> skills;

  // 자격증 및 수상
  private List<Map<String, Object>> certificates;

  // 희망 직무
  private String targetJob;
}
