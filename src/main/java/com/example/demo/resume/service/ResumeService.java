package com.example.demo.resume.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ai.resume.FeedbackResumeAgent;
import com.example.demo.resume.dao.ResumeDao;
import com.example.demo.resume.dto.response.ResumeReportResponse;
import com.example.demo.resume.dto.response.RewriteSuggestion;
import com.example.demo.resume.entity.Resume;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ResumeService {
  @Autowired
  private ResumeDao resumeDao;
  @Autowired
  private FeedbackResumeAgent resumeAiAgent;
  @Autowired
  private ObjectMapper mapper;

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

  // AI 피드백 반영(경력)
  @Transactional
  public int applyCareerRewrite(int resumeId, int index) throws Exception {
    Resume resume = resumeDao.selectResumeById(resumeId);
    if (resume == null) {
      throw new RuntimeException("이력서를 찾을 수 없습니다.");
    }

    // AI 피드백 JSON -> ResumeReportResponse로 변환
    ResumeReportResponse feedback = mapper.readValue(resume.getResumeFeedback(), ResumeReportResponse.class);

    // 해당 제안 가져오기
    RewriteSuggestion suggestion = feedback.getRewriteSuggestions().get(index);

    String before = suggestion.getBefore();
    String after = suggestion.getAfter();

    // JSON 문자열 -> Java 객체 변환
    List<Map<String, Object>> careerList = mapper.readValue(resume.getCareerInfo(), new TypeReference<>() {});

    for (Map<String, Object> item : careerList) {
      if (before.equals(item.get("description"))) {
        item.put("description", after);
      }
    }
    // JSON 문자열로 변환
    String updateJson = mapper.writeValueAsString(careerList);

    return resumeDao.updateCareerinfo(resumeId, updateJson);
  }
}
