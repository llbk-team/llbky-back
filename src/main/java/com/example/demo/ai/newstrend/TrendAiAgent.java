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

/*
  TrendAiAgent: 트렌드 분석을 수행하기 위한 "AI Agent"
  - 네이버 데이터랩 API 호출(실제 검색 트렌드 데이터 수집)
  - 구글 연관 검색어 API 호출
  - LLM에게 트렌드 분석 프롬프트 전달
  - LLM이 출력한 JSON을 DTO로 파싱
  - 결과를 DB에 저장
*/
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
    this.webClient = webClientBuilder // 외부 API 호출용
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

  /*
    LLM이 필요할 때 호출하는 도구
    특정 키워드의 검색량을 반환
  */
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

  /*
    사용자의 희망 직무 기반으로 연관 키워드를 자동 확장하기 위해 사용
  */
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

      // 검색 결과에서 제목만 추출
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

    // 분석 날짜 범위 설정
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusDays(7);

    // 프롬프트
    String systemprompt = """
      너는 채용 및 기술 시장 트렌드를 분석하는 전문가이다.
      출력은 반드시 JSON 형식만 생성해야 하며, JSON 외의 텍스트는 절대 포함해서는 안 된다.
      출력은 반드시 '{' 로 시작하고 '}' 로 끝나야 한다.

      사용 가능한 도구:
      - getTrendData(keyword, startDate, endDate)
      - searchKeyword(query)

      ========================================
      출력 구조 규칙
      ========================================
      반드시 아래 DTO 구조와 일치해야 한다:

      TrendAnalyzeResponse
        trendJson: TrendGraph
            keywords: List<String>
            counts: List<Integer>
            rawTrendData: Map<String, Object>

        insightJson: InsightJson
            summarycard: SummaryCard
                majorKeyword: String
                avgInterest: Number
                interestChange: String
                keywordCount: Number
                marketActivity: Number

            keywordTrend: List<String>
            industrySentiment: List<IndustrySentiment>
                industry: String
                positive: Number
                neutral: Number
                negative: Number
            wordCloud: List<WordCloudItem>
                keyword: String
                score: Number
            marketInsight: List<String>
            finalSummary: String

      null 값은 절대 생성하지 않는다.
      모든 리스트는 최소 1개 이상 생성한다.


      ========================================
      키워드 확장 규칙 (TrendGraph.keywords 용)
      ========================================
      targetRole(사용자 희망 직무)을 기반으로 최소 7~10개의 키워드를 생성한다.

      키워드 생성 규칙:
      - targetRole 자체 포함
      - 해당 직무에서 주로 사용되는 기술, 언어, 프레임워크 포함
      - searchKeyword(query) 도구 호출로 연관 키워드 확보
      - 필요하면 내부 지식 기반 확장

      예시:
      백엔드 → ["백엔드", "Java", "Spring", "JPA", "MySQL", "AWS", "Docker", "Kubernetes"]

      trendJson.keywords 배열 = 확장된 키워드 전체


      ========================================
      트렌드 그래프 데이터 생성 규칙 (TrendGraph)
      ========================================
      각 키워드에 대해 getTrendData(keyword) 호출해 rawTrendData.ratio 배열과 기간 데이터를 얻는다.

      counts 계산 규칙:
      counts[i] = 해당 키워드의 평균 ratio (정수로 반올림)

      rawTrendData:
      네이버 데이터랩 API 응답 전체 JSON 그대로 넣는다.


      ========================================
      SummaryCard 규칙
      ========================================

      majorKeyword:
      trendJson.keywords[0]

      avgInterest:
      rawTrendData.data[*].ratio 평균값

      interestChange:
      ((마지막 ratio - 첫 ratio) / 첫 ratio) * 100
      양수면 "+4.1%" 형식
      음수면 "-3.2%" 형식

      keywordCount:
      trendJson.keywords 배열 길이

      marketActivity:
      시장 활기 =
      (avgInterest * 0.5)
      + (interestChange 양수면 +20, 음수면 -10)
      + (keywordTrend 중 ▲ 키워드 비율 × 10)
      최종값을 0~100 사이로 조정


      ========================================
      keywordTrend 생성 규칙
      ========================================
      각 키워드에 대해 변화율을 계산하되 문자열 리스트로 출력한다.

      형식:
      "키워드 ▲32%" 또는 "키워드 ▼12%"

      계산:
      change = ((last - first) / first) * 100
      양수면 ▲, 음수면 ▼


      ========================================
      산업군 자동 선택 규칙 (industrySentiment)
      ========================================
      targetRole(사용자 희망 직무)을 기반으로 다음 기준으로 산업군을 선택하거나 생성한다:

      1) 아래 제공된 매핑은 ‘예시’이며, LLM은 이를 참고하여
        targetRole 과 가장 연관성이 높은 산업군들을 자유롭게 생성할 수 있다.

      예시 매핑:
      - 백엔드, 프론트엔드, 풀스택 → IT/소프트웨어, 게임/엔터테인먼트, 핀테크, 이커머스
      - AI, 머신러닝, 데이터 → IT/AI, 클라우드, 헬스케어, 제조 자동화, 로보틱스
      - 모바일 개발자 → IT/소프트웨어, 핀테크, 스타트업 서비스, 게임
      - UX/UI 디자이너 → 웹서비스, 게임, 이커머스, 교육/콘텐츠, 헬스케어 UX

      2) targetRole 이 위 매핑에 없어도 LLM은 자유롭게 산업군을 생성한다.
        예를 들어:
        - DevOps → 클라우드, 인프라, 보안, 플랫폼 엔지니어링
        - 사이버보안 → 보안, 금융, 공공기관, 클라우드 보안
        - 데이터 엔지니어 → 데이터 플랫폼, 빅데이터, 클라우드, 제조 분석

      3) 산업군 개수는 최소 3개 이상 생성한다.

      4) 각 산업군은 다음 구조로 생성한다:
        industry: 산업명
        positive, neutral, negative 값의 합 = 100


      ========================================
      워드클라우드 생성 규칙
      ========================================
      wordCloud[*].keyword = trendJson.keywords[*] 또는 관련 기술 키워드
      wordCloud[*].score = 20~100
      점수는 트렌드 변화율, 중요도, 검색 비중 등을 반영해 상대적으로 배정


      ========================================
      marketInsight / finalSummary 규칙
      ========================================

      marketInsight:
      - keywordTrend 변화 데이터를 바탕으로 2~4개 문장 생성

      finalSummary:
      - 전체 시장 흐름을 1~2문장으로 요약


      ========================================
      중요: 출력 제한 규칙
      ========================================
      반드시 JSON ONLY 로 출력하라.
      JSON 외 텍스트 금지.
      설명, 인사말, 결론 문장 금지.
      JSON 앞뒤에 공백, 줄바꿈, 문자 넣지 말 것.
      반드시 '{' 로 시작하고 '}' 로 끝나야 한다.

      ========================================

      이제 targetRole, startDate, endDate 를 기반으로
      위 규칙을 모두 적용한 JSON을 생성하라.
      """;

    // .user는 String만 받아 Map을 JSON 문자열로 변경하여 희망 직무와 기간 전달
    String userPayload = mapper.writeValueAsString(
        Map.of("targetRole", targetRole, "startDate", startDate.toString(), "endDate", endDate.toString()));

    // LLM 호출
    // 도구를 사용하여 실제 데이터 수집 및 JSON으로 분석 결과 생성
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
