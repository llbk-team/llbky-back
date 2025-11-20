package com.example.demo.interview.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.interview.dao.InterviewSessionDao;
import com.example.demo.interview.dao.InterviewQuestionDao;
import com.example.demo.interview.dao.InterviewAnswerDao;
import com.example.demo.interview.entity.InterviewSession;
import com.example.demo.interview.entity.InterviewQuestion;
import com.example.demo.interview.entity.InterviewAnswer;

@RestController
@RequestMapping("/test/interview")
public class InterviewTestController {

    @Autowired
    private InterviewSessionDao sessionDao;

    @Autowired
    private InterviewQuestionDao questionDao;

    @Autowired
    private InterviewAnswerDao answerDao;


    // ================================
    // 1) 세션 INSERT 테스트
    // ================================
    @PostMapping("/session/insert")
    public String insertSession() {
        InterviewSession s = new InterviewSession();
        s.setMemberId(1); // 테스트용
        s.setInterviewType("GENERAL");
        s.setTargetCompany("카카오");

        sessionDao.insertInterviewSession(s);

        return "세션 생성 OK! PK = " + s.getSessionId();
    }


    // ================================
    // 2) 특정 회원의 세션 목록 조회
    // ================================
    @GetMapping("/session/list")
    public List<InterviewSession> selectSessions(@RequestParam("memberId") int memberId) {
        return sessionDao.selectAllInterviewSessions(memberId);
    }


    // ================================
    // 3) 질문 INSERT 테스트
    // ================================
    @PostMapping("/question/insert")
    public String insertQuestion(@RequestParam("sessionId") int sessionId) {
        InterviewQuestion q = new InterviewQuestion();
        q.setSessionId(sessionId);
        q.setQuestionText("자기소개 해주세요");

        questionDao.insertInterviewQuestion(q);

        return "질문 생성 OK! PK = " + q.getQuestionId();
    }


    // ================================
    // 4) 세션별 질문 전체 조회
    // ================================
    @GetMapping("/question/list")
    public List<InterviewQuestion> selectQuestions(@RequestParam("sessionId") int sessionId) {
        return questionDao.selectInterviewQuestionsBySessionId(sessionId);
    }


    // ================================
    // 5) 답변 INSERT (텍스트 + 더미 파일)
    // ================================
    @PostMapping("/answer/insert")
    public String insertAnswer(@RequestParam("questionId") int questionId) {

        // 파일 없이도 테스트 가능 (null 넣어도 됨)
        InterviewAnswer a = new InterviewAnswer();
        a.setQuestionId(questionId);
        a.setAnswerText("저는 백엔드 개발자 규민입니다.");
        a.setAnswerFeedback("{\"score\": 90, \"comment\": \"좋습니다\"}");

        answerDao.insertInterviewAnswer(a);

        return "답변 생성 OK! PK = " + a.getAnswerId();
    }


    // ================================
    // 6) 질문ID로 답변 조회
    // ================================
    @GetMapping("/answer/get")
    public InterviewAnswer getAnswer(@RequestParam("questionId") int questionId) {
        return answerDao.selectInterviewAnswerByQuestionId(questionId);
    }
}
