package com.example.demo.ai.interview;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CompanySearchAgent {

  @Value("${naver.api.client-id}")
  private String clientId;

  @Value("${naver.api.client-secret}")
  private String clientSecret;

  private ChatClient chatClient;
  private WebClient webClient;

  public CompanySearchAgent(ChatClient.Builder chatClientBuilder,
                            WebClient.Builder webClientBuilder
  ) {
    this.chatClient = chatClientBuilder.build();
    this.webClient = webClientBuilder
                .baseUrl("https://openapi.naver.com")
                .build();
  }
  
  public List<String> searchCompanyNames(String query) {

    if (query == null || query.isBlank()) {
        return new ArrayList<>();
    }

    String response = webClient.get()
      .uri(uriBuilder -> uriBuilder
        .path("/v1/search/webkr.json")
        .queryParam("query", query)
        .queryParam("display", 10)
        .queryParam("sort", "date")
        .build()
      )
      .header("X-Naver-Client-Id", clientId)
      .header("X-Naver-Client-Secret", clientSecret)
      .retrieve()
      .bodyToMono(String.class)
      .block();

    if (response == null) {
      return new ArrayList<>();
    }

    JSONArray items = new JSONObject(response).getJSONArray("items");
    List<String> titles = new ArrayList<>();

    for (int i = 0; i < items.length(); i++) {
      String title = items.getJSONObject(i).getString("title");
      titles.add(title.replaceAll("<[^>]*>", "")); // HTML 제거
    }

    String system = """
        당신은 JSON만 출력하는 시스템입니다.

아래 리스트를 기반으로 '기업명처럼 보이는 단어'만 추출하여
반드시 JSON 배열 형식 ONLY로 출력하세요.

절대 문장, 설명, 객체, 키:값 구조를 포함하지 마세요.
절대 따옴표가 빠진 문자열을 넣지 마세요.

반드시 이런 형식이어야 함:
["네이버", "카카오", "삼성전자"]
        """;
    
    String prompt = String.join("\n", titles);

    String aiResult = chatClient.prompt()
      .system(system)
      .user(prompt)
      .call()
      .content();

    // JSON 배열을 List로 변환
    List<String> result = new ArrayList<>();
    JSONArray arr = new JSONArray(aiResult);
    for (int i = 0; i < arr.length(); i++) {
      result.add(arr.getString(i));
    }

    return result;

  }

}
