package com.example.demo.newstrend.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ai.newstrend.TrendAiAgent;
import com.example.demo.newstrend.dao.TrendInsightDao;
import com.example.demo.newstrend.dto.request.TrendAnalyzeRequest;
import com.example.demo.newstrend.dto.response.InsightJson;
import com.example.demo.newstrend.dto.response.TrendAnalyzeResponse;
import com.example.demo.newstrend.dto.response.TrendGraph;
import com.example.demo.newstrend.entity.TrendInsight;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TrendService {
  @Autowired
  private TrendAiAgent trendAiAgent;
  @Autowired
  private TrendInsightDao trendInsightDao;
  @Autowired
  private ObjectMapper mapper;

  // 트렌드 분석 실행 + 저장
  @Transactional
  public TrendAnalyzeResponse analyzeTrend(Integer memberId) throws Exception {
    TrendAnalyzeRequest request = new TrendAnalyzeRequest();
    request.setMemberId(memberId);
    return trendAiAgent.runFullTrendAnalysis(request);
  }

  // 최신 트렌드 데이터 조회
  public TrendInsight getLatestTrend(Integer memberId){
    return trendInsightDao.selectLatestTrendInsight(memberId);
  }

  // 오늘 데이터 조회 없으면 자동 분석
  @Transactional
  public TrendAnalyzeResponse getAnalyzeToday(Integer memberId) throws Exception{
    LocalDate today = LocalDate.now();
    TrendInsight latest = trendInsightDao.selectLatestTrendInsight(memberId);

    // 오늘 분석 데이터가 있으면 DB값 반환
    if (latest != null && latest.getCreatedAt() != null && latest.getCreatedAt().toLocalDate().isEqual(today)){
      return entityToResponse(latest);
    }

    // 오늘 데이터 없을시 분석
    TrendAnalyzeRequest req = new TrendAnalyzeRequest();
    req. setMemberId(memberId);

    return trendAiAgent.runFullTrendAnalysis(req);
  }

  private TrendAnalyzeResponse entityToResponse(TrendInsight entity) throws Exception{
    TrendAnalyzeResponse response = new TrendAnalyzeResponse();
    TrendGraph graph = mapper.readValue(entity.getTrendJson(), TrendGraph.class);
    InsightJson insight = mapper.readValue(entity.getInsightJson(), InsightJson.class);

    response.setTrendJson(graph);
    response.setInsightJson(insight);
    
    return response;
  }
}
