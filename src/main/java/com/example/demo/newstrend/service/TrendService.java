package com.example.demo.newstrend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ai.newstrend.TrendAiAgent;
import com.example.demo.newstrend.dao.TrendInsightDao;
import com.example.demo.newstrend.dto.request.TrendAnalyzeRequest;
import com.example.demo.newstrend.dto.response.TrendAnalyzeResponse;
import com.example.demo.newstrend.entity.TrendInsight;

@Service
public class TrendService {
  @Autowired
  private TrendAiAgent trendAiAgent;
  @Autowired
  private TrendInsightDao trendInsightDao;

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
}
