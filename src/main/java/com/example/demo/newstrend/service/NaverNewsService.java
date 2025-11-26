package com.example.demo.newstrend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;

/**
 * 네이버 뉴스 API 서비스
 * - 네이버 검색 API를 통한 뉴스 검색
 */
@Service
@Slf4j
public class NaverNewsService {
    
    private final WebClient webClient;
    
    @Value("${naver.api.client-id}")
    private String clientId;
    
    @Value("${naver.api.client-secret}")
    private String clientSecret;
    
    public NaverNewsService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl("https://openapi.naver.com/v1/search")
            .build();
    }
    
    /**
     * 네이버 뉴스 검색
     * @param keyword 검색 키워드
     * @return JSON 형식의 뉴스 검색 결과
     */
    public String getNaverNews(String keyword) {
        log.info("네이버 뉴스 검색 - 키워드: {}", keyword);
        
        String result = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/news.json")
                .queryParam("query", keyword)
                .queryParam("display", 6)
                .queryParam("sort", "date")
                .build()
            )
            .header("X-Naver-Client-Id", clientId)
            .header("X-Naver-Client-Secret", clientSecret)
            .retrieve()
            .bodyToMono(String.class)
            .block();
        
        log.debug("네이버 뉴스 검색 완료 - 응답 길이: {} bytes", 
            result != null ? result.length() : 0);
        
        return result;
    }
}
