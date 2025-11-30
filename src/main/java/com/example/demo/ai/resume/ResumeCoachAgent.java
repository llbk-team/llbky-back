package com.example.demo.ai.resume;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.resume.dto.request.ResumeCoachRequest;
import com.example.demo.resume.dto.response.ResumeCoachResponse;

import lombok.extern.slf4j.Slf4j;
/*
  이력서 실시간 코칭
*/
@Component
@Slf4j
public class ResumeCoachAgent {
  private ChatClient chatClient;

  public ResumeCoachAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public ResumeCoachResponse coach(ResumeCoachRequest request) {
    // 출력 변환기 생성
    BeanOutputConverter<ResumeCoachResponse> converter = new BeanOutputConverter<>(ResumeCoachResponse.class);

    String format = converter.getFormat();

    // 프롬프트
    String systemPrompt = """
        당신은 "AI 취업 컨설턴트"입니다.
        사용자가 작성 중인 이력서 문장을 분석해 실시간으로 코칭하세요.

        출력 규칙:
        - 반드시 JSON만 출력
        - 제공된 format 구조 외의 필드 추가 금지
        - summary / strengths/ improvements는 짧고 명확하게
        - improvedText는 사용자의 문장을 자연스럽고 전문적으로 수정한 버전으로 작성
        - 한국어로 작성하세요.

        부적절한 입력 처리 규칙:
          만약 사용자 입력이 다음 중 하나라도 해당하면 “평가 불가”로 처리해야 합니다:
          - 의미 없는 단어 나열 (예: asdf, ㄱㄱㄱ, random text 등)
          - 문장 구조가 없는 단편적 단어
          - 욕설, 비속어, 공격적 표현
          - 자소서 항목으로 볼 수 없는 내용
          - 항목 전체가 비어 있거나 공란인 경우
        """;

    String prompt = """
        사용자 섹션: %s

        [입력 내용]
        %s

        아래 JSON format에 맞게 출력하세요:
        %s
        """.formatted(
        request.getSection(),
        request.getContent(),
        format);

    String json = chatClient.prompt()
        .system(systemPrompt)
        .user(prompt)
        .call()
        .content();

    // JSON -> DTO 변환
    ResumeCoachResponse result = converter.convert(json);

    return result;
  }
}
