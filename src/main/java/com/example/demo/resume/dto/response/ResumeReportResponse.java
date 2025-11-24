package com.example.demo.resume.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class ResumeReportResponse {
  // // 문장 개선 제안(수정 전, 수정 후)
  // private List<RewriteSuggestion> rewriteSuggestions;

  // 점수(경력 기술, 적합도, 완성도)
  private Score score;
  // 강점
  private List<String> strengths;
  // 개선점
  private List<String> weaknesses;

  // 포트폴리오 제안
  private List<String> portfolioSuggestions;
  // 자소서 제안
  private List<String> coverLetterSuggestions;
}
