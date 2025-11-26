package com.example.demo.ai.newstrend;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.newstrend.dto.response.NewsSummaryResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 뉴스 요약 및 분석 Agent
 * - 뉴스 제목과 본문을 입력받아 3줄 요약 생성
 * - 감정 분석 (긍정/중립/부정)
 * - 신뢰도 점수 산출
 * - 편향 감지 및 분류
 * - 카테고리 자동 분류
 */
@Component
@Slf4j
public class NewsAnalysisAgent {
    
    private final ChatClient chatClient;
    
    public NewsAnalysisAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    /**
     * 뉴스 분석 및 요약
     * @param title 뉴스 제목
     * @param content 뉴스 본문
     * @return 요약 및 분석 결과 (감정, 신뢰도, 편향, 카테고리)
     * @throws Exception JSON 파싱 실패 또는 AI 호출 실패 시
     */
    public NewsSummaryResponse analyzeNews(String title, String content) throws Exception {
        log.info("뉴스 분석 시작 - 제목: {}", title);
        
        // Bean to JSON 변환기
        BeanOutputConverter<NewsSummaryResponse> converter = 
            new BeanOutputConverter<>(NewsSummaryResponse.class);
        
        String format = converter.getFormat();
        
        String systemPrompt = """
            당신은 취업·기술 분야 전문 뉴스 분석가입니다.
            뉴스를 객관적으로 분석하고 3줄로 요약해야 합니다.
            
            분석 규칙:
            1. summary: 핵심만 담아 3문장으로 요약 (각 문장은 마침표로 끝남)
            2. sentiment: 긍정(positive)/중립(neutral)/부정(negative) 중 하나
            3. trustScore: 0~100 사이 정수 (객관성, 사실 확인 가능성 기준)
            4. biasDetected: 편향 감지 여부 (true/false)
            5. biasType: 편향 유형 (없으면 null)
            6. category: IT/경제/사회/정치/기타 중 하나
            
            편향 감지 기준:
            - 감정적 표현 과다
            - 특정 입장 일방적 옹호
            - 과장된 표현 사용
            - 출처 불명확한 주장
            
            JSON 형식으로만 응답:
            %s
        """.formatted(format);
        
        String userPrompt = """
            다음 뉴스를 분석하세요:
            
            제목: %s
            
            본문:
            %s
        """.formatted(title, content);
        
        log.debug("AI 호출 시작 - systemPrompt 길이: {}, userPrompt 길이: {}", 
            systemPrompt.length(), userPrompt.length());
        
        String jsonResponse = chatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .options(ChatOptions.builder()
                .temperature(0.3)
                .maxTokens(500)
                .build())
            .call()
            .content();
        
        log.debug("AI 응답 수신 - 길이: {} bytes", jsonResponse.length());
        
        NewsSummaryResponse result = converter.convert(jsonResponse);
        
        log.info("뉴스 분석 완료 - 감정: {}, 신뢰도: {}, 편향: {}, 카테고리: {}", 
            result.getSentiment(), 
            result.getTrustScore(), 
            result.getBiasDetected(), 
            result.getCategory());
        
        return result;
    }
}
