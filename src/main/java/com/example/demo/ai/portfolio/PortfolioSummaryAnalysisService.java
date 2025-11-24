package com.example.demo.ai.portfolio;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.dto.Member;
import com.example.demo.portfolio.dao.PortfolioDao;
import com.example.demo.portfolio.dao.PortfolioImageDao;
import com.example.demo.portfolio.entity.PortfolioImage;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PortfolioSummaryAnalysisService {

  // 필드
  private ChatClient chatClient;

  @Autowired
  private PortfolioDao portfolioDao;

  @Autowired
  private PortfolioImageDao portfolioImageDao;

  @Autowired
  private MemberDao memberDao;

  // 생성자
  public PortfolioSummaryAnalysisService(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public String generateSummary(Integer portfolioId, Integer memberId) {

    // 사용자 직군 직무 조회
    Member member = memberDao.findById(memberId);
    String jobGroup = member.getJobGroup();
    String jobRole = member.getJobRole();

    // 페이지별 피드백 전체 조회
    List<PortfolioImage> pageFeedbackList = portfolioImageDao.selectImagesByPortfolioId(portfolioId);

    // 시스템 메시지
    SystemMessage systemMessage = SystemMessage.builder()
        .text("""
            너는 포트폴리오 전체를 종합 분석하는 AI다.

            규칙
            - 출력은 반드시 JSON만.
            - 다른 텍스트 절대 금지.
            - 직군/직무에 맞는 전문적인 평가 작성.
            - 페이지별 피드백들을 분석해 전체 종합 점수를 도출.

            직군: %s
            역할: %s""".formatted(jobGroup, jobRole))
        .build();

    // 사용자 메시지
    UserMessage userMessage = UserMessage.builder()
        .text("""
            다음은 포트폴리오 각 페이지의 분석 결과다.
            이를 종합해 하나의 최종 분석 JSON을 생성해라.

            페이지 피드백 리스트: %s

            출력 JSON 구조:
            {
              "final_score": 93,
              "strengths": ["강점1", "강점2", ...],
              "weaknesses": ["약점1", "약점2", ...],
              "visual_design": "전체 디자인 평가",
              "information_structure": "정보 구조 평가",
              "technical_composition": "기술 구성 평가",
              "content_quality": "콘텐츠 명료성 평가",
              "expression": "표현력/스토리텔링 평가",
              "overall_review": "전체 종합 의견 (3~5문장)"
            }
            """.formatted(pageFeedbackList.toString()))
        .build();

    // LLM 호출
    String summary = chatClient.prompt()
        .messages(systemMessage, userMessage)
        .call()
        .content();

    log.info("최종 포트폴리오 분석 결과: {}", summary);
    portfolioDao.updatePortfolioFeedback(portfolioId, summary);

    return summary;
  }

}
