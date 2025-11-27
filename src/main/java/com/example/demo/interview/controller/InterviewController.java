package com.example.demo.interview.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.interview.dao.InterviewAnswerDao;
import com.example.demo.interview.dto.response.AnswerFeedbackResponse;
import com.example.demo.interview.dto.response.QuestionResponse;
import com.example.demo.interview.dto.response.SaveSessionResponse;
import com.example.demo.interview.entity.InterviewAnswer;
import com.example.demo.interview.service.InterviewService;

@RestController
@RequestMapping("/interview")
public class InterviewController {
 // Service
@Autowired
private InterviewService interviewService;

// DAO
@Autowired
private InterviewAnswerDao interviewAnswerDao;

    @PostMapping("/ai-questions")
    public ResponseEntity<List<QuestionResponse>> createQuestion(
        @RequestParam("memberId") Integer memberId,
        @RequestParam("type") String type,
        @RequestParam("targetCompany") String targetCompany,
        @RequestParam(value = "keywords", required = false) List<String> keywords,
        @RequestParam(value = "file", required = false) MultipartFile file
    ) throws Exception {

        List<QuestionResponse> result = interviewService.createAiQuestion(memberId, type, targetCompany, keywords, file);
        return ResponseEntity.ok(result);
    }   

    @PostMapping("/session-save")
    public ResponseEntity<List<SaveSessionResponse>> saveSession(
        @RequestParam Integer memberId,
        @RequestParam String type,
        @RequestParam String targetCompany,
        @RequestParam(required = false) List<String> keywords,
        @RequestParam(required = false) List<String> aiQuestions,
        @RequestParam(required = false) List<String> customQuestions,
        @RequestParam(required = false) MultipartFile file
    ) throws Exception {

        List<SaveSessionResponse> response = interviewService.saveSessionAndQuestion(memberId, type, targetCompany, keywords, file, aiQuestions, customQuestions);
        return ResponseEntity.ok(response);
    }

    // 답변 제출============================================================================================================================================
    @PostMapping(
        value = "/submit-answer",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Integer> submitAnswer(
        @RequestParam("questionId") int questionId,
        @RequestParam(value = "audio", required = false) MultipartFile audio,
        @RequestParam(value = "video", required = false) MultipartFile video,
        @RequestParam(value = "frames", required = false) List<MultipartFile> frames
    ) throws Exception {

        return ResponseEntity.ok(interviewService.createInterviewAnswer(questionId, audio, video));        
    }
    
    // 답변별 AI 피드백 생성===================================================================================================================
    @PostMapping(
       value = "/create-feedback",
       consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
       produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AnswerFeedbackResponse> createFeedback(
        @RequestParam("answerId") int answerId,
        @RequestParam(value = "audio", required = false) MultipartFile audio,
        @RequestParam(value = "videoAudio", required = false) MultipartFile videoAudio,
        @RequestParam(value = "frames", required = false) List<MultipartFile> frames
    ) throws Exception {
        
        return ResponseEntity.ok(interviewService.createAnswerFeedback(answerId, audio, videoAudio, frames));
    }
    

    // 면접 질문 기반 답변 조회===============================================================================================================
    @GetMapping("/answers-by-question")
    public ResponseEntity<InterviewAnswer> getAnswersByQuestionId(@RequestParam("questionId") int questionId) {
        return ResponseEntity.ok(interviewService.getInterviewAnswersByQuestionId(questionId));
    }

    // 면접 답변 단건 조회===============================================================================================================================
    @GetMapping("/answer-detail")
    public ResponseEntity<InterviewAnswer> getOneAnswer(@RequestParam("answerId") int answerId) {
        return ResponseEntity.ok(interviewService.getOneInterviewAnswer(answerId));
    }
}
