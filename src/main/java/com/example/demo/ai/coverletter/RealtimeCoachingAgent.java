package com.example.demo.ai.coverletter;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.coverletter.dto.request.CoverLetterCoachRequest;
import com.example.demo.coverletter.dto.response.CoverLetterCoachResponse;

@Component
public class RealtimeCoachingAgent {

    // ChatClient
    private ChatClient chatClient;

    public RealtimeCoachingAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // 실시간 코칭
    public CoverLetterCoachResponse execute(CoverLetterCoachRequest request) {

        // 1. Bean 객체 -> JSON 출력 변환기 생성
        BeanOutputConverter<CoverLetterCoachResponse> converter = new BeanOutputConverter<>(CoverLetterCoachResponse.class);

        String format = converter.getFormat();

        // 2. 요청된 section만 추출
        String section = null;
        switch (request.getSection()) {
            case "supportMotive":
                section = "지원동기";
                break;
            case "growthExperience":
                section = "성장경험";
                break;
            case "jobCapability":
                section = "직무역량";
                break;
            case "futurePlan":
                section = "입사 후 포부";
                break;
            default:
                throw new IllegalArgumentException("Invalid section");
        }

        // 3. 프롬프트 구성
        String prompt = """
            당신은 취업 준비생의 자기소개서를 첨삭해주는 취업 컨설턴트입니다.
            사용자가 방금 작성한 "%s" 항목을 분석하고 피드백을 제공하세요.
            
            [사용자 작성 내용]
            %s

            반드시 아래 JSON 형식을 그대로 채워서 출력하세요:
            %s
           
        """.formatted(section, request.getContent(), format);

        // 4. LLM 호출
        String json = chatClient.prompt()
            .user(prompt)
            .call()
            .content();

        // 5. JSON -> DTO 변환
        CoverLetterCoachResponse response = converter.convert(json);

        return response;
    }
}
