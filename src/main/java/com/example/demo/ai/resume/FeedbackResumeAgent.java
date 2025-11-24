package com.example.demo.ai.resume;

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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FeedbackResumeAgent {
    private ChatClient chatClient;
    private ObjectMapper mapper = new ObjectMapper();
    private MemberDao memberDao;
    private ResumeDao resumeDao;

    public FeedbackResumeAgent(ChatClient.Builder chatClientBuilder, MemberDao memberDao, ResumeDao resumeDao) {
        this.chatClient = chatClientBuilder.build();
        this.memberDao = memberDao;
        this.resumeDao = resumeDao;
    }

    public ResumeReportResponse analyze(int memberId, int resumeId) throws Exception {

        // 이력서 조회
        Resume resume = resumeDao.selectResumeById(resumeId);
        if (resume == null) {
            throw new RuntimeException("Resume not found");
        }

        // JSON 문자열 -> Request DTO 변환
        ResumeReportRequest request = parseResumeToRequest(resume);


        // 멤버 조회
        Member member = memberDao.findById(memberId);
        if (member != null)
            request.setTargetJob(member.getJobRole());

        // 출력 변환기 생성
        BeanOutputConverter<ResumeReportResponse> converter = new BeanOutputConverter<>(ResumeReportResponse.class);

        String format = converter.getFormat();
        String inputJson;
        try {
            inputJson = mapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("JSON 변환 실패", e);
        }
        log.info("REQUEST TO GPT = {}", mapper.writeValueAsString(request));


        // 프롬프트
        String systemPrompt = """
                당신은 'AI 취업 컨설턴트'입니다.
                지원자가 입력한 이력서를 토대로 채용 담당자의 관점에서 분석하세요. 한국어로 출력하세요.

                분석 기준:
                - 역할(Role), 책임(Responsibility), 성과(Result) 중심으로 평가
                - 기술 스택의 실제 적용·난이도·직무 적합성을 평가
                - 경력 기술의 구체성, 애매한 표현, 중복 여부 검토
                - 기술·경력·직무 연결성이 있는지 확인
                - 직무(targetJob)와의 매칭도 점수 포함
                - 개선이 필요한 문장은 before → after 형태로 제안과 함께 이렇게 수정한 이유(reason) 작성
                - format 구조에 포함된 필드만 출력하며 추가/누락 금지

                ★★ 매우 중요한 규칙 (before 오류 방지) ★★
                1) rewriteSuggestions.before는 반드시 원문 이력서의 career.description에서 가져온 '그대로의 문장'이어야 한다.
                2) before 문장을 요약, 변형, 해석 금지. 단 한 글자도 바꾸지 말 것.
                3) 원문에 존재하지 않는 문장은 절대로 before에 넣지 않는다.
                4) 경력이 없을 경우 원문 description이 비어 있음 그럴때는 rewriteSuggestions에 경력사항이 없으니 보완점이나 개선점으로 주어야 한다.
                5) after와 reason만 AI가 생성하며, before는 오직 원문 그대로 복사만 한다.

                출력 규칙:
                - JSON만 출력 (문자열 형태 X)
                - 배열은 반드시 JSON 배열로 출력
                - format 구조를 그대로 따라야 함
                """;
        String prompt = """
                다음은 지원자의 이력서 데이터입니다.
                이력서를 분석하고 format에 맞는 JSON만 출력하세요.

                format:
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

        // DTO -> JSON 문자열로 직렬화하여 저장
        String feedbackJson = mapper.writeValueAsString(resumeReportResponse);
        resumeDao.updateResumeFeedback(resumeId, feedbackJson);

        return resumeReportResponse;
    }

    // String -> JSON 파싱
    private ResumeReportRequest parseResumeToRequest(Resume resume) throws Exception {
        ResumeReportRequest req = new ResumeReportRequest();

        try {
            req.setEducation(mapper.readValue(resume.getEducationInfo(),
                    new TypeReference<List<Map<String, Object>>>() {
                    }));

            req.setCareer(mapper.readValue(resume.getCareerInfo(),
                    new TypeReference<List<Map<String, Object>>>() {
                    }));

            req.setSkills(mapper.readValue(resume.getSkills(),
                    new TypeReference<List<String>>() {
                    }));

            req.setCertificates(mapper.readValue(resume.getCertificates(),
                    new TypeReference<List<Map<String, Object>>>() {
                    }));

        } catch (Exception e) {
            throw new RuntimeException("JSON 파싱 실패", e);
        }

        return req;
    }

}
