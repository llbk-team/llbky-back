package com.example.demo.newstrend.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ai.newstrend.BiasNeutralizationAgent;
import com.example.demo.ai.newstrend.KeywordExtractionAgent;
import com.example.demo.ai.newstrend.NewsAnalysisAgent;
import com.example.demo.newstrend.dto.request.NewsAnalysisRequest;
import com.example.demo.newstrend.dto.response.NewsAnalysisResponse;
import com.example.demo.newstrend.dto.response.NewsKeywordResponse;
import com.example.demo.newstrend.dto.response.NewsSummaryResponse;
import com.example.demo.newstrend.entity.NewsSummary;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 통합 서비스
 * - 뉴스 분석 Agent 통합
 * - 키워드 추출 Agent 통합
 * - 편향 중립화 Agent 통합
 * - 뉴스 저장 처리
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NewsAIService {
    
    private final WebScrapingService webScrapingService;
    private final NewsAnalysisAgent analysisAgent;
    private final KeywordExtractionAgent keywordAgent;
    private final BiasNeutralizationAgent neutralizationAgent;
    private final NewsSummaryService newsSummaryService;
    private final ObjectMapper objectMapper;
    
    /**
     * 뉴스 분석 및 저장
     * @param request 뉴스 분석 요청 (제목, 본문, URL 등)
     * @return 분석된 뉴스 응답 (요약, 감정, 신뢰도, 키워드 등)
     * @throws Exception AI 호출 실패 또는 JSON 변환 실패 시
     */
    @Transactional
    public NewsAnalysisResponse analyzeAndSaveNews(NewsAnalysisRequest request) throws Exception {
        log.info("뉴스 분석 및 저장 시작 - 제목: {}", request.getTitle());

        String fullContent = request.getContent();

        if(fullContent.length()<200){
            log.info("짧은 content 감지, 웹 스크래핑 시도: {}", request.getSourceUrl());
            String scrapedContent= webScrapingService.extractNewsContent(request.getSourceUrl());
            if(scrapedContent!=null && scrapedContent.length()>fullContent.length()){
                fullContent=scrapedContent;
            }
        }
        
        // 1. 뉴스 분석 (요약, 감정, 신뢰도, 편향, 카테고리)
        log.debug("Step 1: 뉴스 분석 Agent 호출");
        NewsSummaryResponse analysis = analysisAgent.analyzeNews(
            request.getTitle(), 
            fullContent  // ✅ 웹 스크래핑된 원문 사용
        );
        log.info("뉴스 분석 완료 - 감정: {}, 신뢰도: {}, 편향: {}", 
            analysis.getSentiment(), 
            analysis.getTrustScore(), 
            analysis.getBiasDetected());
        
        // 2. 키워드 추출
        log.debug("Step 2: 키워드 추출 Agent 호출");
        List<NewsKeywordResponse> keywords = keywordAgent.extractKeywords(
            analysis.getSummary()
        );
        log.info("키워드 추출 완료 - 추출된 키워드 수: {}", keywords.size());
        
        // 3. 편향 감지시 중립화
        String finalSummary = analysis.getSummary();
        if (Boolean.TRUE.equals(analysis.getBiasDetected())) {
            log.debug("Step 3: 편향 감지됨 - 중립화 Agent 호출");
            finalSummary = neutralizationAgent.neutralizeText(analysis.getSummary());
            log.info("편향 중립화 완료");
        } else {
            log.debug("Step 3: 편향 미감지 - 중립화 스킵");
        }
        
        // 4. Entity 생성 및 저장
        log.debug("Step 4: 뉴스 엔티티 생성 및 저장");
        NewsSummary entity = new NewsSummary();
        entity.setMemberId(1); // TODO: SecurityContext에서 가져오거나 NewsAnalysisRequest에서 받기
        entity.setTitle(request.getTitle());
        entity.setSourceName(request.getSourceName());
        entity.setSourceUrl(request.getSourceUrl());
        entity.setPublishedAt(LocalDate.now());
        entity.setSummaryText(finalSummary);
        entity.setDetailSummary(analysis.getSummary());
        
        // ✅ JSONB 필드 설정 - DTO 직접 저장 (analysisMap 사용 안함)
        entity.setAnalysisJson(objectMapper.writeValueAsString(analysis));
        entity.setKeywordsJson(objectMapper.writeValueAsString(keywords));
        
        // 저장
        NewsSummary saved = newsSummaryService.saveNewsSummary(entity);
        log.info("뉴스 저장 완료 - summaryId: {}", saved.getSummaryId());
        
        // 5. Response 변환 - 간단하게 직접 설정
        log.debug("Step 5: Response 객체 생성");
        NewsAnalysisResponse response = createResponse(saved, analysis, keywords);
        
        log.info("뉴스 분석 및 저장 완료 - summaryId: {}", response.getSummaryId());
        return response;
    }
    
    /**
     * ✅ 간단한 Response 생성 (analysisMap 없이 직접 설정)
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
        
        // ✅ AI 분석 결과 - NewsSummaryResponse에서 직접 가져오기
        response.setSentiment(analysis.getSentiment());
        response.setTrustScore(analysis.getTrustScore());
        response.setBiasDetected(analysis.getBiasDetected());
        response.setBiasType(analysis.getBiasType());
        response.setCategory(analysis.getCategory());
        
        // ✅ 키워드 - NewsKeywordResponse 리스트 그대로 사용
        response.setKeywords(keywords);
        
        response.setCreatedAt(saved.getCreatedAt());
        
        return response;
    }
}