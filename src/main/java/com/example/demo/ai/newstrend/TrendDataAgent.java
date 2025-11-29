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
  public Map<String, Object> getTrendData(String keyword, String startDate, String endDate){
    log.info("🔧 [TOOL CALLED] getTrendData(keyword={}, start={}, end={})",
                keyword, startDate, endDate);
    try{
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
    } catch (Exception e){
      log.error("❌ [TOOL ERROR] getTrendData 실패: keyword={}, msg={}",
                    keyword, e.getMessage());
      return Map.of("error", "API 호출 실패: " + e.getMessage());
    }
  }

  public TrendDataContext collect(Integer memberId) throws Exception{
    log.info("🚀 [TrendDataAgent] 데이터 수집 시작 memberId={}", memberId);
    // 사용자 희망 직무 조회
    Member member = memberDao.findById(memberId);
    String targetRole = member.getJobRole();

    // 수집 날짜 범위
    LocalDate end = LocalDate.now();
    LocalDate start = end.minusDays(7);

    String startDate = start.toString();
    String endDate = end.toString();

    String systemPrompt = """
      너는 검색 트렌드 수집을 위한 데이터 수집용 에이전트이다.
      너는 계산이나 분석을 하지 않는다. (계산은 TrendAnalysisAgent가 수행함)

      너의 역할:
      1) targetRole 기반으로 10개만 관련 키워드 생성
      2) 각 키워드에 대해 반드시 getTrendData(keyword, startDate, endDate) 도구 호출
      3) 수집된 원본 데이터(rawTrendData)를 그대로 JSON에 넣기
      4) 결과를 TrendDataContext 형태 JSON으로 반환

      다음 유형의 표현은 절대 키워드로 사용하면 안 된다:
      - 직무 설명형 문장 (예: "서버 관리", "백엔드 아키텍처")
      - 모호한 문장형 표현 (예: "데이터 처리", "서버 사이드 프로그래밍")
      - 너무 길거나 문장처럼 보이는 키워드
      - '기술 키워드' 또는 '짧은 단어형 검색 키워드' 형태로만 생성해야 한다.

      TrendDataContext 구조:
      {
        "memberId": number,
        "targetRole": string,
        "startDate": "YYYY-MM-DD",
        "endDate": "YYYY-MM-DD",
        "keywords": [ ... 10개 ... ],
        "rawTrendData": {
            "<keyword>": { ... getTrendData() 원본 결과 ... }
        }
      }

      규칙:
      - JSON ONLY 반환
      - 설명/문장 금지, '{' 로 시작 '}' 로 끝남
      - null 금지
      - 키워드는 반드시 targetRole 기반
      - getTrendData 도구를 최소 3번 이상 호출해야 함
      """;

  String userPrompt = """
      트렌드 원본 데이터를 수집해라.

      memberId: %d
      targetRole: %s
      startDate: %s
      endDate: %s
      TrendDataContext JSON을 생성하라.
      """.formatted(memberId, targetRole, startDate, endDate);

  String llmResult = chatClient.prompt()
    .system(systemPrompt)
    .user(userPrompt)
    .tools(this)
    .call()
    .content();

    log.info("📦 [LLM RAW OUTPUT] {}", llmResult);

    // JSON -> TrendDataContext(DTO) 변환
    return mapper.readValue(llmResult,TrendDataContext.class);
  }
}
