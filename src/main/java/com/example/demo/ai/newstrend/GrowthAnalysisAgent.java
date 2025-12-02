package com.example.demo.ai.newstrend;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.coverletter.dao.CoverLetterDao;
import com.example.demo.coverletter.entity.CoverLetter;
import com.example.demo.interview.dao.InterviewSessionDao;
import com.example.demo.interview.entity.InterviewSession;
import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.dto.Member;
import com.example.demo.newstrend.dao.SavedKeywordDao;
import com.example.demo.newstrend.dao.TrendInsightDao;
import com.example.demo.newstrend.dto.response.InsightJson;
import com.example.demo.newstrend.entity.SavedKeyword;
import com.example.demo.newstrend.entity.TrendInsight;
import com.example.demo.portfolio.dao.PortfolioDao;
import com.example.demo.portfolio.entity.Portfolio;
import com.example.demo.resume.dao.ResumeDao;
import com.example.demo.resume.entity.Resume;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GrowthAnalysisAgent {
  private ChatClient chatClient;
  @Autowired
  private ObjectMapper mapper;
  @Autowired
  private MemberDao memberDao;
  @Autowired
  private SavedKeywordDao savedKeywordDao;
  @Autowired
  private TrendInsightDao trendInsightDao;
  @Autowired
  private ResumeDao resumeDao;
  @Autowired
  private CoverLetterDao coverLetterDao;
  @Autowired
  private InterviewSessionDao interviewSessionDao;
  @Autowired
  private PortfolioDao portfolioDao;

  public GrowthAnalysisAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  /*
   * 성장 제안 에이전트
   */
  public String generateGrowthAdvice(int memberId) throws Exception {
    // 사용자의 직무, 직군 정보 조회
    Member member = memberDao.findById(memberId);
    String jobGroup = member.getJobGroup();
    String jobRole = member.getJobRole();

    // 저장 키워드
    List<SavedKeyword> keywords = savedKeywordDao.selectSavedKeywordListByMemberId(memberId);

    List<String> savedKeywords = new ArrayList<>();
    for (SavedKeyword sk : keywords) {
      savedKeywords.add(sk.getKeyword());
    }

    // 트렌드 분석 데이터
    InsightJson insightJson = null;
    TrendInsight trend = trendInsightDao.selectLatestTrendInsight(memberId);

    if (trend != null && trend.getInsightJson() != null) {
      insightJson = mapper.readValue(trend.getInsightJson(), InsightJson.class);
    }

    // 각 종합 피드백 추출

    List<Resume> resumes = resumeDao.selectResumesByMemberId(memberId);
    List<String> resumeFeedbackRaw = new ArrayList<>();
    for (Resume r : resumes) {
      resumeFeedbackRaw.add(r.getResumeFeedback()); // JSON 그대로
    }

    List<CoverLetter> coverLetters = coverLetterDao.selectAllCoverLetters(memberId);
    List<String> coverFeedbackRaw = new ArrayList<>();
    for (CoverLetter cl : coverLetters) {
      coverFeedbackRaw.add(cl.getCoverFeedback());
    }

    List<InterviewSession> sessions = interviewSessionDao.selectAllInterviewSessions(memberId);
    List<String> interviewReportsRaw = new ArrayList<>();
    for (InterviewSession s : sessions) {
      interviewReportsRaw.add(s.getReportFeedback());
    }

    List<Portfolio> portfolios = portfolioDao.selectPortfoliosByMemberId(memberId);
    List<String> portfolioFeedbackRaw = new ArrayList<>();
    for (Portfolio p : portfolios) {
      portfolioFeedbackRaw.add(p.getPortfolioFeedback());
    }

    // 프롬프트
    String systemPrompt = """
        당신은 AI 기반 커리어 성장 코치입니다.
        주어진 JSON 데이터를 기반으로 이력서, 면접, 학습 성장 제안을 작성하세요.
        - 다양한 기능(이력서, 자소서, 포트폴리오, 면접)에서 생성된 피드백 JSON들을
          세부적으로 해석하지 않고, 전체적인 흐름과 방향성만 파악합니다.
        - 최신 트렌드(insightJson)와 사용자가 저장한 키워드(savedKeywords)를 기반으로,
          "성장 방향성"만 간단 명료하게 제시합니다.
        - 트렌드에서 강조되는 키워드/분위기/성장 방향을 반영한다

        출력 규칙:
        - JSON ONLY 출력
        - 필드명: resumeAdvice, interviewAdvice, learningAdvice
        - 각 항목은 1~2문장

        금지 규칙:
        - 백틱(`) 절대 출력하지 말 것
        - markdown 코드블록( ``` ) 출력 금지
        - JSON 외 다른 텍스트 금지
        """;

    String request = """
        {
          "jobRole": %s,
          "jobGroup": %s,
          "savedKeywords": %s,
          "resume": %s,
          "coverLetter": %s,
          "interview": %s,
          "portfolio": %s,
          "insightJson": %s
        }
        """.formatted(
        mapper.writeValueAsString(member.getJobRole()),
        mapper.writeValueAsString(member.getJobGroup()),
        mapper.writeValueAsString(savedKeywords),
        mapper.writeValueAsString(resumeFeedbackRaw),
        mapper.writeValueAsString(coverFeedbackRaw),
        mapper.writeValueAsString(interviewReportsRaw),
        mapper.writeValueAsString(portfolioFeedbackRaw),
        mapper.writeValueAsString(insightJson));

    String response = chatClient.prompt()
        .system(systemPrompt)
        .user(request)
        .call()
        .content();

    return response;
  }

}
