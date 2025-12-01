package com.example.demo.newstrend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.newstrend.dao.NewsSummaryDao;
import com.example.demo.newstrend.dto.response.NewsAnalysisResponse;
import com.example.demo.newstrend.dto.response.NewsKeywordResponse;
import com.example.demo.newstrend.dto.response.NewsSummaryResponse;
import com.example.demo.newstrend.entity.NewsSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//중복 체크, 분석 응답 파싱용

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsSummaryService {
    
    private final NewsSummaryDao newsSummaryDao;
    private final ObjectMapper objectMapper;
   
    /**
     * 뉴스 저장
     * @param newsSummary 저장할 뉴스 엔티티
     * @return 저장된 뉴스 (중복일 경우 기존 데이터 반환)
     */
    @Transactional
    public NewsSummary saveNewsSummary(NewsSummary newsSummary) {
        log.info("뉴스 저장 시도 - 제목: {}, URL: {}", 
            newsSummary.getTitle(), 
            newsSummary.getSourceUrl());
        
        // URL 중복 체크
        NewsSummary existing = newsSummaryDao.selectNewsSummaryBySourceUrl(
            newsSummary.getSourceUrl()
        );
        
        if (existing != null) {
            log.info("이미 존재하는 뉴스 URL: {}", newsSummary.getSourceUrl());
            return existing;
        }
        
        newsSummaryDao.insertNewsSummary(newsSummary);
        log.info("뉴스 저장 완료 - summaryId: {}", newsSummary.getSummaryId());
        
        return newsSummary;
    }

    //1.오늘 날짜 뉴스 조회 없으면 자동수집
    @Transactional
    public List<NewsAnalysisResponse> getTodayNewsByMember(int memberId, int limit) throws Exception{
        LocalDate today= LocalDate.now();
        List<NewsSummary> todayNews = newsSummaryDao.selectNewsByMemberAndDate(memberId ,today, limit);
    
         // 2. 오늘 데이터가 있으면 DB값 반환
        if(todayNews!=null && !todayNews.isEmpty()){
           log.info("오늘 뉴스 데이터 존재 -{}건 반환",todayNews.size());
           List<NewsAnalysisResponse> responses= new ArrayList<>();
           for (NewsSummary summary : todayNews) {
                responses.add(convertToResponse(summary));
            }
            return responses;
        }
        //데이터 없을때 빈값만 리턴함
          List<NewsAnalysisResponse> responses= new ArrayList<>();
          return responses;
       
    }

    public boolean existsByUrl(String sourceUrl) {
        return newsSummaryDao.selectNewsSummaryBySourceUrl(sourceUrl) != null;
    }
    

    /**
     * 특정 회원의 최신 뉴스 조회
     * @param memberId 회원 ID
     * @param limit 조회 개수
     * @return 뉴스 분석 결과 리스트
     * @throws com.fasterxml.jackson.core.JsonProcessingException JSON 파싱 실패 시
     */
    public List<NewsAnalysisResponse> getLatestNewsByMember(int memberId, int limit) 
            throws com.fasterxml.jackson.core.JsonProcessingException {
        log.info("회원별 최신 뉴스 조회 - memberId: {}, limit: {}", memberId, limit);
        
        List<NewsSummary> summaries = newsSummaryDao.selectLatestNewsByMemberId(memberId, limit);
        log.info("조회된 뉴스 수: {}", summaries.size());
        
        List<NewsAnalysisResponse> responses = new ArrayList<>();
        for (NewsSummary summary : summaries) {
            NewsAnalysisResponse response = convertToResponse(summary);
            responses.add(response);
        }
        
        return responses;
    }

    /**
     * 특정 회원의 특정 날짜 뉴스 조회
     * @param memberId 회원 ID
     * @param date YYYY-MM-DD 형식의 날짜 문자열
     * @param limit 조회 개수
     * @return 뉴스 분석 결과 리스트
     * @throws com.fasterxml.jackson.core.JsonProcessingException JSON 파싱 실패 시
     */
    public List<NewsAnalysisResponse> getNewsByMemberAndDate(int memberId, LocalDate date, int limit)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        
        log.info("회원별 날짜 뉴스 조회 - memberId: {}, date: {}, limit: {}", memberId, date, limit);

        List<NewsSummary> summaries = newsSummaryDao.selectNewsByMemberAndDate(memberId, date, limit);
        log.info("조회된 뉴스 수: {}", summaries != null ? summaries.size() : 0);

        List<NewsAnalysisResponse> responses = new ArrayList<>();
        if (summaries != null) {
            for (NewsSummary summary : summaries) {
                NewsAnalysisResponse response = convertToResponse(summary);
                responses.add(response);
            }
        }

        return responses;
    }
    //summaryId로 단일 뉴스 조회 (상세보기용)
    
    public NewsAnalysisResponse getNewsBySummaryId(int summaryId) 
                throws com.fasterxml.jackson.core.JsonProcessingException {
            log.info("단일 뉴스 조회 - summaryId: {}", summaryId);
            
            NewsSummary summary = newsSummaryDao.selectNewsSummaryById(summaryId);
            
            if (summary == null) {
                log.warn("존재하지 않는 뉴스 - summaryId: {}", summaryId);
                return null;
            }
            
            NewsAnalysisResponse response = convertToResponse(summary);
            log.info("단일 뉴스 조회 완료 - summaryId: {}", summaryId);
            
            return response;
        }


    
    /**
     * ✅ Entity -> Response 변환 (analysisMap 없이 DTO 직접 변환) - 버그 수정
     * @param summary 뉴스 엔티티
     * @return 뉴스 분석 응답 DTO
     * @throws com.fasterxml.jackson.core.JsonProcessingException JSON 파싱 실패 시
     * 
     */
    private NewsAnalysisResponse convertToResponse(NewsSummary summary) 
            throws com.fasterxml.jackson.core.JsonProcessingException {

        // 기본값 설정
        NewsSummaryResponse defaultAnalysis = new NewsSummaryResponse();
        defaultAnalysis.setSentiment("중립");
        defaultAnalysis.setTrustScore(50);
        defaultAnalysis.setBiasDetected(false);
        defaultAnalysis.setBiasType("없음");
        defaultAnalysis.setCategory("일반");        
        
        log.debug("Response 변환 시작 - summaryId: {}", summary.getSummaryId());
        
        // ✅ 분석 데이터 변환
        NewsSummaryResponse analysisData;
        if(summary.getAnalysisJson() != null && !summary.getAnalysisJson().trim().isEmpty()) {
            analysisData = objectMapper.readValue(summary.getAnalysisJson(), NewsSummaryResponse.class);
        } else {
            analysisData = defaultAnalysis;
        }
       
        // ✅ 키워드 데이터 변환 (별도 변수 사용)
        List<NewsKeywordResponse> keywords = new ArrayList<>();
        if(summary.getKeywordsJson() != null && !summary.getKeywordsJson().trim().isEmpty()) {
            keywords = objectMapper.readValue(
                summary.getKeywordsJson(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, NewsKeywordResponse.class)
            );
        }
        
        // ✅ Response 객체 생성
        NewsAnalysisResponse response = new NewsAnalysisResponse();
        
        // 기본 정보
        response.setSummaryId(summary.getSummaryId());
        response.setTitle(summary.getTitle());
        response.setSourceName(summary.getSourceName());
        response.setSourceUrl(summary.getSourceUrl());
        response.setPublishedAt(summary.getPublishedAt());
        response.setSummaryText(summary.getSummaryText());
        response.setDetailSummary(summary.getDetailSummary());
        
        // ✅ 분석 결과
        response.setSentiment(analysisData.getSentiment());
        response.setTrustScore(analysisData.getTrustScore());
        response.setBiasDetected(analysisData.getBiasDetected());
        response.setBiasType(analysisData.getBiasType());
        response.setCategory(analysisData.getCategory());
        
        // ✅ 키워드
        response.setKeywords(keywords);
        
        response.setCreatedAt(summary.getCreatedAt());
        
        log.debug("Response 변환 완료 - summaryId: {}", summary.getSummaryId());
        return response;
    }
}