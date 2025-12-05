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
import com.example.demo.member.entity.Member;
import com.example.demo.newstrend.dto.response.TrendDataContext;
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
  private ObjectMapper mapper;
  @Autowired
  private NewsSecondSummaryAgent newsSecondSummaryAgent;

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
    log.info("🔧 [TOOL CALLED] getTrendData(keyword={}, start={}, end={})",
        keyword, startDate, endDate);
    try {
      Map<String, Object> requestbody = Map.of(
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
          .bodyValue(requestbody)
          .retrieve()
          .bodyToMono(String.class)
          .block();

      log.info("📥 [API SUCCESS] 네이버 검색량 수집 완료 keyword={}", keyword);

      return mapper.readValue(response, Map.class);
    } catch (Exception e) {
      log.error("❌ [TOOL ERROR] getTrendData 실패: keyword={}, msg={}",
          keyword, e.getMessage());
      return Map.of("error", "API 호출 실패: " + e.getMessage());
    }
  }

  public TrendDataContext collect(Integer memberId) throws Exception {
    log.info("🚀 [TrendDataAgent] 데이터 수집 시작 memberId={}", memberId);
    // 사용자 희망 직무 조회
    Member member = memberDao.findById(memberId);
    String targetRole = member.getJobRole();

    // 수집 날짜 범위
    LocalDate end = LocalDate.now();
    LocalDate start = end.minusDays(7);

    String startDate = start.toString();
    String endDate = end.toString();

    // 뉴스 2차 요약 자동 호출
    String metaNews = newsSecondSummaryAgent.summarizeNews(memberId, 10);

    String systemPrompt = """
        너는 검색 트렌드 수집을 위한 데이터 수집 에이전트이다.
        너는 계산, 분석, 요약을 하지 않는다. (계산은 TrendAnalysisAgent가 수행함)

        ⚠️ 날짜 필드 고정 규칙 (절대 위반 금지)
        아래 startDate, endDate 값은 LLM이 생성하는 값이 아니다.
        LLM은 이 값을 절대로 수정, 삭제, 변환, 요약, 재생성, null 로 변경할 수 없다.
        반드시 출력 JSON에 아래 값 그대로 넣어라:

        "startDate": "%s"
        "endDate": "%s"

        이 두 필드가 null, 빈 문자열, 다른 날짜로 바뀌면 즉시 실패다.

        ────────────────────────────────────────────
        ⚠️ 키워드 생성 규칙 (직무 무관/엉뚱한 키워드 절대 금지)
        ────────────────────────────────────────────
        1) targetRole 기반 “기술 키워드 10개”만 생성한다.
        2) 반드시 단일 기술명 또는 기술 고유명칭이어야 한다.
           예) React, Vue, TypeScript, NestJS, Python, Docker, Kubernetes
        3) 다음 항목은 절대 키워드로 넣으면 안 된다:
           - 문화/정치/사회 용어 (예: 커피 문화, 정치, 환경)
           - 직무 설명 문장 (예: 웹 개발, 백엔드 아키텍처)
           - 모호한 용어 (예: 기술, 인터넷, AI 기술, 보안)
           - 문장형/두 단어 조합 (예: 브라우저 보안, 서버 성능 튜닝)
        4) metaNews는 “참고만” 할 수 있지만,
           targetRole 과 무관한 힌트는 무시해야 한다.
        5) 키워드는 반드시 기술 스택/언어/도구여야 한다.

        ────────────────────────────────────────────
        ⚠️ TrendDataContext 출력 규칙
        ────────────────────────────────────────────
        - JSON ONLY 반환
        - '{' 로 시작하고 '}' 로 끝나야 한다
        - null 절대 금지
        - 코드블록 금지
        - 설명문/서론 금지
        - JSON 외 어떤 텍스트도 출력하지 말 것

        반드시 아래 구조 그대로 출력한다:

        {
          "memberId": number,
          "targetRole": string,
          "startDate": "YYYY-MM-DD",
          "endDate": "YYYY-MM-DD",
          "keywords": [ 10개 기술 키워드 ],
          "rawTrendData": {
              "<keyword>": { getTrendData 결과 원본 }
          },
          "metaNews": "string"
        }

        ⚠️ getTrendData 도구 최소 3회 이상 호출 필수

        """;

    String userPrompt = """
        아래 값들은 그대로 JSON에 넣어라. 절대 수정 금지.

        memberId: %d
        targetRole: %s
        startDate: %s
        endDate: %s
        metaNews: %s

        TrendDataContext JSON을 생성하라.
        """.formatted(memberId, targetRole, startDate, endDate, metaNews);

    String llmResult = chatClient.prompt()
        .system(systemPrompt)
        .user(userPrompt)
        .tools(this)
        .call()
        .content();

    log.info("📦 [LLM RAW OUTPUT] {}", llmResult);

    // JSON -> TrendDataContext(DTO) 변환
    return mapper.readValue(llmResult, TrendDataContext.class);
  }
}
