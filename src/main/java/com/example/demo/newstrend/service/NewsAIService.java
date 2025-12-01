package com.example.demo.newstrend.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.ai.newstrend.BiasNeutralizationAgent;
import com.example.demo.ai.newstrend.KeywordExtractionAgent;
import com.example.demo.ai.newstrend.NewsAnalysisAgent;
import com.example.demo.newstrend.dao.NewsSummaryDao;
import com.example.demo.newstrend.dto.request.NewsAnalysisRequest;
import com.example.demo.newstrend.dto.response.NewsAnalysisResponse;
import com.example.demo.newstrend.dto.response.NewsAnalysisResult;
import com.example.demo.newstrend.dto.response.NewsKeywordResponse;
import com.example.demo.newstrend.dto.response.NewsSummaryResponse;
import com.example.demo.newstrend.entity.NewsSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * AI 통합 서비스
 * - AI Agent들을 순서대로 실행하여 뉴스 분석
 * - 기사 원문 저장, 요약문 저장까지 담당
 * - 흐름: Controller → Service (AI Agent로 데이터 가공) → 저장 → DB
 */
@Service
@Slf4j
public class NewsAIService {

    // Optional WebClient builder (example exception constructor)
    
    private WebClient webClient;

    // AI Agents (field injection as requested)
    @Autowired
    private NewsAnalysisAgent analysisAgent;

    @Autowired
    private KeywordExtractionAgent keywordAgent;

    @Autowired
    private BiasNeutralizationAgent neutralizationAgent;

    // 데이터베이스 저장
    @Autowired
    private NewsSummaryDao newsSummaryDao;

    @Autowired
    private ObjectMapper objectMapper;

    // 예외 케이스: WebClient.Builder를 직접 주입받는 public 생성자 허용
    public NewsAIService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    /**
     * 뉴스 분석  (AI Agent 활용)
     * @param request 뉴스 분석 요청 (제목, 본문, URL 등)
     * @return 분석된 뉴스 응답 (요약, 감정, 신뢰도, 키워드 등)
     * @throws Exception AI 호출 실패 또는 JSON 변환 실패 시
     */
    @Transactional
    public NewsAnalysisResult analyzeNews(NewsAnalysisRequest request) throws Exception {
        log.info("뉴스 분석 및 저장 시작 - 제목: {}", request.getTitle());

          // 0. 원문 확보 (짧으면 웹 스크래핑)
        String fullContent = request.getContent();
        if (fullContent == null || fullContent.length() < 200) {
            log.info("짧은 content 감지, 웹 스크래핑 시도: {}", request.getSourceUrl());
            String scrapedContent = extractNewsContent(request.getSourceUrl());
            if (scrapedContent != null && scrapedContent.length() > fullContent.length()) {
                fullContent = scrapedContent;
            }
        }

        // 2. AI Agent: 뉴스 분석 (요약, 감정, 신뢰도, 편향, 카테고리)
        log.debug("Step 1: 뉴스 분석 Agent 호출");
        NewsSummaryResponse analysis = analysisAgent.analyzeNews(request.getTitle(),fullContent);
        log.info("뉴스 분석 완료 - 감정: {}, 신뢰도: {}, 편향: {}",
            analysis.getSentiment(), analysis.getTrustScore(), analysis.getBiasDetected());

        // 3. AI Agent: 키워드 추출
        log.debug("Step 2: 키워드 추출 Agent 호출");
        List<NewsKeywordResponse> keywords = keywordAgent.extractKeywords(
            analysis.getSummary()
        );
        log.info("키워드 추출 완료 - 추출된 키워드 수: {}", keywords.size());

        // 4. AI Agent: 편향 감지시 중립화
        String finalSummary = analysis.getSummary();
        if (Boolean.TRUE.equals(analysis.getBiasDetected())) {
            log.debug("Step 3: 편향 감지됨 - 중립화 Agent 호출");
            finalSummary = neutralizationAgent.neutralizeText(fullContent);
            log.info("편향 중립화 완료");
        } else {
            log.debug("Step 3: 편향 미감지 - 중립화 스킵");
        }

       

        // 5. 분석 결과 반환 (저장 안함)
        return NewsAnalysisResult.builder()
            .originalContent(fullContent)
            .analysis(analysis)
            .keywords(keywords)
            .finalSummary(finalSummary)
            .build();
    }
    
    /**
     * Response 생성 헬퍼 메서드
     * @param saved 저장된 뉴스 엔티티
     * @param analysis AI 분석 결과
     * @param keywords 추출된 키워드 리스트
     * @return 뉴스 분석 응답 DTO
     */



 private NewsAnalysisResponse createResponse(
            NewsSummary saved,
            NewsSummaryResponse analysis,
            List<NewsKeywordResponse> keywords) {

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
        response.setSentiment(analysis.getSentiment());
        response.setTrustScore(analysis.getTrustScore());
        response.setBiasDetected(analysis.getBiasDetected());
        response.setBiasType(analysis.getBiasType());
        response.setCategory(analysis.getCategory());

        // 키워드
        response.setKeywords(keywords);

        response.setCreatedAt(saved.getCreatedAt());

        return response;
    }

    /**
     * 웹 스크래핑: 뉴스 본문 추출
     * - 짧은 본문이 들어왔을 때 원문을 가져오기 위해 사용
     * - User-Agent 설정으로 정상적인 HTML 반환 유도
     * @param url 뉴스 URL
     * @return 추출된 본문 (실패시 null)
     */
    private String extractNewsContent(String url) throws IOException {
        try {
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

            // 네이버 뉴스
            if (url.contains("news.naver.com")) {
                Element article = doc.selectFirst("#dic_area, #articeBody, article");
                if (article != null) {
                    return article.text();
                }
            }

            // 일반 뉴스 사이트
            Element article = doc.selectFirst("article, .article-content, .news-content");
            if (article != null) {
                return article.text();
            }

            log.warn("본문 추출 실패: {}", url);
            return null;

        } catch (IOException e) {
            log.error("웹 스크래핑 오류 - URL: {}", url, e);
            throw e;
        }
    }



    // ===== CRUD / 조회 헬퍼 메서드 =====

  

    public List<NewsAnalysisResponse> getLatestNewsByMember(int memberId, int limit) throws com.fasterxml.jackson.core.JsonProcessingException {
        List<NewsSummary> summaries = newsSummaryDao.selectLatestNewsByMemberId(memberId, limit);
        List<NewsAnalysisResponse> responses = new ArrayList<>();
        if (summaries != null) {
            for (NewsSummary s : summaries) {
                responses.add(convertEntityToResponse(s));
            }
        }
        return responses;
    }

    public List<NewsAnalysisResponse> getNewsByMemberAndDate(int memberId, LocalDate date, int limit) throws com.fasterxml.jackson.core.JsonProcessingException {
        List<NewsSummary> summaries = newsSummaryDao.selectNewsByMemberAndDate(memberId, date, limit);
        List<NewsAnalysisResponse> responses = new ArrayList<>();
        if (summaries != null) {
            for (NewsSummary s : summaries) {
                responses.add(convertEntityToResponse(s));
            }
        }
        return responses;
    }


    private NewsAnalysisResponse convertEntityToResponse(NewsSummary summary) throws com.fasterxml.jackson.core.JsonProcessingException {
        NewsSummaryResponse analysisData = objectMapper.readValue(summary.getAnalysisJson(), NewsSummaryResponse.class);
        List<NewsKeywordResponse> keywords = objectMapper.readValue(
            summary.getKeywordsJson(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, NewsKeywordResponse.class)
        );

        NewsAnalysisResponse response = new NewsAnalysisResponse();
        response.setSummaryId(summary.getSummaryId());
        response.setTitle(summary.getTitle());
        response.setSourceName(summary.getSourceName());
        response.setSourceUrl(summary.getSourceUrl());
        response.setPublishedAt(summary.getPublishedAt());
        response.setSummaryText(summary.getSummaryText());
        response.setDetailSummary(summary.getDetailSummary());

        response.setSentiment(analysisData.getSentiment());
        response.setTrustScore(analysisData.getTrustScore());
        response.setBiasDetected(analysisData.getBiasDetected());
        response.setBiasType(analysisData.getBiasType());
        response.setCategory(analysisData.getCategory());

        response.setKeywords(keywords);
        response.setCreatedAt(summary.getCreatedAt());
        return response;
    }


}