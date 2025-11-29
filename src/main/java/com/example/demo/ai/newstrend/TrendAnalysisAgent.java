package com.example.demo.ai.newstrend;

import java.time.LocalDate;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.newstrend.dao.TrendInsightDao;
import com.example.demo.newstrend.dto.response.TrendAnalyzeResponse;
import com.example.demo.newstrend.dto.response.TrendDataContext;
import com.example.demo.newstrend.entity.TrendInsight;
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
  @Autowired
  private TrendInsightDao trendInsightDao;

  public TrendAnalysisAgent(ChatClient.Builder chatClientBuilder, TrendInsightDao trendInsightDao, MemberDao memberDao,
      WebClient.Builder webClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public TrendAnalyzeResponse analyze(TrendDataContext context) throws Exception {

    String systemPrompt = """
        너는 채용/기술 트렌드를 분석하는 TrendAnalysisAgent 이다.
        입력은 TrendDataContext JSON이며, 너는 계산과 분석만 수행한다.
        키워드 생성 또는 원본 데이터 수집은 절대 하지 않는다. (TrendDataAgent 역할)

        ⚠ 출력 규칙 (절대 위반 금지)
        - JSON ONLY 출력
        - '{' 로 시작하고 '}' 로 끝나야 함
        - null 금지
        - 자연어 설명/해설 금지
        - 필드명/구조 절대 변경 금지
        - 순서 변경 금지
        - 추가 필드 생성 금지

        ==================================================
        ■ TrendAnalyzeResponse — 반드시 아래 구조 100% 동일하게 출력
        ==================================================
        {
          "trendJson": {
            "keywords": [ "string", ... 최소 1개 ],
            "counts": [ number(int), ... keywords 길이와 동일 ],
            "rawTrendData": {
                "<keyword>": {
                    "title": "string",
                    "keywords": [ "string", ... 최소 1개 ],
                    "data": [
                        {
                            "period": "YYYY-MM-DD",
                            "ratio": number
                        }
                    ]
                }
            }
          },

          "insightJson": {
            "summarycard": {
              "majorKeyword": "string",
              "avgInterest": number,
              "interestChange": "string",
              "keywordCount": number
            },

            "keywordTrend": [
              "string"
            ],

            "industrySentiment": [
              {
                "industry": "string",
                "positive": number,
                "neutral": number,
                "negative": number
              }
            ],

            "wordCloud": [
              {
                "keyword": "string",
                "score": number
              }
            ],

            "marketInsight": [
              "string",
              "string",
              "string"
            ],

            "finalSummary": "string"
          }
        }

        ==================================================
        ■ ⛔ 계산 규칙 (절대 변경 금지)
        ==================================================

        ★ TrendGraph.counts 계산식
        1) 각 keyword에 대해 rawTrendData[keyword].data[*].ratio 배열을 그대로 나열한다.
        2) ratio 값들을 모두 더해 “sum”을 계산한다.
        3) 평균(avg) = sum / 개수
        4) 반올림(round)하여 정수(int)로 만든다.
        5) data 배열이 비어있으면 counts 값 = 0
        6) 계산 과정(sum/avg)은 최종 JSON에 포함하지 않는다.

        반드시 다음과 같은 방식으로 내부 계산한다:

        예시)
        ratio = [40.1, 50.2, 60.3]
        sum = 40.1 + 50.2 + 60.3 = 150.6
        avg = 150.6 / 3 = 50.2
        round(avg) = 50

        ★ SummaryCard.majorKeyword
        - trendJson.keywords[0]

        ★ SummaryCard.avgInterest
        - majorKeyword의 ratio 평균
        - 소수점 1자리까지 표시 (예: 72.4)
        - ratio 배열 비어있으면 0.0

        ★ SummaryCard.interestChange
        - ratio 개수 >= 2 일 때만 계산
              first = 첫 번째 ratio
              last = 마지막 ratio
              change = ((last - first) / first) * 100
        - ratio 0개 또는 1개 → 0%
        - 표기 규칙:
              change > 0 → "+12%"
              change < 0 → "-8%"
              change = 0 → "0%"

        소수점은 반올림하여 정수로 만든다.

        ★ keywordTrend 계산식
        각 keyword 동일 공식 적용:
           change = ((last - first) / first) * 100

        출력 형식:
           양수 → "키워드 ▲12%"
           음수 → "키워드 ▼8%"
           0 → "키워드 0%"

        반드시 부호 포함. 소수점 반올림 정수 처리.

        ★ industrySentiment
        - targetRole 과 관련 산업군 6개 생성
        - positive + neutral + negative = 100

        ★ wordCloud
        - 10개 생성
        - score 범위 20~100

        ★ marketInsight
        - keywordTrend / avgInterest 기반으로 3문장 생성

        ★ finalSummary
        - 전체 트렌드 1~2문장 요약

        ==================================================
        ■ 출력 검증 규칙 (중요)
        ==================================================
        - JSON 외 텍스트(설명/문장/인사말) 포함 시 오류로 간주하고 출력하지 않는다.
        - 구조/필드명 누락 시 출력하지 않는다.
        - 계산 규칙을 따르지 못하면 출력하지 않는다.

        ==================================================
        이제 제공된 TrendDataContext(JSON)를 기반으로
        위 규칙을 정확히 사용하여 TrendAnalyzeResponse JSON ONLY 를 생성하라.

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
