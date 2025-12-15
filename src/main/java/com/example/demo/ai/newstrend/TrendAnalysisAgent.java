package com.example.demo.ai.newstrend;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.newstrend.dao.TrendInsightDao;
import com.example.demo.newstrend.dto.response.SentimentResponse;
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
  @Autowired
  private SentimetalAnalysisAgent sentimetalAnalysisAgent;

  public TrendAnalysisAgent(ChatClient.Builder chatClientBuilder, TrendInsightDao trendInsightDao,
      MemberDao memberDao) {
    this.chatClient = chatClientBuilder.build();
  }

  public TrendAnalyzeResponse analyze(TrendDataContext context) throws Exception {

    String systemPrompt = """
        너는 채용/기술 트렌드를 분석하는 TrendAnalysisAgent이다.
        입력은 TrendDataContext(JSON)이며,
        다음 데이터를 포함한다:
        - keywords: 검색 트렌드 기반 키워드
        - rawTrendData: 각 키워드의 검색 트렌드 원본 데이터
        - keywordFrequency: 뉴스에서 키워드가 언급된 횟수
        - metaNews: 뉴스 요약 정보
        너는 계산과 분석만 수행한다.
        키워드 생성 또는 원본 데이터 수집은 절대 하지 않는다. (TrendDataAgent 역할)
        metaNews는 뉴스 50개에 대한 이슈에 대한 여론을 기반으로 감정을 분석한 내용이다. 이 흐름을 참고하여서 분석한다.

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
        - majorKeyword는 반드시 trendJson.keywords 배열에 포함된 키워드 중에서만 선택해야 한다.
        - 새로운 키워드를 생성하거나 배열 외의 값을 선택해서는 안 된다.
        - 아래 요소를 종합하여 LLM이 계산 결과를 기반으로 선정한다. 
          1) 각 키워드의 최근 관측 기간 내 평균 관심도
          2) 최근 7일 관측 구간에서 초반,후반 평균 ratio 비교를 통한 관심도 변화
          - 각 키워드 k에 대해 최근 7일 ratio 값을 r1, r2, r3, r4, r5, r6, r7 로 정의한다.
          - 초반 평균 x 는 다음과 같이 계산한다:
          x = (r1 + r2 + r3) / 3
          - 후반 평균 y 는 다음과 같이 계산한다:
          y = (r5 + r6 + r7) / 3
          - 관심도 변화 지표 C 는 다음과 같이 계산한다:
          C = y / (x + 1)
          - C 값이 클수록 최근 관심도가 더 증가한 것으로 판단한다.

          3) metaNews에서의 언급 빈도 및 맥락
          - Concept Keyword란,
            trendJson.keywords 배열에 포함된 키워드 중
            산업/기술의 상위 개념을 나타내는 키워드를 의미한다.
          - Concept Keyword 여부는 외부 지식이 아니라
            trendJson.keywords 배열 내 키워드들의 의미 관계를 기준으로 판단한다.
          - 우선적으로 Concept Keyword 중에서 C 값이 가장 큰 키워드를 선정한다.
          - Concept Keyword가 없거나 C 값이 유사할 경우,
            전체 키워드 중 C 값이 가장 큰 키워드를 선정한다.
          - 최종적으로 majorKeyword는 하나만 선택한다.
          - 위 기준을 종합했을 때 현재 채용 시장에서 가장 주목할 기술 하나만 선정한다.

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
        - wordCloud는 검색 관심도(trendJson.keywords)가 아니라,
          뉴스 언급 빈도(keywordFrequency)를 기준으로 생성한다.
        - 뉴스에서 실제로 자주 언급된 키워드만 사용한다.
        - 기술, 서비스, 개념, 이슈 키워드를 모두 포함할 수 있다.
        - 단, 다음 조건을 만족해야 한다:
          1) 뉴스 키워드 후보 목록(keywordPool)에 포함된 키워드일 것
          2) targetRole과 완전히 무관한 키워드는 제외할 것
        - 상위 10개 키워드만 선정한다.

        점수(score) 계산 규칙:
        - score는 뉴스 언급 빈도의 상대적 크기를
          로그 스케일로 변환하여 계산한다.
        - 계산식은 다음과 같다:

          score = log(freq + 1) / log(maxFreq + 1) * 100

        - score가 30 미만일 경우 30으로 보정한다.
        - keywordFrequency 값은 이미 계산된 데이터이므로
          새로 추론하거나 수정하지 않는다.
        - 제공된 값을 그대로 사용하여 score를 계산한다.

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
        mapper.writeValueAsString(context)
        );

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
