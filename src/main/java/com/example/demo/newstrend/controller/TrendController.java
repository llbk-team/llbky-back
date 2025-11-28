package com.example.demo.newstrend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.newstrend.dto.response.TrendAnalyzeResponse;
import com.example.demo.newstrend.entity.TrendInsight;
import com.example.demo.newstrend.service.TrendService;




@RestController
@RequestMapping("/trend")
public class TrendController {
  @Autowired
  private TrendService trendService;

  // DB 저장된 최신 트렌드 데이터 조회(test용)
  @GetMapping("/latest")
  public TrendInsight getLatestTrend(@RequestParam("memberId") Integer memberId) {
    return trendService.getLatestTrend(memberId);
  }

  // 트렌드 분석 실행 + 저장(test용)
  @PostMapping("/analyze")
  public TrendAnalyzeResponse runTrendAnalysis(@RequestParam("memberId") Integer memberId) throws Exception {
    return trendService.analyzeTrend(memberId);
  }
  
  @GetMapping("/today")
  public TrendAnalyzeResponse getTodayTrend(@RequestParam("memberId") Integer memberId) throws Exception {
    return trendService.getAnalyzeToday(memberId);
  }
  
  
}
