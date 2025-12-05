package com.example.demo.ai.newstrend;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.newstrend.dao.TrendInsightDao;
import com.example.demo.newstrend.dto.response.TrendAnalyzeResponse;
import com.example.demo.newstrend.dto.response.TrendDataContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/*
  TrendAnalysisAgent
  - LLM이 분석만 수행하는 에이전트
  rawTrendData(트렌드 데이터 수집본)을 기반으로 계산 및 요약 분석 수행
*/
@Component
@Slf4j
public class TrendAnalysisAgent {
  private ChatClient chatClient;
  @Autowired
  private ObjectMapper mapper;

  public TrendAnalysisAgent(ChatClient.Builder chatClientBuilder, TrendInsightDao trendInsightDao, MemberDao memberDao,
      WebClient.Builder webClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public TrendAnalyzeResponse analyze(TrendDataContext context) throws Exception {

    String systemPrompt = """
        너는 채용/기술 트렌드를 분석하는 TrendAnalysisAgent이다.
        입력은 TrendDataContext(JSON)이며, 너는 계산과 분석만 수행한다.
        키워드 생성 또는 원본 데이터 수집은 절대 하지 않는다. (TrendDataAgent 역할)
        metaNews는 뉴스 흐름 참고용이다. 채용 시장 분위기에는 적극 참고해도 좋다

        metaNews는 참고만 하고, 문장을 그대로 출력하거나 복사하면 안된다.

        ⚠ 출력 규칙 (절대 위반 금지)
        - JSON ONLY 출력
        - '{' 로 시작하고 '}' 로 끝나야 함
        - null 금지
        - 자연어 설명/해설 금지
        - 필드명/구조 절대 변경 금지
        - 순서 변경 금지
        - 추가 필드 생성 금지

        ==================================================
        ■ TrendAnalyzeResponse — 반드시 아래 구조 동일하게 출력
        ==================================================
        {
          "trendJson": {
            "keywords": ["string"],
            "counts": [number],
            "rawTrendData": {
              "<keyword>": {
                ... 원본 그대로 ...
              }
            }
          },

          "insightJson": {
            "summarycard": {
              "majorKeyword": "string",
              "avgInterest": number,
              "keywordCount": number
            },

            "industrySentiment": [
              { "industry": "string", "positive": number, "neutral": number, "negative": number }
            ],

            "wordCloud": [
              { "keyword": "string", "score": number }
            ],

            "marketInsight": ["string", "string", "string"],
            "finalSummary": "string"
          }
        }

        ==================================================
        ■ 계산 규칙
        ==================================================

        ────────────────────────────────────────
        ★ TrendGraph.counts = 각 키워드의 “평균 관심도”
        ────────────────────────────────────────
        1) rawTrendData[keyword] 안의 ratio 배열 사용
           - 구조가 { data: [] } 또는 { results: [ { data: [] } ] } 둘 중 하나일 수 있음
        2) 산술평균 계산 → 반올림하여 int
        3) 배열 비어있으면 0

        ────────────────────────────────────────
        ★ SummaryCard.majorKeyword
        ────────────────────────────────────────
        trendJson.keywords[0]

        ────────────────────────────────────────
        ★ SummaryCard.avgInterest = 전체 평균 관심도
        ────────────────────────────────────────
        - counts 배열 평균
        - 소수점 1자리
        - 비어있으면 0.0

        ────────────────────────────────────────
        ★ SummaryCard.keywordCount
        ────────────────────────────────────────
        - trendJson.keywords 길이

        ────────────────────────────────────────
        ★ industrySentiment
        ────────────────────────────────────────
        - 6개 산업군
        - positive + neutral + negative = 100

        ────────────────────────────────────────
        ★ wordCloud
        ────────────────────────────────────────
        - 10개
        - score 20~100

        ────────────────────────────────────────
        ★ marketInsight / finalSummary
        ────────────────────────────────────────
        평균 관심도(counts 평균), 주요 키워드산업, 분위기 기반으로 작성
        최소 3문장이상 작성

        ==================================================
        이제 주어진 TrendDataContext(JSON)를 기반으로
        위 규칙 100% 적용하여 TrendAnalyzeResponse JSON ONLY 를 출력하라.


        """;

    String userPrompt = """
        아래는 TrendDataContext(JSON)이다.
        이 원본 데이터를 기반으로 TrendAnalyzeResponse를 생성하라.

        TrendDataContext:
        %s
        """.formatted(
        mapper.writeValueAsString(context));

    String llmResult = chatClient.prompt()
        .system(systemPrompt)
        .user(userPrompt)
        .call()
        .content();

    TrendAnalyzeResponse response = mapper.readValue(llmResult, TrendAnalyzeResponse.class);

    return response;
  }
}
