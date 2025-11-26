package com.example.demo.newstrend.service;

import com.example.demo.newstrend.dto.request.NewsAnalysisRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 뉴스 자동 수집 및 분석 서비스
 * - 매일 정해진 시간에 자동으로 뉴스 수집
 * - 네이버 뉴스 API와 NewsAPI.org에서 뉴스 수집
 * - AI 분석 후 데이터베이스 저장
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NewsCollectorService {
    
    private final NaverNewsService naverNewsService;
    // private final NewsService newsService;
    private final NewsAIService newsAIService;
    private final NewsSummaryService newsSummaryService;
    
    // 기본 검색 키워드 목록
    private static final List<String> DEFAULT_KEYWORDS = Arrays.asList(
        "AI 개발자",
        "백엔드 채용",
        "프론트엔드 개발자",
        "데이터 엔지니어",
        "클라우드 엔지니어",
        "풀스택 개발자",
        "DevOps",
        "취업 트렌드"
    );
    
    /**
     * 매일 오전 9시에 자동으로 뉴스 수집 및 분석
     * Cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void scheduledNewsCollection() {
        log.info("=== 뉴스 자동 수집 시작 (Scheduled) ===");
        try {
            int analyzed = collectAndAnalyzeNews(DEFAULT_KEYWORDS, 1);
            log.info("=== 뉴스 자동 수집 완료: {}건 분석 ===", analyzed);
        } catch (Exception e) {
            log.error("뉴스 자동 수집 중 오류 발생", e);
        }
    }
    
    /**
     * 키워드 기반 뉴스 수집 및 분석
     * @param keywords 검색 키워드 목록
     * @param memberId 회원 ID (null이면 1로 설정)
     * @return 분석된 뉴스 개수
     */
    @Transactional
    public int collectAndAnalyzeNews(List<String> keywords, Integer memberId) throws Exception {
        log.info("뉴스 수집 시작 - 키워드: {}, memberId: {}", keywords, memberId);
        
        if (keywords == null || keywords.isEmpty()) {
            keywords = DEFAULT_KEYWORDS;
        }
        
        int totalAnalyzed = 0;
        int duplicateCount = 0;
        int errorCount = 0;
        
        // 중복 제거를 위한 Map (sourceUrl을 키로 사용)
        Map<String, NewsAnalysisRequest> uniqueNewsMap = new LinkedHashMap<>();
        
        // 1. 각 키워드별로 뉴스 수집
        for (String keyword : keywords) {
            log.debug("키워드 '{}' 검색 시작", keyword);
            
            try {
                // 1-1. 네이버 뉴스 수집
                String naverResponse = naverNewsService.getNaverNews(keyword);
                List<NewsAnalysisRequest> naverNews = parseNaverNews(naverResponse);
                log.debug("네이버 뉴스 {}건 수집: {}", naverNews.size(), keyword);
                
                // Map에 추가 (중복 제거)
                for (NewsAnalysisRequest news : naverNews) {
                    if (!uniqueNewsMap.containsKey(news.getSourceUrl())) {
                        uniqueNewsMap.put(news.getSourceUrl(), news);
                    }
                }
                
                // API 호출 제한을 위한 대기
                Thread.sleep(500);
                
            } catch (Exception e) {
                log.warn("네이버 뉴스 수집 실패: {}", keyword, e);
                errorCount++;
            }
            
            // try {
            //     // 1-2. NewsAPI 뉴스 수집
            //     String newsApiResponse = newsService.getHeadlines(keyword);
            //     List<NewsAnalysisRequest> newsApiNews = parseNewsApi(newsApiResponse);
            //     log.debug("NewsAPI 뉴스 {}건 수집: {}", newsApiNews.size(), keyword);
                
            //     // Map에 추가 (중복 제거)
            //     for (NewsAnalysisRequest news : newsApiNews) {
            //         if (!uniqueNewsMap.containsKey(news.getSourceUrl())) {
            //             uniqueNewsMap.put(news.getSourceUrl(), news);
            //         }
            //     }
                
            //     // API 호출 제한을 위한 대기
            //     Thread.sleep(500);
                
            // } catch (Exception e) {
            //     log.warn("NewsAPI 뉴스 수집 실패: {}", keyword, e);
            //     errorCount++;
            // }
        }
        
        log.info("총 {}건의 고유 뉴스 수집 완료", uniqueNewsMap.size());
        
        // 2. 수집된 뉴스 분석 및 저장
        for (NewsAnalysisRequest newsRequest : uniqueNewsMap.values()) {
            try {
                // 2-1. URL 중복 체크
                if (newsSummaryService.existsByUrl(newsRequest.getSourceUrl())) {
                    log.debug("이미 저장된 뉴스: {}", newsRequest.getSourceUrl());
                    duplicateCount++;
                    continue;
                }
                
                // 2-2. memberId 설정
                newsRequest.setMemberId(memberId != null ? memberId : 1);
                
                // 2-3. AI 분석 및 저장
                newsAIService.analyzeAndSaveNews(newsRequest);
                totalAnalyzed++;
                
                log.debug("뉴스 분석 완료: {}", newsRequest.getTitle());
                
                // API 호출 제한을 위한 대기
                Thread.sleep(500);
                
            } catch (Exception e) {
                log.error("뉴스 분석 실패: {}", newsRequest.getTitle(), e);
                errorCount++;
            }
        }
        
        log.info("뉴스 수집 완료 - 총 수집: {}건, 분석: {}건, 중복: {}건, 오류: {}건", 
                uniqueNewsMap.size(), totalAnalyzed, duplicateCount, errorCount);
        
        return totalAnalyzed;
    }
    
    /**
     * 네이버 뉴스 API 응답 파싱
     * @param jsonResponse 네이버 API JSON 응답
     * @return 뉴스 요청 목록
     */
    private List<NewsAnalysisRequest> parseNaverNews(String jsonResponse) {
        List<NewsAnalysisRequest> newsList = new ArrayList<>();
        
        try {
            JSONObject json = new JSONObject(jsonResponse);
            JSONArray items = json.getJSONArray("items");
            
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                
                NewsAnalysisRequest news = new NewsAnalysisRequest();
                news.setTitle(cleanHtml(item.optString("title", "")));
                news.setContent(cleanHtml(item.optString("description", "")));
                news.setSourceUrl(item.optString("link", ""));
                news.setSourceName("네이버뉴스");
                
                // 유효한 뉴스만 추가
                if (news.getSourceUrl() != null && !news.getSourceUrl().isEmpty()) {
                    newsList.add(news);
                }
            }
            
        } catch (Exception e) {
            log.error("네이버 뉴스 파싱 오류", e);
        }
        
        return newsList;
    }
    
    /**
     * NewsAPI.org API 응답 파싱
     * @param jsonResponse NewsAPI JSON 응답
     * @return 뉴스 요청 목록
     */
    private List<NewsAnalysisRequest> parseNewsApi(String jsonResponse) {
        List<NewsAnalysisRequest> newsList = new ArrayList<>();
        
        try {
            JSONObject json = new JSONObject(jsonResponse);
            
            // status 체크
            String status = json.optString("status", "");
            if (!"ok".equals(status)) {
                log.warn("NewsAPI 응답 상태 오류: {}", status);
                return newsList;
            }
            
            JSONArray articles = json.optJSONArray("articles");
            if (articles == null) {
                return newsList;
            }
            
            for (int i = 0; i < articles.length(); i++) {
                JSONObject article = articles.getJSONObject(i);
                
                NewsAnalysisRequest news = new NewsAnalysisRequest();
                news.setTitle(article.optString("title", ""));
                news.setContent(article.optString("description", ""));
                news.setSourceUrl(article.optString("url", ""));
                
                // source 객체에서 이름 추출
                JSONObject source = article.optJSONObject("source");
                if (source != null) {
                    news.setSourceName(source.optString("name", "NewsAPI"));
                } else {
                    news.setSourceName("NewsAPI");
                }
                
                // 유효한 뉴스만 추가
                if (news.getSourceUrl() != null && !news.getSourceUrl().isEmpty()) {
                    newsList.add(news);
                }
            }
            
        } catch (Exception e) {
            log.error("NewsAPI 파싱 오류", e);
        }
        
        return newsList;
    }
    
    /**
     * HTML 태그 제거 및 특수 문자 변환
     * @param text HTML이 포함된 텍스트
     * @return 정제된 텍스트
     */
    private String cleanHtml(String text) {
        if (text == null) {
            return "";
        }
        
        return text
            .replaceAll("<.*?>", "")           // HTML 태그 제거
            .replaceAll("&quot;", "\"")        // &quot; → "
            .replaceAll("&amp;", "&")          // &amp; → &
            .replaceAll("&lt;", "<")           // &lt; → <
            .replaceAll("&gt;", ">")           // &gt; → >
            .replaceAll("&nbsp;", " ")         // &nbsp; → 공백
            .replaceAll("\\s+", " ")           // 연속된 공백 → 하나의 공백
            .trim();
    }
}
