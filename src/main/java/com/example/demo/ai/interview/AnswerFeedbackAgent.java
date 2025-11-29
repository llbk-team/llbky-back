package com.example.demo.ai.interview;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.interview.dao.InterviewAnswerDao;
import com.example.demo.interview.dto.response.AnswerFeedbackResponse;
import com.example.demo.interview.entity.InterviewAnswer;
import com.fasterxml.jackson.databind.ObjectMapper;

// 면접 진행 에이전트 (답변별 피드백 생성)
@Component
public class AnswerFeedbackAgent {

    /*==============
      필드
    =============== */

    // ChatClient
    private ChatClient chatClient;

    // AI 응답 DTO → JSON 문자열로 직렬화하기 위한 ObjectMapper
    @Autowired
    private ObjectMapper objectMapper;
    
    // DAO
    @Autowired
    private InterviewAnswerDao interviewAnswerDao;
    
    
    /*==============
      생성자
    =============== */
    public AnswerFeedbackAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    

    /*==============
      메소드
    =============== */

    // 답변별 피드백 생성
    @Transactional
    public AnswerFeedbackResponse execute(int answerId, String answerText, List<String> visualFeedbackList) throws Exception {
        
        // 1. DB에서 답변 불러오기
        InterviewAnswer answer = interviewAnswerDao.selectOneAnswer(answerId);

        if (answer == null) {
            throw new RuntimeException("Answer not found");
        }

        
        // 2. 이미지 분석 리스트 -> 하나의 문자열로 변환
        String visualFeedbackText;

        if (visualFeedbackList == null || visualFeedbackList.isEmpty()) {
            visualFeedbackText = "영상 분석 없음";
        } else {
            StringBuilder sb = new StringBuilder(); 
            for (int i = 0; i < visualFeedbackList.size(); i++) {
                sb.append("프레임 ").append(i + 1).append(" 분석: ");
                sb.append(visualFeedbackList.get(i));
                sb.append("\n");
            }

            visualFeedbackText = sb.toString();
        }


        // 3. Bean 객체 -> JSON 출력 변환기 생성
        BeanOutputConverter<AnswerFeedbackResponse> converter = new BeanOutputConverter<>(AnswerFeedbackResponse.class);

        // DTO 구조 제공 -> JSON 출력 포맷 지정
        String format = converter.getFormat();

        // 4. 프롬프트 구성
        String systemPrompt = """
            당신은 전문 면접관입니다.
            아래 지원자의 면접 답변(언어적 요소)과
            이미지 분석(표정·자세 등 비언어적 요소)를 모두 반영하여
            면접 피드백을 생성하세요.

            평가 항목:
            - languageScore: 발음, 말투, 논리 구조, 내용 명확성
            - nonLanguageScore: 표정, 시선, 자세, 긴장도, 태도
            - totalScore: 두 점수의 종합 평가
            - overallSummary: 전체 요약
            - keyCoachingPoint: 가장 중요한 코칭 포인트
            - speechAnalysis: 언어적 분석
            - toneExpressionAnalysis: 표정·태도 분석
            - timeStructureAnalysis: 말하는 구조, 시간 배분 분석
            - contentAnalysis: 내용의 깊이, 전문성 분석

            평가 규칙:
            - 각 항목 점수는 0~100점 사이 정수로 계산한다.
            - 점수는 명확한 근거 기반으로 설명해야 한다.
            - JSON 형식 이외의 불필요한 문장을 출력하지 않는다.

            사용자의 STT 결과(answerText)가 아래 조건 중 하나면:
            - null
            - 빈 문자열
            - 1~3 단어 이하
            - 의미 없는 발성 ("음", "어…", "하…")
            - 배경 소리만 존재하는 경우

            이럴 때는 다음 규칙대로 답변하라:

            {
            "languageScore": 0,
            "nonLanguageScore": (표정/자세 분석 결과 기반 점수),
            "totalScore": 0,
            "overallSummary": "언어적 답변이 감지되지 않아 내용 기반 분석이 불가합니다.",
            "keyCoachingPoint": "면접 질문에는 최소한 20초 이상의 언어적 답변이 필요합니다.",
            "speechAnalysis": "언어적 발화가 확인되지 않았습니다.",
            "toneExpressionAnalysis": "비언어적 행동 분석은 아래 분석 결과를 참고하세요.",
            "timeStructureAnalysis": "답변 구조 분석이 불가능합니다.",
            "contentAnalysis": "답변 내용이 존재하지 않아 평가할 수 없습니다."
            }

            ---

            그 외 일반적인 답변일 경우에는 기존 규칙에 따라 상세하게 평가하라.

            모든 문장은 공식 보고서 톤인 ‘~합니다’ 형태로 작성하며, ‘~한다’와 같은 평서형은 절대 사용하지 않습니다.

            아래 JSON 형식을 정확히 준수해 답변할 것:
            %s
        """.formatted(format);

        String prompt = """
            다음은 지원자의 면접 답변 내용입니다. 이를 분석하고 피드백을 제공하세요.
            
            [지원자 답변 내용]
            %s

            [이미지 기반 비언어적 분석 결과]
            %s
        """.formatted(answerText, visualFeedbackText);

        // 5. LLM 호출
        String json = chatClient.prompt()
            .system(systemPrompt)
            .user(prompt)
            .call()
            .content();

        // 6. JSON -> DTO 변환
        AnswerFeedbackResponse response = converter.convert(json);

        // 7. DTO -> JSON으로 다시 직렬화해서 DB 업데이트
        String jsonString = objectMapper.writeValueAsString(response);
        interviewAnswerDao.updateAnswerFeedback(answerId, jsonString);

        return response;
        
    }


}
