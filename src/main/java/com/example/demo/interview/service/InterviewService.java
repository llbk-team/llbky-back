package com.example.demo.interview.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.ai.interview.AnswerFeedbackAgent;
import com.example.demo.ai.interview.CreateQuestionAgent;
import com.example.demo.ai.interview.STTAgent;
import com.example.demo.ai.interview.VisualAnalysisAgent;
import com.example.demo.interview.dao.InterviewAnswerDao;
import com.example.demo.interview.dao.InterviewQuestionDao;
import com.example.demo.interview.dao.InterviewSessionDao;
import com.example.demo.interview.dto.request.QuestionRequest;
import com.example.demo.interview.dto.response.AnswerFeedbackResponse;
import com.example.demo.interview.dto.response.QuestionResponse;
import com.example.demo.interview.dto.response.SaveSessionResponse;
import com.example.demo.interview.entity.InterviewAnswer;
import com.example.demo.interview.entity.InterviewQuestion;
import com.example.demo.interview.entity.InterviewSession;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InterviewService {
    // DAO
    @Autowired
    private InterviewSessionDao interviewSessionDao;
    @Autowired
    private InterviewQuestionDao interviewQuestionDao;
    @Autowired
    private InterviewAnswerDao interviewAnswerDao;

    // AI Agent
    @Autowired
    private CreateQuestionAgent createQuestionAgent;
    @Autowired
    private STTAgent sttAgent;
    @Autowired
    private VisualAnalysisAgent visualAnalysisAgent;
    @Autowired
    private AnswerFeedbackAgent answerFeedbackAgent;

    /*====================
      면접 세션 관련 메소드
    =====================*/
    // 면접 세션 생성
    
    
    // 면접 세션 종료 & 종합 피드백 생성
    
    
    // 면접 목록 조회
    
    
    // 면접 리포트 상세보기
    

    /*====================
      면접 질문 관련 메소드
    =====================*/

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

    /*====================
      면접 답변 관련 메소드
    =====================*/

    // 답변 제출=======================================================================================
    public int createInterviewAnswer(
        int questionId, 
        MultipartFile audio, 
        MultipartFile video
    ) throws Exception {
        
        // DB에 답변 원본 파일 저장
        InterviewAnswer answer = new InterviewAnswer();
        answer.setQuestionId(questionId);

        if (audio != null && !audio.isEmpty()) {
            answer.setAudioFileName(audio.getOriginalFilename());
            answer.setAudioFileType(audio.getContentType());
            answer.setAudioFileData(audio.getBytes());
        } else if (video != null && !video.isEmpty()) {
            answer.setVideoFileName(video.getOriginalFilename());
            answer.setVideoFileType(video.getContentType());
            answer.setVideoFileData(video.getBytes());
        }

        return interviewAnswerDao.insertInterviewAnswer(answer);
    }

    // 답변 다시 제출===================================================================================
    public int modifyInterviewAnswer(int answerId, MultipartFile audio, MultipartFile video) throws Exception {

        // DB에 답변 원본 파일 업데이트
        InterviewAnswer answer = interviewAnswerDao.selectOneAnswer(answerId);
        if (answer == null) {
            throw new RuntimeException("Answer not found");
        }

        if (audio != null && !audio.isEmpty()) {
            answer.setAudioFileName(audio.getOriginalFilename());
            answer.setAudioFileType(audio.getContentType());
            answer.setAudioFileData(audio.getBytes());
        } else if (video != null && !video.isEmpty()) {
            answer.setVideoFileName(video.getOriginalFilename());
            answer.setVideoFileType(video.getContentType());
            answer.setVideoFileData(video.getBytes());
        }

        return interviewAnswerDao.updateInterviewAnswer(answer);
    }

    // 답변 분석 + 피드백 생성===========================================================================
    @Transactional
    public AnswerFeedbackResponse createAnswerFeedback(
        int answerId,
        MultipartFile audio,
        MultipartFile videoAudio,
        List<MultipartFile> frames
    ) throws Exception {

        // 1. DB에 저장된 답변 조회
        InterviewAnswer answer = interviewAnswerDao.selectOneAnswer(answerId);
        if (answer == null) {
            throw new RuntimeException("Answer not found");
        }

        // 2. STT Agent 호출 - 변환된 텍스트 얻기

        // 변환할 파일
        byte[] sttBytes = null;
        String fileName = null;

        if (videoAudio != null && !videoAudio.isEmpty()) {
            log.info("=== [STT INPUT - videoAudio] ===");
            log.info("Content-Type: {}", videoAudio.getContentType());
            log.info("Filename: {}", videoAudio.getOriginalFilename());
            log.info("Size (bytes): {}", videoAudio.getSize());
            
            sttBytes = videoAudio.getBytes();   //영상 오디오 파일
            fileName = videoAudio.getOriginalFilename();
            
        } else if (audio != null && !audio.isEmpty()) {
            log.info("=== [STT INPUT - audio] ===");
            log.info("Content-Type: {}", audio.getContentType());
            log.info("Filename: {}", audio.getOriginalFilename());
            log.info("Size (bytes): {}", audio.getSize());

            sttBytes = audio.getBytes();    // 순수 음성 파일
            fileName = audio.getOriginalFilename();

        } else {
            log.warn("=== [STT INPUT] No audio/videoAudio file received ===");

            sttBytes = null;
        }

        // 변환된 텍스트
        String answerText = "음성 입력이 감지되지 않았습니다.";
        if (videoAudio != null && !videoAudio.isEmpty()) {
            answerText = sttAgent.sttSave(
                answerId,
                videoAudio.getOriginalFilename(),
                videoAudio.getBytes()
            );
        } else if (audio != null && !audio.isEmpty()) {
            answerText = sttAgent.sttSave(
                answerId,
                audio.getOriginalFilename(), 
                audio.getBytes()
            );
        }
        // 3. VisualAnalysis Agent 호출 - 이미지 프레임 분석 리스트 얻기
        List<String> visualFeedback = visualAnalysisAgent.analyzeFrames(frames);

        // 4. AnswerFeedback Agent 호출 - 답변별 피드백 생성 
        AnswerFeedbackResponse answerFeedback = answerFeedbackAgent.execute(answerId, answerText, visualFeedback);

        return answerFeedback;
    }

    // 면접 질문 선택 시 해당하는 답변 조회===================================================================
    public InterviewAnswer getInterviewAnswersByQuestionId(int questionId) {
        return interviewAnswerDao.selectInterviewAnswerByQuestionId(questionId);
    }

    // 답변 ID로 답변 조회==================================================================================
    public InterviewAnswer getOneInterviewAnswer(int answerId) {
        return interviewAnswerDao.selectOneAnswer(answerId);
    }


}
