package com.example.demo.ai.learning;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.learning.dto.response.MemoCheckResponse;
import com.example.demo.learning.entity.LearningDay;

// 일일 학습 노트 검증용 에이전트

@Component
public class MemoCheckAgent {

    // ChatClient
    private ChatClient chatClient;

    public MemoCheckAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // 검증
    public MemoCheckResponse execute(LearningDay day, String origin) {

        // 구조화된 출력 변환기 생성
        BeanOutputConverter<MemoCheckResponse> converter = new BeanOutputConverter<>(MemoCheckResponse.class);

        String format = converter.getFormat();

        // 프롬프트 구성
        String sysPrompt = """
            당신은 전문 학습 코치이며, 사용자가 작성한 메모가 
            오늘의 학습 주제와 실제로 관련 있는지 매우 엄격하게 검증해야 합니다.

            다음 규칙을 절대적으로 지키십시오:

            1) 사용자의 메모가 오늘의 학습 주제 또는 학습 내용과 
            **직접적인 개념적 연결이 없으면 반드시 isValid = false** 로 설정합니다.

            2) 단순히 개발과 관련된 용어가 등장하거나,
            얼핏 보기에는 그럴싸해 보이는 문장이 포함되어 있어도  
            오늘의 학습 주제와 실제로 연결되지 않으면 **false** 를 반환합니다.

            3) 부분적으로라도 연관이 부족하거나 핵심을 벗어나면  
            isValid = false 로 설정합니다.  
            ("애매하지만 비슷해 보이는" 경우도 false)

            4) 사용자의 메모 안에 잘못된 기술적 설명이나 오해가 포함되어 있으면  
            관련성이 있더라도 **false** 로 설정하고 reason에 정확히 지적합니다.

            5) isValid가 true가 되려면 다음을 충족해야 합니다:
            - 학습 주제를 정확히 언급하거나
            - 학습 내용의 핵심 개념을 이해하고 설명하고 있으며
            - 사용자의 메모가 학습 목표와 직접적인 관계를 가지며
            - 기술적 사실이 틀리지 않아야 합니다.

            6) 결과는 반드시 아래 JSON 형식을 정확히 지켜 출력해야 합니다:
            %s
        """.formatted(format);

        String userPrompt = """   

            [오늘의 학습 주제]
            %s

            [오늘의 학습 내용]
            %s

            [사용자가 입력한 메모]
            %s

            오늘의 학습 주제와 내용을 사용자가 입력한 메모와 비교하여 관련성이 충분한지 검증하고,
            사용자가 입력한 메모에서 부족한 점이나 이상한 내용이 있으면 알려주세요. 

        """.formatted(day.getTitle(), day.getContent(), origin);

        // AI 응답 받기
        String json = chatClient.prompt()
            .system(sysPrompt)
            .user(userPrompt)
            .call()
            .content();

        // JSON -> DTO 변환
        MemoCheckResponse checkResult = converter.convert(json);

        return checkResult;
    }
}
