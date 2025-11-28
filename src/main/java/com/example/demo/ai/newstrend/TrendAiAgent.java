package com.example.demo.ai.newstrend;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.dto.Member;
import com.example.demo.newstrend.dao.TrendInsightDao;
import com.example.demo.newstrend.dto.request.TrendAnalyzeRequest;
import com.example.demo.newstrend.dto.response.TrendAnalyzeResponse;
import com.example.demo.newstrend.entity.TrendInsight;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TrendAiAgent {
  private ChatClient chatClient;
  private TrendInsightDao trendInsightDao;
  private MemberDao memberDao;
  private WebClient webClient;
  @Autowired
  private ObjectMapper mapper;

  public TrendAiAgent(ChatClient.Builder chatClientBuilder, TrendInsightDao trendInsightDao, MemberDao memberDao,
      WebClient.Builder webClientBuilder) {
    this.chatClient = chatClientBuilder.build();
    this.trendInsightDao = trendInsightDao;
    this.memberDao = memberDao;
    this.webClient = webClientBuilder
        .defaultHeader("Accept", "application/json")
        .build();
  }

  @Value("${naver.api.client-id}")
  private String naverClientId;

  @Value("${naver.api.client-secret}")
  private String naverClientSecret;

  @Value("${naver.datalab.trend.url}")
  private String naverTrendUrl;

  @Value("${google.search.apiKey}")
  private String googleApiKey;

  @Value("${google.search.engineId}")
  private String googleEngineId;

  @Tool(description = "네이버 데이터랩 API에서 트렌드 데이터 가져오기")
  public Map<String, Object> getTrendData(String keyword, String startDate, String endDate) {
    try {
      Map<String, Object> body = Map.of(
          "startDate", startDate,
          "endDate", endDate,
          "timeUnit", "date",
          "keywordGroups", List.of(Map.of(
              "groupName", "트렌드",
              "keywords", List.of(keyword))));

      String response = webClient.post()
          .uri(naverTrendUrl)
          .header("X-Naver-Client-Id", naverClientId)
          .header("X-Naver-Client-Secret", naverClientSecret)
          .bodyValue(body)
          .retrieve()
          .bodyToMono(String.class)
          .block();

      return mapper.readValue(response, Map.class);
    } catch (Exception e) {
      return Map.of("error", "네이버 데이터랩 실패" + e.getMessage());
    }
  }

  @Tool(description = "연관 키워드 탐색 검색 API 호출")
  public List<String> searchKeyword(String query) {
    try {
      String response = webClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("https://www.googleapis.com/customsearch/v1")
              .queryParam("key", googleApiKey)
              .queryParam("cs", googleEngineId)
              .queryParam("q", query)
              .queryParam("num", 5)
              .build())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      Map<String, Object> result = mapper.readValue(response, Map.class);
      List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
      if (items == null) {
        return List.of();
      }

      return items.stream()
          .map(item -> (String) item.getOrDefault("title", ""))
          .filter(t -> !t.isBlank())
          .toList();
    } catch (Exception e) {
      return List.of("구글 검색 실패: " + e.getMessage());
    }
  }

  public TrendAnalyzeResponse runFullTrendAnalysis(TrendAnalyzeRequest request) throws Exception {
    // 사용자 희망 직무 조회
    Member member = memberDao.findById(request.getMemberId());
    String targetRole = member.getJobRole();

    // 자동 기간 설정(최근 30일)
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusDays(7);

    // 프롬프트
    String systemprompt = """
                너는 채용 및 기술 시장 트렌드 분석 전문가이다.

        아래의 도구를 필요할 때 자유롭게 호출하여 실제 데이터를 수집한다:
        - getTrendData(keyword, startDate, endDate)
        - searchKeyword(query)

        출력 규칙(절대 어기면 안 된다):
        1) 출력은 반드시 JSON ONLY. 불필요한 텍스트 금지.
        2) 절대 null 생성 금지. 모든 필드는 유효한 값으로 채운다.
        3) 모든 문자열 필드는 빈 문자열 ""이 가능하나 null은 불가.
        4) 배열(List)은 최소 1개 이상의 요소를 가진다.
        5) trendJson.keywords는 상위 키워드 목록이다. rawTrendData 내부 keywords와 혼동 금지.
        6) summarycard는 반드시 객체 형태로 작성하며 null 불가.
        7) 숫자 값(avgInterest, marketActivity 등)은 반드시 숫자(Number)로 반환한다.
        8) JSON 구조는 반드시 아래 형식과 100% 동일해야 한다:

        {
          "trendJson": {
            "keywords": ["백엔드", "AI"],             // 문자열 배열 (null 절대 금지)
            "counts": [10, 20, 30],                 // 숫자 배열
            "rawTrendData": {                       // 네이버 데이터랩 전체 응답 JSON 복사 가능
              "data": [
                { "period": "2025-11-20", "ratio": 73.5 }
              ],
              "title": "트렌드",
              "keywords": ["백엔드"]
            }
          },
          "insightJson": {
            "summarycard": {                        // null 절대 금지
              "majorKeyword": "백엔드",
              "avgInterest": 75.3,
              "keywordCount": 3,
              "marketActivity": 82
            },
            "keywordTrend": ["백엔드", "AI"],        // 문자열 배열
            "industrySentiment": [
              { "industry": "IT", "positive": 10, "neutral": 5, "negative": 3 }
            ],
            "marketInsight": [
              "백엔드 분야는 11월 26일에 최고 관심도를 기록했다."
            ],
            "finalSummary": "백엔드 분야는 전반적으로 높은 관심을 유지하고 있다."
          }
        }

        추가 규칙:
        - trendJson.keywords가 비지 않게 설정한다 (예: 사용자의 희망 직무 기반 1개 이상)
        - summarycard.majorKeyword는 trendJson.keywords 중 첫 번째 값으로 설정해도 된다.
        - industrySentiment는 최소 1개 이상의 산업 정보를 포함한다.

        이제 targetRole(희망 직무)와 최근 30일 트렌드를 기반으로 위 JSON만 출력하라.

                """;

    // .user는 String만 받아 Map을 JSON 문자열로 변경
    String userPayload = mapper.writeValueAsString(
        Map.of("targetRole", targetRole, "startDate", startDate.toString(), "endDate", endDate.toString()));

    String llmoutput = chatClient.prompt()
        .system(systemprompt)
        .user(userPayload)
        .tools(this)
        .call()
        .content();

    // JSON -> DTO 변환
    TrendAnalyzeResponse response = mapper.readValue(llmoutput, TrendAnalyzeResponse.class);

    // DTO -> JSON 문자열
    String trendJson = mapper.writeValueAsString(response.getTrendJson());
    String insightJson = mapper.writeValueAsString(response.getInsightJson());

    // DB저장
    TrendInsight trend = new TrendInsight();
    trend.setMemberId(member.getMemberId());
    trend.setStartDate(startDate);
    trend.setEndDate(endDate);
    trend.setTrendJson(trendJson);
    trend.setInsightJson(insightJson);
    trendInsightDao.insertTrendInsight(trend);

    return response;

  }
}
