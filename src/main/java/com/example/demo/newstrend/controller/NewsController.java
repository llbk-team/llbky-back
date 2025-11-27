package com.example.demo.newstrend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.newstrend.dto.request.NewsAnalysisRequest;
import com.example.demo.newstrend.dto.response.NewsAnalysisResponse;
import com.example.demo.newstrend.service.NaverNewsService;
import com.example.demo.newstrend.service.NewsAIService;
import com.example.demo.newstrend.service.NewsCollectorService;
import com.example.demo.newstrend.service.NewsSummaryService;

import lombok.extern.slf4j.Slf4j;

/**
 * 뉴스 트렌드 컨트롤러
 * - 뉴스 분석  
 * - 저장된 뉴스 조회  
 */
@RestController
@RequestMapping("/news")
@Slf4j
public class NewsController {
    
  @Autowired
  private  NewsAIService newsAIService;
  @Autowired
  private  NewsSummaryService newsSummaryService;
  @Autowired
  private  NaverNewsService naverNewsService;
  // @Autowired
  // private  NewsService newsApiService;
  @Autowired
  private  NewsCollectorService newsCollectorService;
    
    /**
     * 네이버 뉴스 검색
     * 
     * GET /news/search?keyword=AI
     * 
     * @param keyword 검색 키워드
     * @return 네이버 뉴스 검색 결과 (JSON)
     */
    @GetMapping("/search")
    public String searchNews(@RequestParam String keyword) {
        log.info("네이버 뉴스 검색 요청 - 키워드: {}", keyword);
        
        String naverResult = naverNewsService.getNaverNews(keyword);
        
        log.info("네이버 뉴스 검색 완료 - 키워드: {}", keyword);
        
        return naverResult;
    }
    
    /**
     * 단일 뉴스 분석 및 저장
     * 
     * POST /news/analyze
     * 
     * 요청 예시:
     * {
     *   "title": "AI 기술 발전으로 일자리 시장 변화",
     *   "content": "인공지능 기술의 발전으로...",
     *   "sourceUrl": "https://example.com/news/123",
     *   "sourceName": "테크뉴스"
     * }
     * 
     * @param request 뉴스 분석 요청 (제목, 본문, URL, 출처)
     * @return 분석된 뉴스 응답 (요약, 감정, 신뢰도, 키워드 등)
     * @throws Exception AI 호출 실패 또는 JSON 변환 실패 시
     */
    @PostMapping("/analyze")
    public NewsAnalysisResponse analyzeNews(@RequestBody NewsAnalysisRequest request) 
            throws Exception {
        log.info("뉴스 분석 요청 - 제목: {}", request.getTitle());
        
        NewsAnalysisResponse response = newsAIService.analyzeAndSaveNews(request);
        
        log.info("뉴스 분석 완료 - summaryId: {}, 감정: {}, 신뢰도: {}", 
            response.getSummaryId(), 
            response.getSentiment(), 
            response.getTrustScore());
        
        return response;
    }
    
    /**
     * 저장된 뉴스 조회 (회원별)
     * 
     * GET /news/member/{memberId}?limit=10
     * 
     * @param memberId 회원 ID
     * @param limit 조회 개수 (기본값: 10)
     * @return 뉴스 분석 결과 리스트
     * @throws com.fasterxml.jackson.core.JsonProcessingException JSON 파싱 실패 시
     */
    @GetMapping("/member/{memberId}")
    public List<NewsAnalysisResponse> getMemberNews(
            @PathVariable int memberId,
            @RequestParam(defaultValue = "10") int limit) 
            throws com.fasterxml.jackson.core.JsonProcessingException {
        
        log.info("회원별 뉴스 조회 요청 - memberId: {}, limit: {}", memberId, limit);
        
        List<NewsAnalysisResponse> news = newsSummaryService.getLatestNewsByMember(
            memberId, 
            limit
        );
        
        log.info("회원별 뉴스 조회 완료 - 조회된 뉴스 수: {}", news.size());
        
        return news;
    }
    
    /**
     * 뉴스 자동 수집 및 분석 (수동 실행)
     * 
     * POST /news/collect
     * 
     * 요청 예시:
     * {
     *   "keywords": ["AI 개발자", "백엔드 채용", "프론트엔드"],
     *   "memberId": 1
     * }
     * 
     * @param request 수집 요청 (키워드 리스트, 회원 ID)
     * @return 수집 결과 메시지
     */
    @PostMapping("/collect")
    public String collectNews(@RequestBody CollectNewsRequest request) throws Exception {
        log.info("뉴스 수집 요청 - keywords: {}, memberId: {}", 
            request.getKeywords(), 
            request.getMemberId());
        
        int analyzed = newsCollectorService.collectAndAnalyzeNews(
            request.getKeywords(), 
            request.getMemberId()
        );
        
        String message = String.format("뉴스 수집 완료: %d건 분석됨", analyzed);
        log.info(message);
        
        return message;
    }
    
    /**
     * 뉴스 수집 요청 DTO
     */
    public static class CollectNewsRequest {
        private List<String> keywords;
        private Integer memberId;
        
        public List<String> getKeywords() {
            return keywords;
        }
        
        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }
        
        public Integer getMemberId() {
            return memberId;
        }
        
        public void setMemberId(Integer memberId) {
            this.memberId = memberId;
        }
    }
}
