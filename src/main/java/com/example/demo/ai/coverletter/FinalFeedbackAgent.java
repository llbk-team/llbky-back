package com.example.demo.ai.coverletter;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.coverletter.dao.CoverLetterDao;
import com.example.demo.coverletter.dto.response.CoverLetterFinalFeedback;
import com.example.demo.coverletter.entity.CoverLetter;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class FinalFeedbackAgent {
    
    // DAO
    @Autowired
    private CoverLetterDao coverLetterDao;

    // AI 응답 DTO → JSON 문자열로 직렬화하기 위한 ObjectMapper
    @Autowired
    private ObjectMapper objectMapper;

    // ChatClient
    private ChatClient chatClient;

    public FinalFeedbackAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // 종합 피드백 생성
    public CoverLetterFinalFeedback execute(int coverletterId) throws Exception {

        // 1. DB에서 자소서 불러오기
        CoverLetter coverLetter = coverLetterDao.selectOneCoverLetter(coverletterId);
        if (coverLetter == null) {
            throw new RuntimeException("CoverLetter not found");
        }

        // 2. Bean 객체 -> JSON 출력 변환기 생성
        BeanOutputConverter<CoverLetterFinalFeedback> converter = new BeanOutputConverter<>(CoverLetterFinalFeedback.class);

        // DTO 구조 제공 -> JSON 출력 포맷 지정
        String format = converter.getFormat();

        // 3. 프롬프트 구성
        String prompt = """
            아래 자기소개서를 종합적으로 분석하세요.

            분석 항목:
            1) 문장 분석 (문법, 가독성, 논리 흐름, STAR 기법 적용 여부)
            2) 강점 3가지
            3) 개선 제안 3가지

            반드시 아래 JSON 형식을 그대로 채워서 출력하세요:
            %s

            --- 자기소개서 내용 ---
            [지원동기]
            %s

            [성장과정]
            %s

            [직무 역량]
            %s

            [입사 후 포부]
            %s
        """.formatted(
            format,
            coverLetter.getSupportMotive(),
            coverLetter.getGrowthExperience(),
            coverLetter.getJobCapability(),
            coverLetter.getFuturePlan()
        );

        // 4. LLM 호출
        String json = chatClient.prompt()
            .user(prompt)
            .call()
            .content();

        // 5. JSON -> DTO 변환
        CoverLetterFinalFeedback feedback = converter.convert(json);

        // 6. DTO -> JSON으로 다시 직렬화해서 DB 업데이트
        String jsonString = objectMapper.writeValueAsString(feedback);
        coverLetter.setCoverFeedback(jsonString);
        coverLetterDao.updateCoverLetter(coverLetter);

        return feedback;
    }

}
