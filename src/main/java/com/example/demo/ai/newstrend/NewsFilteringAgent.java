package com.example.demo.ai.newstrend;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

//뉴스 연관성 판단

@Component
@Slf4j
public class NewsFilteringAgent {
  private ChatClient chatClient;

  public NewsFilteringAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public boolean isRelevant(String title, String content, List<String> keywords) {
    String keywordStr = String.join(", ", keywords);

    String systemPrompt = """
        당신은 취업·기술 뉴스 관련성 판단 전문가입니다.
            주어진 뉴스가 사용자의 검색 키워드와 실제로 관련이 있는지 판단합니다.

            판단 기준:
            1. 키워드가 제목이나 본문에 의미적으로 연관되어 있는가?
            2. 취업, 채용 분야와 관련이 있는가?
            3. 단순히 키워드 일부만 포함되어 있는 것은 관련 없음으로 판단

            응답 형식: "true" 또는 "false"만 출력 (다른 텍스트 금지)
        """;

    String userPrompt = """
        검색 키워드: %s

        뉴스 제목: %s
        뉴스 내용: %s

        이 뉴스가 검색 키워드와 관련이 있습니까?
        """.formatted(keywordStr, title, content);

    String response = chatClient.prompt()
        .system(systemPrompt)
        .user(userPrompt)
        .options(ChatOptions.builder()
            .temperature(0.1)
            .maxTokens(10)
            .build())
        .call()
        .content()
        .trim()
        .toLowerCase();

    boolean isRelevant = "true".equals(response);

    log.debug("뉴스 관련성 판단 완료 - 제목: {}, 결과: {}", title, isRelevant);

    return isRelevant;
  }

}
