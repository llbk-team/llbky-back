package com.example.demo.ai.newstrend;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.example.demo.newstrend.dao.NewsSummaryDao;
import com.example.demo.newstrend.dto.response.SentimentResponse;
import com.example.demo.newstrend.dto.response.TrendDataContext;
import com.example.demo.newstrend.entity.NewsSummary;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/*
  LLM이 직무 기반 키워드 확장
  네이버API(데이터랩) 도구 호출
  LLM이 반환한 JSON -> TrendDataContext 변환

  LLM이 직무 기반으로 추출한 키워드와 트렌드 api 데이터를 저장하는 에이전트
*/
@Component
@Slf4j
public class TrendDataAgent {
  private ChatClient chatClient;
  private WebClient webClient;
  @Autowired
  private MemberDao memberDao;

  @Autowired
  private SentimetalAnalysisAgent sentimetalAnalysisAgent;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private NewsSummaryDao newsSummaryDao;

  public TrendDataAgent(ChatClient.Builder chatClientBuilder, WebClient.Builder webClientBuilder) {
    this.chatClient = chatClientBuilder.build();
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

  @Tool(description = "네이버 데이터랩에서 특정 키워드의 검색량(기간별 ratio)을 가져옵니다.")
  public Map<String, Object> getTrendData(String keyword, String startDate, String endDate) {
    log.info("[TOOL CALLED] getTrendData(keyword={}, start={}, end={})",
        keyword, startDate, endDate);
    try {
      // 요청 바디 생성(네이버 DataLab 규격)
      Map<String, Object> requestbody = Map.of(
          "startDate", startDate,
          "endDate", endDate,
          "timeUnit", "date", // 일간 단위
          "keywordGroups", List.of(Map.of(
              "groupName", "트렌드", // 주제어이며 검색어 묶음을 대표하는 이름
              "keywords", List.of(keyword)))); // 주제어에 해당하는 검색어

      String response = webClient.post()
          .uri(naverTrendUrl)
          .header("X-Naver-Client-Id", naverClientId)
          .header("X-Naver-Client-Secret", naverClientSecret)
          .bodyValue(requestbody) // 요청 바디 JSON으로 직렬화해서 전송
          .retrieve()
          .bodyToMono(String.class)
          .block();

      log.info("[API SUCCESS] 네이버 검색량 수집 완료 keyword={}", keyword);

      return mapper.readValue(response, Map.class);
    } catch (Exception e) {
      log.error("❌ [TOOL ERROR] getTrendData 실패: keyword={}, msg={}",
          keyword, e.getMessage());
      return Map.of("error", "API 호출 실패: " + e.getMessage());
    }
  }

  public TrendDataContext collect(Integer memberId) throws Exception {
    log.info("[TrendDataAgent] 데이터 수집 시작 memberId={}", memberId);
    // 사용자 희망 직무 조회
    Member member = memberDao.findById(memberId);
    String jobGroup = member.getJobGroup();
    String targetRole = member.getJobRole();

    // 수집 날짜 범위(7일)
    LocalDate end = LocalDate.now();
    LocalDate start = end.minusDays(7);

    String startDate = start.toString();
    String endDate = end.toString();

    // 1) 뉴스 50개 조회
    List<NewsSummary> newsList = newsSummaryDao.selectLatestNewsByMemberId(memberId, 50);

    // 2) 키워드 후보 풀 생성
    List<String> keywordPool = new ArrayList<>();

    for (NewsSummary n : newsList) {
      if (n.getKeywordsJson() == null)
        continue;

      // JSON 파싱
      List<Map<String, String>> list = mapper.readValue(n.getKeywordsJson(), List.class);

      for (Map<String, String> k : list) {
        keywordPool.add(k.get("keyword"));
      }
    }

    Map<String, Integer> freq = new HashMap<>();

    for (String k : keywordPool) {
      freq.put(k, freq.getOrDefault(k, 0) + 1);
    }

    StringBuilder keywordStats = new StringBuilder();

    // 키워드 빈도 리스트 문자열 (ex -AI: 11회) LLM에게 계산 줄이게 하기 위해
    freq.entrySet().stream()
        .sorted((a, b) -> b.getValue() - a.getValue())
        .forEach(e -> {
          keywordStats.append("- ")
              .append(e.getKey())
              .append(": ")
              .append(e.getValue())
              .append("회\n");
        });

    SentimentResponse metaNews = sentimetalAnalysisAgent.excute(memberId, 50);

    String systemPrompt = """
          너는 검색 트렌드 수집을 위한 데이터 수집 에이전트이다.
          너는 계산, 분석, 요약을 하지 않는다.
          (계산과 해석은 TrendAnalysisAgent가 수행한다)

          ────────────────────────────────────────────
          ⚠️ 날짜 필드 고정 규칙 (절대 위반 금지)
          ────────────────────────────────────────────
          아래 startDate, endDate 값은 LLM이 생성하는 값이 아니다.
          LLM은 이 값을 절대로 수정, 삭제, 변환, 요약, 재생성, null 로 변경할 수 없다.
          반드시 출력 JSON에 아래 값 그대로 넣어라:

          "startDate": "%s"
          "endDate": "%s"

          이 두 필드가 변경되면 즉시 실패다.

          ────────────────────────────────────────────
          ⚠️ 키워드 선정 및 확장 규칙 (핵심)
          ────────────────────────────────────────────
          - 키워드는 뉴스에서 추출된 keywordPool을 기준으로 선정한다.
          - 단, keywordPool에 포함된 키워드와
            의미적으로 직접 연결된 개념 키워드는 제한적으로 허용한다.
          - 키워드는 무조건 10개를 선정한다.

          - 개념 확장은 다음을 모두 허용한다:
            1) 상위 개념 (예: LLM → AI)
            2) 하위 개념 (예: Cloud → AWS, Azure)
            3) 동의·대표 개념

          - 단, 확장된 키워드는 반드시 다음 조건을 만족해야 한다:
            a) keywordPool의 키워드들과 명확한 의미적 연결 관계가 있을 것
            b) 뉴스 및 targetRole 맥락에서 설명 가능할 것
            c) 키워드 수를 채우기 위한 임의 확장이 아닐 것

          - 뉴스·keywordPool·직무와 무관한 키워드는 생성해서는 안 된다.

          ────────────────────────────────────────────
          ⚠️ 검색 데이터 결합 규칙
          ────────────────────────────────────────────
          - 트렌드 키워드 후보에 대해
            getTrendData를 순차적으로 호출한다.
          - 호출 성공 여부는 사전에 알 수 없으며,
            호출 결과를 통해서만 판단한다.
          - 검색 데이터가 없는 키워드는
            그래프용 키워드에서는 제외하고,
          - 키워드의 검색 트렌드 데이터 존재 여부는
            getTrendData 도구 호출 결과를 통해서만 판단할 수 있다.
          - getTrendData 호출에 실패하거나
          - getTrendData 결과의 "results" 배열이 비어 있는 경우,
            해당 키워드는 검색 데이터가 없는 것으로 간주하며
            트렌드 키워드에서 제외한다.

          ────────────────────────────────────────────
          ⚠️ metaNews 사용 규칙
          ────────────────────────────────────────────
          - metaNews는 키워드 생성을 직접 유도하는 용도가 아니다.
          - metaNews는 시장 분위기와 키워드 중요도를
            보정·설명하는 참고 정보로만 사용한다.

          ────────────────────────────────────────────
          ⚠️ TrendDataContext 출력 규칙 (구조 절대 유지)
          ────────────────────────────────────────────
          - JSON ONLY 반환
          - '{' 로 시작하고 '}' 로 끝나야 한다
          - null 절대 금지
          - 코드블록, 설명문, 서론 금지
          - JSON 외 어떤 텍스트도 출력하지 말 것

          반드시 아래 구조 그대로 출력한다:

          {
            "memberId": number,
            "jobGroup": string,
            "targetRole": string,
            "startDate": "YYYY-MM-DD",
            "endDate": "YYYY-MM-DD",
            "keywords": [string],
            "rawTrendData": {
              "<keyword>": { getTrendData 결과 원본 }
            },
            "metaNews": "string",
            "keywordFrequency": {"string": number}
          }

        - 트렌드 키워드 후보에 대해
          getTrendData를 순차적으로 호출한다.
        - 호출 실패 또는 유효 데이터 없음이 발생한 키워드는
          제외하고 다음 키워드를 시도한다.
        - 최종적으로 유효한 트렌드 데이터가 존재하는
          키워드를 기준으로 rawTrendData를 구성한다.
          """;

    String userPrompt = """
        아래 값들은 그대로 JSON에 넣어라. 절대 수정 금지.

        memberId: %d
        jobGroup: %s
        targetRole: %s
        startDate: %s
        endDate: %s

        [뉴스 기반 키워드 후보 목록]
        %s

        [뉴스 기반 키워드 빈도]
        %s

        [시장 분위기 참고용]
        metaNews: %s

        TrendDataContext JSON을 생성하라.
        """.formatted(
        memberId,
        jobGroup,
        targetRole,
        startDate,
        endDate,
        keywordPool.toString(),
        keywordStats.toString(),
        metaNews);

    String llmResult = chatClient.prompt()
        .system(systemPrompt)
        .user(userPrompt)
        .tools(this)
        .call()
        .content();

    log.info(" [LLM RAW OUTPUT] {}", llmResult);

    // JSON -> TrendDataContext(DTO) 변환
    return mapper.readValue(llmResult, TrendDataContext.class);
  }
}
