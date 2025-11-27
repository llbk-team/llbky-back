package com.example.demo.interview.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.ai.interview.CreateQuestionAgent;
import com.example.demo.interview.dao.InterviewQuestionDao;
import com.example.demo.interview.dao.InterviewSessionDao;
import com.example.demo.interview.dto.request.QuestionRequest;
import com.example.demo.interview.dto.response.QuestionResponse;
import com.example.demo.interview.dto.response.SaveSessionResponse;
import com.example.demo.interview.entity.InterviewQuestion;
import com.example.demo.interview.entity.InterviewSession;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InterviewService {

  @Autowired
  private InterviewSessionDao interviewSessionDao;

  @Autowired
  private InterviewQuestionDao interviewQuestionDao;

  @Autowired
  private CreateQuestionAgent createQuestionAgent;

  // AI 면접 질문 생성 ---------------------------------------------------------------------------------------------------------------
  public List<QuestionResponse> createAiQuestion(Integer memberId, String type, String targetCompany, List<String> keywords, MultipartFile file) throws Exception {

    QuestionRequest request = new QuestionRequest();
    request.setMemberId(memberId);
    request.setType(type);
    request.setTargetCompany(targetCompany);
    request.setKeywords(keywords);

    if (file != null && !file.isEmpty()) {
      request.setDocumentFileData(file.getBytes());
      request.setDocumentFileName(file.getOriginalFilename());
      request.setDocumentFileType(file.getContentType());
    }

    // Agent 호출
    List<QuestionResponse> questionList = createQuestionAgent.createQuestion(request);
    
    return questionList;
  }

  // // 사용자의 면접 질문 생성 ---------------------------------------------------------------------------------------------------------------
  // public InterviewQuestion createUserQuestion(Integer SessionId, String question) {
  //   InterviewQuestion interviewQuestion = new InterviewQuestion();
  //   interviewQuestion.setSessionId(SessionId);
  //   interviewQuestion.setQuestionText(question);
  //   interviewQuestionDao.insertCustomQuestion(interviewQuestion);

  //   return interviewQuestion;
  // }


  // DB에 면접 질문 저장 ---------------------------------------------------------------------------------------------------------------
  public List<SaveSessionResponse> saveSessionAndQuestion(Integer memberId, String type, String targetCompany, List<String> keywords, MultipartFile file, 
                                     List<String> aiQuestions, List<String> customQuestions) throws Exception { 

    InterviewSession session = new InterviewSession();
    session.setMemberId(memberId);
    session.setInterviewType(type);
    session.setTargetCompany(targetCompany);
    session.setKeyowrds(keywords);
    session.setDocumentFileName(file.getOriginalFilename());
    session.setDocumentFileType(file.getContentType());
    session.setDocumentFileData(file.getBytes());

    // 세션 저장
    interviewSessionDao.insertInterviewSession(session);
    Integer sessionId = session.getSessionId();
    log.info("sessionId : ", sessionId);

    // AI 질문과 사용자 질문 합치기
    List<String> finalQuestions = new ArrayList<>();
    if (aiQuestions != null) finalQuestions.addAll(aiQuestions);
    if (customQuestions != null) finalQuestions.addAll(customQuestions);

    // 질문 저장
    List<SaveSessionResponse> responseList = new ArrayList<>();

    for (String q : finalQuestions) {
      InterviewQuestion question = new InterviewQuestion();
      question.setSessionId(sessionId);
      question.setQuestionText(q);
      interviewQuestionDao.insertInterviewQuestion(question);

      // 저장된 내용을 확인하기 위한 반환값
      SaveSessionResponse dto = new SaveSessionResponse();
      dto.setSessionId(sessionId);
      dto.setQuestionId(question.getQuestionId());
      dto.setQuestionText(question.getQuestionText());
      responseList.add(dto);
    }

    return responseList;
    
  }
}
