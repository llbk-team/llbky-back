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

  public TrendAnalysisAgent(ChatClient.Builder chatClientBuilder, TrendInsightDao trendInsightDao, MemberDao memberDao) {
    this.chatClient = chatClientBuilder.build();
  }

  public TrendAnalyzeResponse analyze(TrendDataContext context) throws Exception {

    String systemPrompt = """
        너는 채용/기술 트렌드를 분석하는 TrendAnalysisAgent이다.
        입력은 TrendDataContext(JSON)이며, 너는 계산과 분석만 수행한다.
        키워드 생성 또는 원본 데이터 수집은 절대 하지 않는다. (TrendDataAgent 역할)
        metaNews는 뉴스 흐름 참고용이다. 채용 시장 분위기에는 적극 참고해도 좋다

        ⚠ 출력 규칙 (절대 위반 금지)
        - JSON ONLY 출력
        - '{' 로 시작하고 '}' 로 끝나야 함
        - null 금지
        - 자연어 설명/해설 금지
        - 필드명/구조 절대 변경 금지
        - 순서 변경 금지
        - 추가 필드 생성 금지
        - 한국어로 출력해라

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
        industrySentiment 배열은 반드시 6개의 산업군으로 구성해야 한다.
        “산업군 이름, 감정 비율, 산업군 구성 방식”은 아래 규칙을 100% 따라야 한다.

        ────────────────────────────────────────
        ■ 1) 산업군 이름 생성 규칙 (고정 리스트 금지)
        ────────────────────────────────────────

        산업군 이름은 TrendDataContext 안의 다음 정보를 바탕으로
        LLM이 "적절하고 의미 있는 산업군"을 직접 생성해야 한다:

        1) jobGroup (사용자가 선택한 직군)

        2) targetRole (사용자가 선택한 직무)

        3) keywords (기술/도메인 관련 키워드)

        4) metaNews (뉴스 기반 산업 흐름 및 감정)

        위 4가지 정보를 종합하여  
        **사용자 직무/산업 맥락과 실제 시장 흐름을 반영한 6개의 산업군을 생성해야 한다.**

        6개의 산업군은 서로 겹치지 않고, 의미적으로 구분되어야 한다.

        ────────────────────────────────────────
        ■ 2) 감정 비율 생성 규칙 (positive/neutral/negative = 100)
        ────────────────────────────────────────

        각 산업군의 감정 비율은 아래 기준으로 반드시 다르게 계산해야 한다:

        1) metaNews 내 industrySentiment 또는 issue 기반 감정 경향 반영  
          - AI 관련 긍정 기사 ↑ → AI 산업군 positive 비중 증가  
          - 보안 사고, 채용 축소 뉴스 ↑ → 관련 산업군 negative 비중 증가  

        2) 각 산업군의 positive + neutral + negative = 100 유지

        ────────────────────────────────────────
        ■ 3) 출력 형태
        ────────────────────────────────────────

        industrySentiment 배열은 반드시 아래 구조의 6개 항목으로 구성되어야 한다:

        {
          "industry": "산업군 이름",
          "positive": number,
          "neutral": number,
          "negative": number
        }

        순서 변경 금지, null 금지, 생략 금지.


        ────────────────────────────────────────
        ★ wordCloud
        ────────────────────────────────────────
        - 10개
        - score 20~100

        ────────────────────────────────────────
        ★ marketInsight
        ────────────────────────────────────────
        평균 관심도(counts 평균), 주요 키워드산업, 분위기 기반으로 작성
        최소 3문장이상 작성
        
        ────────────────────────────────────────
        ★ finalSummary
        ────────────────────────────────────────
        finalSummary는 다음 내용을 모두 포함해야 한다:

        1) industrySentiment에 포함된 6개 산업군 각각에 대해  
          - 해당 산업군의 positive/neutral/negative 비율이 왜 그렇게 결정되었는지  
          - metaNews(뉴스 감정 분석) 기반의 구체적 사건·이슈를 근거로 설명해야 한다.

        2) 산업군별 설명은 최소 1문장 이상이며,
          총 6개 산업군 모두에 대해 누락 없이 작성해야 한다.

        3) 마지막 단락에서 6개 산업군의 흐름을 종합하여  
          전체 기술·채용 시장 분위기를 요약해야 한다.

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
    log.info("[FINAL SUMMARY] {}", response.getInsightJson().getFinalSummary());

    return response;
  }
}
