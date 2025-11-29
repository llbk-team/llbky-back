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

  public CompanySearchAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public CompanySearchAgent(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://openapi.naver.com/v1/search")
                .build();
    }

  public List<String> searchCompanyNames(String query) {
    String response = webClient.get()
      .uri(uriBuilder -> uriBuilder
        .path("/webkr.json")
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
        당신은 기업명 필터링 전문가입니다.
        아래 제목 리스트에서 '기업명처럼 보이는 단어'만 뽑아서 JSON 배열로 반환하세요.

        반드시 기업명만 남기고 다른 단어는 제외하세요.
        예시: ["네이버", "카카오", "삼성전자"]
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
