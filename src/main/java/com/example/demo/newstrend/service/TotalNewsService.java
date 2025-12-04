package com.example.demo.newstrend.service;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.dto.Member;
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
 * 
 * 단계	내용	클래스
    1번	뉴스 원문 수집	String(originalContent)
    2번	AI 분석 결과 조각들 반환	NewsSummaryResponse, NewsKeywordResponse
    3번	AI 결과를 하나로 묶음	NewsAnalysisResult ← 여기!
    4번	DB 저장용 entity로 변환	NewsSummary
    5번	프론트 응답 DTO	NewsAnalysisResponse

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

  @Autowired
  private MemberDao memberDao;

  // 회원 맞춤 뉴스 피드 조회(오늘 데이터 없으면 자동 수집)
  @Transactional
  public List<NewsAnalysisResponse> getNewsFeed(
      int memberId, 
      String period,
      LocalDateTime lastPublishedAt,   
      Integer lastSummaryId,
      int limit) throws Exception{
    
    List<String> jobGroupKeywords= generateJobGroupKeywords(memberId);

      if(jobGroupKeywords.isEmpty()){
        log.warn("키워드 생성 실패 - memberId: {}", memberId);
        return new ArrayList<>();
      }
      log.info("생성된 키워드: {}", jobGroupKeywords);

      List<NewsAnalysisResponse> feedList = newsSummaryService.getNewsByJobGroup(
           jobGroupKeywords, 
           memberId, 
           period, 
           lastPublishedAt,   
            lastSummaryId,     
           limit);

      if (feedList == null || feedList.isEmpty()) {
            log.info("{}기간  피드 데이터 없음 - 자동 수집 시작",period);
            
            int analyzed = collectAndAnalyzeNews(jobGroupKeywords, memberId,limit);
            log.info("자동 수집 완료 - {}건 분석됨", analyzed);
            
            // 4. 수집 후 다시 조회
            feedList = newsSummaryService.getNewsByJobGroup(
                jobGroupKeywords, 
                memberId, 
                period, 
                lastPublishedAt,   
                lastSummaryId,     
                limit
            );
        }
        
        log.info("뉴스 피드 조회 완료 - {}건", feedList != null ? feedList.size() : 0);
        return feedList != null ? feedList : new ArrayList<>();
  }

  /**
     * ✅ 사용자 직군에 맞는 키워드 생성
     */
    private List<String> generateJobGroupKeywords(int memberId) {
        List<String> keywords = new ArrayList<>();

        //1. 회원 정보 조회
            Member member = memberDao.findById(memberId);
            if (member != null && member.getJobGroup() != null) {
                String jobGroup = member.getJobGroup();
                String role = member.getJobRole();
        // 2. 직군별 특화 키워드 추가
              List<String> jobKeywords = JOB_GROUP_KEYWORDS.get(jobGroup);

              if (jobKeywords != null) {
                  keywords.addAll(jobKeywords);
                  log.info("직군 '{}' 키워드 {}개 추가", jobGroup, jobKeywords.size());
              } else {
                  log.warn("매핑되지 않은 직군: {}", jobGroup);
                  // 기본 키워드 추가
                  keywords.addAll(Arrays.asList(jobGroup + " 채용", jobGroup + " 모집"));
              }
              //세부 직무 추가
              if (role != null && !role.trim().isEmpty()) {
              keywords.add(role);
              keywords.add(role + " 채용");
              log.info("세부 직무 '{}' 추가", role);
            }

            }
        

        // 3. 키워드가 없으면 개발 직군 기본값 사용
        if (keywords.isEmpty()) {
            keywords.addAll(JOB_GROUP_KEYWORDS.get("개발"));
        }

        return keywords.stream().distinct().collect(Collectors.toList());
    }



  /**
   * 단일 뉴스 분석 및 저장
   */
  @Transactional
  public NewsAnalysisResponse analyzeAndSaveNews(NewsAnalysisRequest request) throws Exception{
    //중복 체크
    if(newsSummaryService.existsByUrl(request.getSourceUrl())){
      log.warn("이미 존재하는 뉴스입니다.:{}",request.getSourceUrl());
      return null;
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
      entity.setPublishedAt(LocalDateTime.now());
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
  public int collectAndAnalyzeNews(List<String> keywords, Integer memberId, int limit) throws Exception {
    log.info("뉴스 수집 및 분석 통합 처리 시작 - keywords: {}, memberId: {}", keywords, memberId);
    

    // 1. 뉴스 수집 (NewsCollectorService)
     List<NewsAnalysisRequest> collectedNews = newsCollectorService.collectNews(keywords, memberId, limit);
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
        // if (newsSummaryService.existsByUrl(newsRequest.getSourceUrl())) {
        //   log.debug("이미 저장된 뉴스: {}", newsRequest.getSourceUrl());
        //   duplicateCount++;
        //   continue;
        // }
        
        // 2-2. AI 분석 실행
        log.debug("AI 분석 시작: {}", newsRequest.getTitle());
        NewsAnalysisResult analysisResult = newsAIService.analyzeNews(newsRequest);
        
        // 2-3. 엔티티 생성 및 AI 분석 결과 매핑
        NewsSummary entity = new NewsSummary();
        entity.setMemberId(newsRequest.getMemberId());
        entity.setTitle(newsRequest.getTitle());
        entity.setSourceName(newsRequest.getSourceName());
        entity.setSourceUrl(newsRequest.getSourceUrl());
        entity.setPublishedAt(
            newsRequest.getPublishedAt() != null 
                ? newsRequest.getPublishedAt()
                : LocalDateTime.now()
        );
        // 2-4. AI 분석 결과를 JSON으로 저장
        entity.setSummaryText(analysisResult.getFinalSummary()); // AI 요약문
        
        entity.setDetailSummary(analysisResult.getAnalysis().getDetailSummary());
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
            int analyzed = collectAndAnalyzeNews(null, memberId,limit);  // ✅ 통합 메소드 호출
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
  public int searchNews(List<String> keywords, Integer memberId,int limit) throws Exception {
      log.info("뉴스 검색 시작 - keywords: {}, memberId: {}", keywords, memberId);
      
      // ✅ 통합 메소드 호출
      int analyzed = collectAndAnalyzeNews(keywords, memberId,limit);
      
      log.info("뉴스 검색 완료 - {}건 분석됨", analyzed);
      return analyzed;
  }

  /**
   * 키워드 리스트로 네이버 뉴스 검색 (단순 검색, 저장 안함)
   * 
   * @param keywords 검색할 키워드 리스트
   * @param limitPerKeyword 각 키워드당 가져올 기사 수
   * @return 기사 제목, URL, 설명이 담긴 리스트
   */
  public List<Map<String, String>> searchNewsByKeywords(List<String> keywords, int limitPerKeyword) {
      return newsCollectorService.searchNewsByKeywords(keywords, limitPerKeyword);
  }

  /**
   * Response 생성 헬퍼 메서드
   */
  private NewsAnalysisResponse createResponse(NewsSummary saved, NewsAnalysisResult result) {
      NewsAnalysisResponse response = new NewsAnalysisResponse();

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
        response.setSentimentScores(analysis.getSentimentScores()); 
        response.setTrustScore(analysis.getTrustScore());
        response.setBiasDetected(analysis.getBiasDetected());
        response.setBiasType(analysis.getBiasType());
        response.setCategory(analysis.getCategory());

        // 키워드
        
        response.setKeywords(result.getKeywords());

        response.setCreatedAt(saved.getCreatedAt());

        return response;
    }

// ✅ 직군별 키워드 매핑
    private static final Map<String, List<String>> JOB_GROUP_KEYWORDS = Map.of(
            "개발", Arrays.asList(
                    "개발자 채용", "백엔드 채용", "프론트엔드 채용", "풀스택 개발자",
                    "소프트웨어 엔지니어", "프로그래머", "코딩", "Java", "Python", "React", "Spring",
                  "IT", "기술", "소프트웨어", "엔지니어링", "개발", "프로그래밍", "개발자"),

            "AI/데이터", Arrays.asList(
                    "데이터 사이언티스트", "데이터 엔지니어", "AI 개발자", "머신러닝 엔지니어",
                    "빅데이터", "데이터 분석가", "인공지능", "딥러닝", "ML", "IT", "기술", "데이터", "AI", "분석"),

            "디자인", Arrays.asList(
                    "UI 디자이너", "UX 디자이너", "웹디자인", "그래픽 디자이너", "프로덕트 디자이너",
                    "디자인 채용", "포토샵", "피그마", "일러스트", "브랜딩",
                 "디자인", "디자이너", "크리에이티브" ),

            "기획", Arrays.asList(
                    "기획자 채용", "서비스 기획", "상품 기획", "사업 기획", "전략 기획",
                    "기획 업무", "기획 직무", "비즈니스 분석"),

            "PM", Arrays.asList(
                    "프로덕트 매니저", "프로젝트 매니저", "PM 채용", "PO", "프로덕트 오너",
                    "애자일", "스크럼", "프로젝트 관리",
                  "기획", "기획자","PM", "매니저", "관리"),

            "마케팅", Arrays.asList(
                    "마케팅 매니저", "디지털 마케팅", "퍼포먼스 마케팅", "콘텐츠 마케팅",
                    "브랜드 마케팅", "마케팅 기획", "광고", "SNS 마케팅", "SEO",
                  "마케팅", "마케터"),

            "영업", Arrays.asList(
                    "영업 대표", "세일즈", "비즈니스 개발", "B2B 영업", "고객 관리",
                    "영업 기획", "계정 관리", "Sales",
                  "영업", "세일즈"),

            "경영", Arrays.asList( // 경영으로 추정
                    "경영", "경영관리", "경영기획", "전략", "경영지원", "임원", "관리자","경영", "관리", "임원"),

            "교육", Arrays.asList(
                    "교육 기획", "강사", "교육 콘텐츠", "이러닝", "교육 프로그램", "연수", "교육생","교육", "강의", "트레이닝", "부트캠프", "양성", "과정"),

            "기타", Arrays.asList(
                    "인사", "총무", "재무", "회계", "법무", "운영", "고객서비스", "품질관리","지원", "관리"));



  

}
