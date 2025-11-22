package com.example.demo.resume.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.ai.ResumeAiAgent;
import com.example.demo.resume.dao.ResumeDao;
import com.example.demo.resume.dto.request.ResumeReportRequest;
import com.example.demo.resume.dto.response.ResumeReportResponse;
import com.example.demo.resume.entity.Resume;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ResumeService {
  @Autowired
  private ResumeDao resumeDao;
  @Autowired
  private ResumeAiAgent resumeAiAgent;

  private ObjectMapper mapper = new ObjectMapper();
  
  private String toJsonString(Object value){
    try{
      if(value == null)
        return null;
      // 이미 문자열이면 그대로
      if(value instanceof String)
        return (String) value;

      return mapper.writeValueAsString(value);
    } catch (Exception e){
      throw new RuntimeException("JSON 변환 실패", e);
    }
  }

  // 이력서 작성
  public int createResume(Resume resume){
    // JSON 객체 -> String(JSON)
    resume.setCareerInfo(toJsonString(resume.getCareerInfo()));
    resume.setEducationInfo(toJsonString(resume.getEducationInfo()));
    resume.setSkills(toJsonString(resume.getSkills()));
    resume.setCertificates(toJsonString(resume.getCertificates()));
    resume.setAwards(toJsonString(resume.getAwards()));
    resume.setActivities(toJsonString(resume.getActivities()));
    resume.setResumeFeedback(toJsonString(resume.getResumeFeedback()));

    resumeDao.insertResume(resume);
    return resume.getResumeId();
  }

  // 이력서 조회
  public Resume getResume(int resumeId){
    return resumeDao.selectResumeById(resumeId);
  }

  // AI 분석
  public ResumeReportResponse analyzeResume(int memberId, int resumeId){
    ResumeReportResponse aiResult =  resumeAiAgent.analyze(memberId ,resumeId);
    log.info("AI 분석 결과: {}", aiResult);
    return aiResult;
  }

  // AI 피드백 반영
  public int applyFeedback(Integer resumeId, String feedbackJson){
    return resumeDao.updateResumeFeedback(resumeId, feedbackJson);
  }
}
