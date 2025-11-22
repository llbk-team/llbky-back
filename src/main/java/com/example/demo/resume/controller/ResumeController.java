package com.example.demo.resume.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.resume.dto.request.ResumeReportRequest;
import com.example.demo.resume.dto.response.ResumeReportResponse;
import com.example.demo.resume.entity.Resume;
import com.example.demo.resume.service.ResumeService;

@RestController
@RequestMapping("/resume")
public class ResumeController {
  @Autowired
  private ResumeService resumeService;

  // 이력서 생성
  @PostMapping("/create")
  public int createResume(@RequestBody Resume resume) {
    return resumeService.createResume(resume);
  }

  // 이력서 조회
  @GetMapping("/{resumeId}")
  public Resume getResume(@PathVariable int resumeId) {
    return resumeService.getResume(resumeId);
  }

  // AI 분석
  @PostMapping("/analyze/{memberId}/{resumeId}")
  public ResumeReportResponse analyze(
    @PathVariable int memberId,
    @PathVariable int resumeId) {      
      return resumeService.analyzeResume(memberId, resumeId);
  }

  // AI 피드백 반영
  @PostMapping("/{resumeId}/apply-feedback")
  public int applyFeedback(
      @PathVariable int resumeId,
      @RequestBody String feedbackJson) {
    return resumeService.applyFeedback(resumeId, feedbackJson);
  }

}
