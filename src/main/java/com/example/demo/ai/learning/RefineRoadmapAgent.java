package com.example.demo.ai.learning;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.learning.dto.request.RoadmapRefineRequest;
import com.example.demo.learning.dto.response.AiCreateRoadmapResponse;

@Component
public class RefineRoadmapAgent {
  private ChatClient chatClient;

  public RefineRoadmapAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public AiCreateRoadmapResponse refineRoadmap(RoadmapRefineRequest request) {

    BeanOutputConverter<AiCreateRoadmapResponse> converter = new BeanOutputConverter<>(AiCreateRoadmapResponse.class);
    String format = converter.getFormat();

    String system = """
            당신은 전문 커리어 학습 코치입니다.
            아래는 사용자가 이미 생성한 학습 로드맵입니다.

            사용자가 요청한 수정 사항을 반영하여 '전체 로드맵 구조는 유지'하면서
            필요한 부분만 업데이트하여 새로운 로드맵을 생성하세요.

            수정 규칙:
            - 전체 week 수(예: 4주)는 유지
            - days 구조도 유지
            - 사용자가 요청한 변화가 어느 Day/Week에 반영되는지 자연스럽게 조정
            - 기술/학습 범위가 추가된다면 무리 없는 범위로 확장
            - JSON 구조는 아래 형식을 따라야 함

            출력 형식:
            %s
        """.formatted(format);

    String prompt = """
            [기존 로드맵 JSON]
            %s

            [사용자 수정 요청]
            %s
        """.formatted(request.getOriginalRoadmapJson(), request.getUserFeedback());

    // LLM 호출
    String json = chatClient.prompt()
        .system(system)
        .user(prompt)
        .call()
        .content();

    // JSON → DTO 변환
    AiCreateRoadmapResponse result = converter.convert(json);

    return result;
  }

}
