package com.example.demo.resume.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.ai.ResumeAiAgent;
import com.example.demo.resume.dao.ResumeDao;
import com.example.demo.resume.dto.response.ResumeReportResponse;
import com.example.demo.resume.entity.Resume;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ResumeService {
  @Autowired
  private ResumeDao resumeDao;
  @Autowired
  private ResumeAiAgent resumeAiAgent;

  // 이력서 작성
  public int createResume(Resume resume) {

    resumeDao.insertResume(resume);
    return resume.getResumeId();
  }

  // 이력서 조회
  public Resume getResume(int resumeId) {
    return resumeDao.selectResumeById(resumeId);
  }

  // AI 분석
  public ResumeReportResponse analyzeResume(int memberId, int resumeId) throws Exception {
    ResumeReportResponse aiResult = resumeAiAgent.analyze(memberId, resumeId);
    log.info("AI 분석 결과: {}", aiResult);
    return aiResult;
  }

  // AI 피드백 반영
  public int applyFeedback(Integer resumeId, String feedbackJson) {
    return resumeDao.updateResumeFeedback(resumeId, feedbackJson);
  }
}
