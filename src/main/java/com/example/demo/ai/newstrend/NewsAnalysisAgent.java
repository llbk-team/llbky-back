package com.example.demo.ai.newstrend;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.newstrend.dto.response.NewsAnalysisResponse;
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
     * @return 요약 및 분석 결과 (감정, 신뢰도, 편향, 카테고리, 키워드)
     * @throws Exception JSON 파싱 실패 또는 AI 호출 실패 시
     */
    public NewsSummaryResponse analyzeNews(String title, String content) throws Exception {
        log.info("뉴스 분석 시작 - 제목: {}", title);
        
        // Bean to JSON 변환기
        BeanOutputConverter<NewsSummaryResponse > converter = 
            new BeanOutputConverter<>(NewsSummaryResponse .class);
        
        String format = converter.getFormat();
        
        String systemPrompt = """
            당신은 취업·기술 분야 전문 뉴스 분석가입니다.
            뉴스를 객관적으로 분석하고 요약해야 합니다.
            
            ======================================== 
            필수 생성 규칙
            ======================================== 
            1. summary (String, 필수)
               - 뉴스의 핵심 내용을 3문장으로 요약
               - 각 문장은 마침표(.)로 끝남
               - 문장 간 구분은 공백 1칸
               - null 불가
               
            2. detailSummary (String, 필수)
               - 뉴스의 상세 내용을 5~7문장으로 요약
               - 각 문장은 마침표(.)로 끝남
               - 배경, 현황, 전망을 포함
               - null 불가
               
            3. sentiment (String, 필수)
               - 반드시 다음 중 하나: "positive", "neutral", "negative"
               - 소문자로만 작성
               - null 불가
               
            4. trustScore (Integer, 필수)
               - 0~100 사이의 정수
               - 객관성, 사실 확인 가능성, 출처 명확성 기준
               - null 불가
               
                           
            5. category (String, 필수)
               - 반드시 다음 중 하나: "IT", "경제", "사회", "정치", "기타"
               - null 불가
            
            ======================================== 
            신뢰도 점수 산출 기준
            ======================================== 
            - 90~100점: 공식 발표, 1차 출처, 구체적 수치
            - 70~89점: 복수 출처 확인, 전문가 의견 포함
            - 50~69점: 단일 출처, 일부 추측 포함
            - 30~49점: 출처 불명확, 과장 표현 있음
            - 0~29점: 루머성, 검증 불가능
            
            ======================================== 
            JSON 형식 예시
            ======================================== 
            {
                "summary": "삼성전자가 3분기 영업이익 10조원을 기록했다. 반도체 부문 회복이 주요 원인으로 분석된다. 4분기에도 긍정적 전망이 이어질 것으로 보인다.",
                "detailSummary": "삼성전자가 2024년 3분기 실적을 발표했다. 영업이익은 전년 동기 대비 30퍼센트 증가한 10조원을 기록했다. 메모리 반도체 가격 상승과 수요 회복이 주요 원인이다. HBM3 등 AI 반도체 수요가 급증하면서 실적 개선을 이끌었다. 파운드리 부문도 손익분기점을 넘어섰다. 4분기에는 계절적 비수기 영향이 있을 전망이다. 하지만 AI 반도체 수요 지속으로 양호한 실적이 예상된다.",
                "sentiment": "positive",
                "trustScore": 85,
                "biasDetected": false,
                "biasType": null,
                "category": "IT",
                "keywords": ["삼성전자", "3분기 실적", "영업이익 10조원", "반도체", "HBM3", "AI 반도체", "메모리", "파운드리"]
            }
            
            ======================================== 
            중요 주의사항
            ======================================== 
            - null 값은 biasType에만 허용됨 (biasDetected=false일 때)
            - 모든 필수 필드는 반드시 값이 있어야 함
            - sentiment, category는 정해진 값 외 사용 불가
            - trustScore는 반드시 0~100 사이 정수
            - JSON 형식 외 다른 텍스트 출력 금지
            
            출력 형식:
            %s
        """.formatted(format);
        
        String userPrompt = """
            다음 뉴스를 분석하세요:
            
            제목: %s
            
            본문:
            %s
            
            위 규칙을 엄격히 준수하여 JSON으로만 응답하세요.
        """.formatted(title, content);
        
        log.debug("AI 호출 시작 - systemPrompt 길이: {}, userPrompt 길이: {}", 
            systemPrompt.length(), userPrompt.length());
        
        String jsonResponse = chatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .options(ChatOptions.builder()
                .temperature(0.3)
                .maxTokens(3000)
                .build())
            .call()
            .content();
        
        log.debug("AI 응답 수신 - 길이: {} bytes", jsonResponse.length());
        log.debug("AI 응답 내용:\n{}", jsonResponse);
        
        NewsSummaryResponse  result = converter.convert(jsonResponse);
        
        log.info("뉴스 분석 완료 - 감정: {}, 신뢰도: {}, 편향: {}, 카테고리: {}, 키워드 수: {}", 
            result.getSentiment(), 
            result.getTrustScore(), 
            result.getBiasDetected(), 
            result.getCategory());
        
        return result;
    }
    
}