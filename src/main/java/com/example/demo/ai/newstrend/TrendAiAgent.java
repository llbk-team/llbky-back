package com.example.demo.ai.newstrend;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
  @Autowired
  private ObjectMapper mapper;

  public TrendAiAgent(ChatClient.Builder chatClientBuilder, TrendInsightDao trendInsightDao, MemberDao memberDao) {
    this.chatClient = chatClientBuilder.build();
    this.trendInsightDao = trendInsightDao;
    this.memberDao = memberDao;
  }

  @Tool(description = "네이버 데이터랩 API에서 트렌드 데이터 가져오기")
  public Map<String, Object> getTrendData(String keyword, String startDate, String endDate) {
    return Map.of();
  }

  @Tool(description = "연관 키워드 탐색 검색 API 호출")
  public List<String> searchKeyword(String query) {
    return List.of();
  }

  public TrendAnalyzeResponse runFullTrendAnalysis(TrendAnalyzeRequest request) throws Exception {
    // 사용자 희망 직무 조회
    Member member = memberDao.findById(request.getMemberId());
    String targetRole = member.getJobRole();

    // 자동 기간 설정(최근 30일)
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusDays(30);

    // 프롬프트
    String systemprompt = """
        너는 채용 및 기술 시장 트렌드 분석 전문가이다.

        -targetRole(희망 직무)을 기반으로 최근 30일간의 시장 트렌드를 분석한다.
        -필요하면 아래 도구를 호출하여 실제 데이터를 수집한다:
          * getTrendData(keyword, startDate, endDate)
          * searchKeyword(query)

        규칙:
        - null 금지
        - 트렌드 데이터 기반으로만 인사이트 작성(뉴스 기반 분석은 아직 금지)
        - 출력은 반드시 JSON만 포함해야 한다.
        - trend_json 기간은 DB에 저장되므로 JSON 내부에 포함하지 않는다.
        - 다음 JSON 구조에 맞춰라:

        {
          "trendJson": {
            "labels": [],
            "counts": [],
            "rawTrendData": {}
          },
          "insightJson": {
            "summaryCard": {
              "majorKeyword": "",
              "avgInterest": 0,
              "keywordCount": 0,
              "marketActivity": 0
            },
            "keywordTrend": [],
            "industrySentiment": [
              { "industry": "", "positive": 0, "neutral": 0, "negative": 0 }
            ],
            "marketInsight": [],
            "finalSummary": ""
          }
        }
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
