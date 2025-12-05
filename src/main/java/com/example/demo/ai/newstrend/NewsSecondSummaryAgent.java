package com.example.demo.ai.newstrend;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.newstrend.dao.NewsSummaryDao;
import com.example.demo.newstrend.dto.response.NewsSecondSummaryResponse;
import com.example.demo.newstrend.entity.NewsSummary;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class NewsSecondSummaryAgent {
  private ChatClient chatClient;
  @Autowired
  private ObjectMapper mapper;
  @Autowired
  private NewsSummaryDao newsSummaryDao;

  public NewsSecondSummaryAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public String summarizeNews(int memberId, int limit) throws Exception {
    // 1) DB에서 최신 뉴스 10개 조회
    List<NewsSummary> newsList = newsSummaryDao.selectLatestNewsByMemberId(memberId, limit);

    // 뉴스 긴 요약문만 10개 추출
    List<String> details = new ArrayList<>();

    for (NewsSummary n : newsList) {
      String s = n.getDetailSummary();
      if (s != null && !s.isBlank()) {
        details.add(s);
      }
      if (details.size() == 10) {
        break;
      }
    }

    log.info("📝 [추출된 detailSummary 개수] = {}", details.size());
    log.debug("📄 [detailSummary 목록] = {}", details);

    String systemPrompt = """
        당신은 여러 뉴스의 공통 흐름을 짧게 묶어주는 2차 요약기입니다.
        JSON ONLY로 아래 형식으로 출력하세요.

        {
          "metaSummary": "..."
        }
        """;
    String userPrompt = """
        [뉴스 요약 리스트]
        %s
        """.formatted(details.toString());

    String json = chatClient.prompt()
        .system(systemPrompt)
        .user(userPrompt)
        .call()
        .content();

    log.info("📦 [LLM RAW OUTPUT] {}", json);

    // String -> DTO 변환
    NewsSecondSummaryResponse dto = mapper.readValue(json, NewsSecondSummaryResponse.class);
    log.info("✨ [최종 metaSummary 생성 완료]");
    log.debug("metaSummary = {}", dto.getMetaSummary());

    return dto.getMetaSummary();
  }
}
