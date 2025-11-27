package com.example.demo.interview.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.ai.interview.AnswerFeedbackAgent;
import com.example.demo.ai.interview.STTAgent;
import com.example.demo.ai.interview.VisualAnalysisAgent;
import com.example.demo.interview.dao.InterviewAnswerDao;
import com.example.demo.interview.dao.InterviewQuestionDao;
import com.example.demo.interview.dao.InterviewSessionDao;
import com.example.demo.interview.dto.response.AnswerFeedbackResponse;
import com.example.demo.interview.entity.InterviewAnswer;

@Service
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
    // AI 면접 예상 면접 질문 생성


    // 사용자 예상 면접 질문 추가


    // 면접 질문 목록 조회


    // 면접 리포트 상세보기 시 질문 목록 조회



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
        if (videoAudio != null && !videoAudio.isEmpty()) {
            sttBytes = videoAudio.getBytes();   //영상 오디오 파일
        } else if (audio != null && !audio.isEmpty()) {
            sttBytes = audio.getBytes();    // 순수 음성 파일
        } else {
            sttBytes = null;
        }

        // 변환된 텍스트
        String answerText = "음성 입력이 감지되지 않았습니다.";
        if (sttBytes != null) {
            String fileName = (videoAudio != null && !videoAudio.isEmpty())
                    ? "video_audio.wav"
                    : "audio.wav";

            answerText = sttAgent.sttSave(answerId, fileName, sttBytes);
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
