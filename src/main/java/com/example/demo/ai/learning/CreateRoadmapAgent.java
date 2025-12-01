package com.example.demo.ai.learning;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.learning.dto.request.RoadmapRequest;
import com.example.demo.learning.dto.response.AiCreateRoadmapResponse;
import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.dto.Member;

@Component
public class CreateRoadmapAgent {

  private ChatClient chatClient;

  @Autowired
  private MemberDao memberDao;

  public CreateRoadmapAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public AiCreateRoadmapResponse generateRoadmap(RoadmapRequest request) {

    // 사용자 직무
    Member member = memberDao.findById(request.getMemberId());
    String jobRole = member.getJobRole();

    // Bean 객체 -> JSON 출력 변환기 생성
    BeanOutputConverter<AiCreateRoadmapResponse> converter = new BeanOutputConverter<>(AiCreateRoadmapResponse.class); 
    
    // DTO 구조 제공 -> JSON 출력 포맷 지정
    String format = converter.getFormat();
    
    // 시스템 프롬프트 구성
    String system = """
        당신은 학습 설계 전문가입니다.
        학습 계획 정보를 바탕으로, 주차별 / 일차별 학습 로드맵을 JSON 형식으로 생성하세요.
        학습 플랜은 4주차 기준이며 각 주차는 무조건 7일이어야 합니다. 각 일차별로 계획을 작성하세요.
        절대로 코드블록(```), 백틱(``) 등을 포함하지 마세요.

        형식(JSON만 출력):
        %s
        """.formatted(format);

    // 사용자 프롬프트 구성
    String prompt = """
        사용자 학습 정보
        - 희망 직무: %s
        - 하루 공부 가능 시간:
        - 학습 목적: 
        - 관심 기술:
        
        위 정보를 기반으로 4주차 맞춤 학습 로드맵을 설계하세요.
        """.formatted(jobRole, request.getStudyHours(), request.getPurposes(), request.getSkills());


    // LLM 호출
    String responseJson = chatClient.prompt()
      .system(system)
      .user(prompt)
      .call()
      .content();

    // JSON -> DTO 변환
    AiCreateRoadmapResponse response = converter.convert(responseJson);

    return response;
  }
}
