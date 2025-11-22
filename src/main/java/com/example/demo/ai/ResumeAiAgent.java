package com.example.demo.ai;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.dto.Member;
import com.example.demo.resume.dao.ResumeDao;
import com.example.demo.resume.dto.request.ResumeReportRequest;
import com.example.demo.resume.dto.response.ResumeReportResponse;
import com.example.demo.resume.entity.Resume;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ResumeAiAgent {
  private ChatClient chatClient;
  private ObjectMapper mapper = new ObjectMapper();
  private MemberDao memberDao;
  private ResumeDao resumeDao;

  public ResumeAiAgent(ChatClient.Builder chatClientBuilder, MemberDao memberDao, ResumeDao resumeDao) {
    this.chatClient = chatClientBuilder.build();
    this.memberDao = memberDao;
    this.resumeDao = resumeDao;
  }

  public ResumeReportResponse analyze(int memberId, int resumeId) {

    // 이력서 조회
    Resume resume = resumeDao.selectResumeById(resumeId);
    if (resume == null) {
      throw new RuntimeException("Resume not found");
    }
    ResumeReportRequest request = new ResumeReportRequest();
    request.setCareer((List<Map<String, Object>>) resume.getCareerInfo());
    request.setEducation((List<Map<String, Object>>) resume.getEducationInfo());
    request.setSkills((List<String>) resume.getSkills());
    request.setCertificates((List<Map<String, Object>>) resume.getCertificates());

    // 멤버 조회
    Member member = memberDao.findById(memberId);

    // 회원 직무를 Request(Json)에 삽입
    if (member != null) {
      request.setTargetJob(member.getJobRole());
    }

    // 출력 변환기 생성
    BeanOutputConverter<ResumeReportResponse> converter = new BeanOutputConverter<>(ResumeReportResponse.class);

    String format = converter.getFormat();

    // 입력 DTO -> JSON 문자열
    String inputJson = writeJson(request);
    

    // 프롬프트
    String systemPrompt = """
        너는 JSON Schema를 엄격하게 따르는 이력서 분석 AI이다.

        출력은 반드시 ResumeReportResponse(JSON Schema)에 맞아야 한다.

        필드 설명:

        1. score (object)
            - careerScore (int): 경력 기술의 전문성 평가 (0~100)
            - matchScore (int): 목표 직무와의 적합도 (0~100)
            - completionScore (int): 이력서 완성도 (0~100)

        2. strengths (array of string)
            - 사용자의 강점을 2~4개 단문으로 출력

        3. weaknesses (array of string)
            - 개선할 점을 2~4개 단문으로 출력

        4. rewriteSuggestions (array of object)
            - before (string): 원래 문장
            - after (string): 개선된 문장

        규칙:
        - 반드시 유효한 JSON만 출력
        - JSON 외 텍스트/설명/주석 절대 금지
        - 배열은 JSON 배열로 출력 (문자열로 감싸지 않음)
        - 정수 점수는 int로 출력
        - format(JSON Schema)에 없는 필드는 절대 추가하지 말 것

        """;

    String prompt = """
        아래 입력 데이터를 기반으로 format에 맞는 JSON만 출력하라.

        format(JSON Schema):
        %s

        input:
        %s
        """.formatted(format, inputJson);

    // LLM 호출
    String responseJson = chatClient.prompt()
        .system(systemPrompt)
        .user(prompt)
        .options(ChatOptions.builder()
            .temperature(0.3)
            .maxTokens(1500)
            .build())
        .call()
        .content();

    // JSON -> DTO 변환
    ResumeReportResponse resumeReportResponse = converter.convert(responseJson);

    return resumeReportResponse;
  }

  private String writeJson(Object obj){
        try{
            return mapper.writeValueAsString(obj);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
