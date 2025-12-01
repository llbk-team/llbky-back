package com.example.demo.newstrend.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.newstrend.dto.request.NewsAnalysisRequest;
import com.example.demo.newstrend.dto.response.NewsAnalysisResponse;
import com.example.demo.newstrend.dto.response.NewsAnalysisResult;
import com.example.demo.newstrend.dto.response.NewsSummaryResponse;
import com.example.demo.newstrend.entity.NewsSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 뉴스 통합 서비스
 * - 각 서비스들을 조합하여 비즈니스 로직 처리
 * - NewsCollectorService: 수집
 * - NewsAIService: AI 분석  
 * - NewsSummaryService: 저장
 */
@Service
@Slf4j
public class TotalNewsService {

  @Autowired
  private NewsAIService newsAIService;

  @Autowired
  private NewsCollectorService newsCollectorService;

  @Autowired
  private NewsSummaryService newsSummaryService;

  @Autowired
  private ObjectMapper objectMapper;

  /**
   * 단일 뉴스 분석 및 저장
   */
  @Transactional
  public NewsAnalysisResponse analyzeAndSaveNews(NewsAnalysisRequest request) throws Exception{
    //중복 체크
    if(newsSummaryService.existsByUrl(request.getSourceUrl())){
      log.warn("이미 존재하는 뉴스입니다.:{}",request.getSourceUrl());
    }

    //AI 분석 수행
  NewsAnalysisResult analysisResult = newsAIService.analyzeNews(request);
 log.info("AI 분석 완료 - 감정: {}, 신뢰도: {}", 
            analysisResult.getAnalysis().getSentiment(), 
            analysisResult.getAnalysis().getTrustScore());

    //엔티티 생성 (NewsAnalysisResult 구조에 맞게 매핑)
      NewsSummary entity = new NewsSummary();
      // 요청에 포함된 memberId 사용
      entity.setMemberId(request.getMemberId());
      entity.setTitle(request.getTitle());
      entity.setSourceName(request.getSourceName());
      entity.setSourceUrl(request.getSourceUrl());
      entity.setPublishedAt(LocalDate.now());
      // 최종 요약(중립화 적용된 문자열)
      entity.setSummaryText(analysisResult.getFinalSummary());
      // 상세 요약은 AI 분석 결과의 detailSummary 사용
      if (analysisResult.getAnalysis() != null) {
        entity.setDetailSummary(analysisResult.getAnalysis().getDetailSummary());
      }
      // analysisJson / keywordsJson은 ObjectMapper로 직렬화
      entity.setAnalysisJson(objectMapper.writeValueAsString(analysisResult.getAnalysis()));
      entity.setKeywordsJson(objectMapper.writeValueAsString(analysisResult.getKeywords()));

    //저장
    newsSummaryService.saveNewsSummary(entity);

    //Response 변환 및 반환
     NewsAnalysisResponse response = createResponse(entity,analysisResult);
    return response;
  }

  /**
   * 뉴스 수집, AI 분석 및 저장 통합 처리
   * 
   * @param keywords 검색 키워드 목록
   * @param memberId 회원 ID
   * @return 분석된 뉴스 개수
   */
  @Transactional
  public int collectAndAnalyzeNews(List<String> keywords, Integer memberId) throws Exception {
    log.info("뉴스 수집 및 분석 통합 처리 시작 - keywords: {}, memberId: {}", keywords, memberId);
    
    // 1. 뉴스 수집 (NewsCollectorService)
    List<NewsAnalysisRequest> collectedNews = newsCollectorService.collectNews(keywords, memberId);
    log.info("뉴스 수집 완료 - {}건", collectedNews.size());
    
    if (collectedNews.isEmpty()) {
      log.info("수집된 뉴스가 없습니다.");
      return 0;
    }
    
    int totalAnalyzed = 0;
    int duplicateCount = 0;
    int errorCount = 0;
    
    // 2. 수집된 각 뉴스에 대해 AI 분석 및 저장
    for (NewsAnalysisRequest newsRequest : collectedNews) {
      try {
        // 2-1. URL 중복 체크
        if (newsSummaryService.existsByUrl(newsRequest.getSourceUrl())) {
          log.debug("이미 저장된 뉴스: {}", newsRequest.getSourceUrl());
          duplicateCount++;
          continue;
        }
        
        // 2-2. AI 분석 실행
        log.debug("AI 분석 시작: {}", newsRequest.getTitle());
        NewsAnalysisResult analysisResult = newsAIService.analyzeNews(newsRequest);
        
        // 2-3. 엔티티 생성 및 AI 분석 결과 매핑
        NewsSummary entity = new NewsSummary();
        entity.setMemberId(newsRequest.getMemberId());
        entity.setTitle(newsRequest.getTitle());
        entity.setSourceName(newsRequest.getSourceName());
        entity.setSourceUrl(newsRequest.getSourceUrl());
        entity.setPublishedAt(LocalDate.now());
        
        // 2-4. AI 분석 결과를 JSON으로 저장
        entity.setSummaryText(analysisResult.getFinalSummary()); // AI 요약문
        entity.setDetailSummary(analysisResult.getOriginalContent());
        entity.setAnalysisJson(objectMapper.writeValueAsString(analysisResult.getAnalysis()));
        entity.setKeywordsJson(objectMapper.writeValueAsString(analysisResult.getKeywords()));
        
        // 2-5. 저장
        newsSummaryService.saveNewsSummary(entity);
        totalAnalyzed++;
        
        log.info("뉴스 분석 및 저장 완료: {} (감정: {}, 신뢰도: {})", 
            newsRequest.getTitle(), 
            analysisResult.getAnalysis().getSentiment(),
            analysisResult.getAnalysis().getTrustScore());
        
        // API 호출 제한을 위한 대기
        Thread.sleep(1000); // AI 분석 시간을 고려하여 1초 대기
        
      } catch (Exception e) {
        log.error("뉴스 분석 및 저장 실패: {}", newsRequest.getTitle(), e);
        errorCount++;
      }
    }
    
    log.info("뉴스 수집 및 분석 완료 - 수집: {}건, 분석 성공: {}건, 중복: {}건, 오류: {}건", 
        collectedNews.size(), totalAnalyzed, duplicateCount, errorCount);
    
    return totalAnalyzed;
  }

  /**
   * 오늘 날짜 뉴스 조회(없으면 자동 수집)
   */
  @Transactional
  public List<NewsAnalysisResponse> getTodayNewsByMember(int memberId, int limit) throws Exception{
     log.info("오늘 뉴스 조회 - memberId: {}, limit: {}", memberId, limit);
     //1. db 먼저 조회
     List<NewsAnalysisResponse> responses =newsSummaryService.getTodayNewsByMember(memberId, limit);
   //2. 데이터 없으면 자동 수집
   if(responses==null || responses.isEmpty()){
    log.info("오늘 뉴스 데이터 없음 - 자동 수집 시작");
            int analyzed = collectAndAnalyzeNews(null, memberId);  // ✅ 통합 메소드 호출
            log.info("자동 수집 완료 - {}건 분석됨", analyzed);
    //3. 수집 후 다시 조회
    responses = newsSummaryService.getTodayNewsByMember(memberId, limit);

   }
   log.info("오늘 뉴스 조회 완료 {}건 ", responses.size());
   return responses;
  }

  /**
   * 회원별 최신 뉴스 조회
   */
  public List<NewsAnalysisResponse> getLatestNewsByMember(int memberId, int limit) throws JsonProcessingException{
    List<NewsAnalysisResponse> responses = newsSummaryService.getLatestNewsByMember(memberId, limit);

     log.info("최신 뉴스 조회 완료 - {}건", responses.size());
      return responses;
  }

  /**
   * 뉴스 검색 및 수집 (API 엔드포인트용)
   */
  @Transactional
  public int searchNews(List<String> keywords, Integer memberId) throws Exception {
      log.info("뉴스 검색 시작 - keywords: {}, memberId: {}", keywords, memberId);
      
      // ✅ 통합 메소드 호출
      int analyzed = collectAndAnalyzeNews(keywords, memberId);
      
      log.info("뉴스 검색 완료 - {}건 분석됨", analyzed);
      return analyzed;
  }

  /**
   * Response 생성 헬퍼 메서드
   */
  private NewsAnalysisResponse createResponse(NewsSummary saved, NewsAnalysisResult result) {        NewsAnalysisResponse response = new NewsAnalysisResponse();

        // 기본 정보
        response.setSummaryId(saved.getSummaryId());
        response.setTitle(saved.getTitle());
        response.setSourceName(saved.getSourceName());
        response.setSourceUrl(saved.getSourceUrl());
        response.setPublishedAt(saved.getPublishedAt());

        // 요약 정보
        response.setSummaryText(saved.getSummaryText());
        response.setDetailSummary(saved.getDetailSummary());

        // AI 분석 결과
      NewsSummaryResponse analysis = result.getAnalysis();
        response.setSentiment(analysis.getSentiment());
        response.setTrustScore(analysis.getTrustScore());
        response.setBiasDetected(analysis.getBiasDetected());
        response.setBiasType(analysis.getBiasType());
        response.setCategory(analysis.getCategory());

        // 키워드
        response.setKeywords(result.getKeywords());

        response.setCreatedAt(saved.getCreatedAt());

        return response;
    }



}
