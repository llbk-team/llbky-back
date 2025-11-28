package com.example.demo.ai.interview;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.interview.dao.InterviewQuestionDao;
import com.example.demo.interview.dto.request.QuestionRequest;
import com.example.demo.interview.dto.response.AiQuestionResponse;
import com.example.demo.interview.entity.InterviewQuestion;

// 면접 질문 생성 에이전트
@Component
public class CreateQuestionAgent {

  @Autowired
  private InterviewQuestionDao interviewQuestionDao;

  // AI 응답 DTO → JSON 문자열로 직렬화하기 위한 ObjectMapper
  @Autowired
  private PdfReader pdfReader;

  // ChatClient
  private ChatClient chatClient;

  public CreateQuestionAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public List<AiQuestionResponse> createQuestion(QuestionRequest request) throws Exception {

    // PDF에서 텍스트 추출
    String documentText = "";
    if (request.getDocumentFileData() != null) {
      documentText = pdfReader.read(request.getDocumentFileData());
    }

    // JSON 변환
    BeanOutputConverter<AiQuestionResponse[]> converter = new BeanOutputConverter<>(AiQuestionResponse[].class);
    String format = converter.getFormat();

    // System prompt
    String system = """
        당신은 면접관입니다.
        제공된 내용을 기반으로 실제 면접에서 사용할 질문을 생성합니다.
        서류가 첨부되었다면 서류 내용도 포함해서 질문을 생성합니다.

        출력 규칙:
        - 출력은 반드시 JSON 배열이어야 합니다.
        - 키워드가 없으면 keyword 질문은 생성하지 않습니다.
        - JSON 이외의 텍스트 출력 금지.

        JSON 출력 형식:
        %s
        """.formatted(format);

    // User prompt
    String prompt = """
        다음 정보를 기반으로 면접 질문을 생성하세요.
        [면접 유형]
        %s

        [지원 기업명]
        %s

        [선택된 키워드]
        %s

        =============
        [서류 내용]
        %s
        =============
        """.formatted(request.getType(), request.getTargetCompany(), request.getKeywords(), documentText);

    // LLM 호출
    String responseJson = chatClient.prompt()
        .system(system)
        .user(prompt)
        .call()
        .content();

    // JSON -> DTO 배열
    AiQuestionResponse[] arr = converter.convert(responseJson);

    // 배열 -> List
    List<AiQuestionResponse> list = Arrays.asList(arr);

    // // DB 저장
    // for (QuestionResponse q : list) {
    //   InterviewQuestion question = new InterviewQuestion();
    //   question.setSessionId(sessionId);
    //   question.setQuestionText(q.getAiQuestion());
    //   interviewQuestionDao.insertInterviewQuestion(question);
    // }

    // 질문 리스트 반환
    return list;
  }
}
