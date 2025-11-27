package com.example.demo.interview.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.interview.dto.request.SessionRequest;
import com.example.demo.interview.dto.response.QuestionResponse;
import com.example.demo.interview.dto.response.SaveSessionResponse;
import com.example.demo.interview.service.InterviewService;


@RestController
@RequestMapping("/interview")
public class InterviewController {

  @Autowired
  private InterviewService interviewService;

  @PostMapping("/ai-questions")
  public ResponseEntity<List<QuestionResponse>> createQuestion(
      @RequestParam("memberId") Integer memberId,
      @RequestParam("type") String type,
      @RequestParam("targetCompany") String targetCompany,
      @RequestParam(value = "keywords", required = false) List<String> keywords,
      @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {

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
        @RequestParam(required = false) MultipartFile file) throws Exception {

    List<SaveSessionResponse> response = interviewService.saveSessionAndQuestion(memberId, type, targetCompany, keywords, file, aiQuestions, customQuestions);
    return ResponseEntity.ok(response);
  }
  

}
