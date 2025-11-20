package com.example.demo.newstrend.controller;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.newstrend.dao.JobInsightDao;
import com.example.demo.newstrend.dao.NewsSummaryDao;
import com.example.demo.newstrend.dao.SavedKeywordDao;
import com.example.demo.newstrend.dao.TrendInsightDao;
import com.example.demo.newstrend.entity.JobInsight;
import com.example.demo.newstrend.entity.NewsSummary;
import com.example.demo.newstrend.entity.SavedKeyword;
import com.example.demo.newstrend.entity.TrendInsight;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/test/all")
@RequiredArgsConstructor
public class InsightAllTestController {

  private final NewsSummaryDao newsSummaryDao;
  private final SavedKeywordDao savedKeywordDao;
  private final TrendInsightDao trendInsightDao;
  private final JobInsightDao jobInsightDao;

  // ---------------------------
  // 1) NEWS SUMMARY 테스트
  // ---------------------------
  @PostMapping("/news/insert")
  public String insertNews() {
    NewsSummary ns = new NewsSummary();
    ns.setMemberId(1);
    ns.setSourceName("네이버 뉴스");
    ns.setSourceUrl("https://news.example.com/ai");
    ns.setTitle("AI 채용 급증");
    ns.setPublishedAt(LocalDate.now());
    ns.setSummaryText("AI 채용이 증가하고 있습니다.");
    ns.setDetailSummary("2025년에는 AI 인력 수요가 폭발적으로 증가하고...");
    ns.setAnalysisJson("{\"sentiment\":\"positive\",\"trust\":95}");
    ns.setKeywordsJson("[\"AI\",\"백엔드\",\"ML\"]");

    int r = newsSummaryDao.insertNewsSummary(ns);
    return "News INSERT result = " + r;
  }

  @GetMapping("/news/list")
  public Object listNews() {
    return newsSummaryDao.selectLatestNewsByMemberId(1, 1);
  }

  @DeleteMapping("/news/delete/{id}")
  public String deleteNews(@PathVariable("id") int id) {
    int r = newsSummaryDao.deleteNewsSummary(id);
    return "News DELETE result = " + r;
  }

  // ---------------------------
  // 2) SAVED KEYWORD 테스트
  // ---------------------------
  @PostMapping("/keyword/insert")
  public String insertKeyword() {
    SavedKeyword sk = new SavedKeyword();
    sk.setMemberId(1);
    sk.setKeyword("MLOps");
    sk.setSourceLabel("직접추가");

    int r = savedKeywordDao.insertSavedKeyword(sk);
    return "Keyword INSERT result = " + r;
  }

  @GetMapping("/keyword/list")
  public Object listKeyword() {
    return savedKeywordDao.selectSavedKeywordListByMemberId(1);
  }

  @GetMapping("/keyword/find")
  public Object findKeyword() {
    return savedKeywordDao.selectSavedKeyword(1, "MLOps");
  }

  @DeleteMapping("/keyword/delete/{id}")
  public String deleteKeyword(@PathVariable("id") int id) {
    int r = savedKeywordDao.deleteSavedKeyword(id);
    return "Keyword DELETE result = " + r;
  }

  // ---------------------------
  // 3) TREND INSIGHT 테스트
  // ---------------------------
  @PostMapping("/trend/insert")
  public String insertTrend() {
    TrendInsight ti = new TrendInsight();
    ti.setMemberId(1);
    ti.setBaseJobTitle("백엔드 개발자");
    ti.setStartDate(LocalDate.of(2025, 1, 1));
    ti.setEndDate(LocalDate.of(2025, 1, 7));
    ti.setInsightJson("{\"market\":\"AI 채용 증가\",\"score\":88}");

    int r = trendInsightDao.insertTrendInsight(ti);
    return "Trend INSERT result = " + r;
  }

  @GetMapping("/trend/latest")
  public TrendInsight latestTrend() {
    return trendInsightDao.selectLatestTrendInsight(1);
  }

  @DeleteMapping("/trend/delete/{id}")
  public String deleteTrend(@PathVariable("id") int id) {
    int r = trendInsightDao.deleteTrendInsight(id);
    return "Trend DELETE result = " + r;
  }

  // ---------------------------
  // 4) JOB INSIGHT 테스트
  // ---------------------------
  @PostMapping("/job/insert")
  public String insertJob() {
    JobInsight ji = new JobInsight();
    ji.setMemberId(1);
    ji.setBaseJobTitle("백엔드 개발자");
    ji.setAnalysisJson("{\"strength\":\"API 설계 능력 우수\",\"weakness\":\"테스트 부족\"}");
    ji.setRelatedJobsJson("[\"AI 엔지니어\", \"데이터 엔지니어\"]");

    int r = jobInsightDao.insertJobInsight(ji);
    return "Job INSERT result = " + r;
  }

  @GetMapping("/job/latest")
  public JobInsight latestJob() {
    return jobInsightDao.selectLatestJobInsight(1);
  }

  @DeleteMapping("/job/delete/{id}")
  public String deleteJob(@PathVariable("id") int id) {
    int r = jobInsightDao.deleteJobInsight(id);
    return "Job DELETE result = " + r;
  }
}
