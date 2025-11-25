package com.example.demo.resume.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ai.resume.FeedbackResumeAgent;
import com.example.demo.ai.resume.ResumeCoachAgent;
import com.example.demo.resume.dao.ResumeDao;
import com.example.demo.resume.dto.request.ResumeCoachRequest;
import com.example.demo.resume.dto.response.ResumeCoachResponse;
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
  private FeedbackResumeAgent resumeAiAgent;
  @Autowired
  private ResumeCoachAgent resumeCoachAgent;
  @Autowired
  private ObjectMapper mapper;

  // 이력서 작성
  @Transactional
  public int createResume(Resume resume) throws Exception {
    // 이력서 저장
    resumeDao.insertResume(resume);

    // pk 가져오기
    int newId = resume.getResumeId();

    // AI 자동 분석 실행
    resumeAiAgent.analyze(resume.getMemberId(), newId);

    return resume.getResumeId();
  }

  // 이력서 상세 조회
  public Resume getResume(int resumeId) {
    return resumeDao.selectResumeById(resumeId);
  }

  // 이력서 목록 조회
  public List<Resume> getResumeList(int memberId) {
    return resumeDao.selectResumesByMemberId(memberId);
  }

  // 이력서 수정
  public int updateResume(Resume resume){
    return resumeDao.updateResume(resume);
  }

  // 이력서 삭제
  public int deleteResume(int resumeId) {
    return resumeDao.deleteResume(resumeId);
  }

  // AI 분석
  public ResumeReportResponse analyzeResume(int memberId, int resumeId) throws Exception {
    ResumeReportResponse aiResult = resumeAiAgent.analyze(memberId, resumeId);
    log.info("AI 분석 결과: {}", aiResult);
    return aiResult;
  }

  // AI 분석 조회
  public ResumeReportResponse getResumeReport(int resumeId) throws Exception {
    Resume resume = resumeDao.selectResumeById(resumeId);

    if (resume == null) {
      throw new RuntimeException("이력서를 찾을 수 없습니다.");
    }
    return mapper.readValue(resume.getResumeFeedback(), ResumeReportResponse.class);
  }

  // 실시간 코칭
  public ResumeCoachResponse coachResponse(ResumeCoachRequest request) throws Exception {
    return resumeCoachAgent.coach(request);
  }

  // // AI 피드백 반영(경력)
  // @Transactional
  // public int applyCareerRewrite(int resumeId) throws Exception {
  // Resume resume = resumeDao.selectResumeById(resumeId);
  // if (resume == null) {
  // throw new RuntimeException("이력서를 찾을 수 없습니다.");
  // }

  // // AI 피드백 JSON -> ResumeReportResponse로 변환
  // ResumeReportResponse feedback = mapper.readValue(resume.getResumeFeedback(),
  // ResumeReportResponse.class);

  // // 해당 제안 가져오기
  // List<RewriteSuggestion> suggestions = feedback.getRewriteSuggestions();
  // if (suggestions == null || suggestions.isEmpty()) return 0;

  // // JSON 문자열 -> List<Map> 변환
  // List<Map<String, Object>> careerList =
  // mapper.readValue(resume.getCareerInfo(), new TypeReference<>() {});

  // int applyCount = 0;

  // for(RewriteSuggestion s : suggestions){
  // String beforeNorm = normalize(s.getBefore());
  // log.info("=== Checking Suggestion ===");

  // for(Map<String, Object> item : careerList){
  // Object descObj = item.get("description");
  // if(descObj == null) continue; // description 없으면 스킵

  // String descNorm = normalize(descObj.toString());
  // if (descNorm.equals(beforeNorm)){
  // // 반영 내용으로 교체
  // item.put("description", s.getAfter());
  // applyCount++;
  // break; // 다음 제안으로 넘어감
  // }
  // }
  // }

  // // JSON 문자열로 변환
  // String updateJson = mapper.writeValueAsString(careerList);
  // resumeDao.updateCareerinfo(resumeId, updateJson);

  // return applyCount;
  // }

  // private String normalize(String text){
  // return text == null? "": text.replace("\r\n","\n").trim();
  // }
}
