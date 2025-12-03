package com.example.demo.learning.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.learning.dto.response.AiCreateRoadmapResponse;
import com.example.demo.learning.dto.response.LearningResponse;
import com.example.demo.learning.dto.response.RecommendSkillResponse;
import com.example.demo.learning.entity.LearningDay;
import com.example.demo.learning.service.LearningDayService;
import com.example.demo.learning.service.LearningService;




@RestController
@RequestMapping("/learning")
public class LearningController {

  @Autowired
  private LearningService learningService;
  @Autowired
  private LearningDayService learningDayService;

  // AI 학습 로드맵 생성
  @PostMapping("/roadmap-create")
  public ResponseEntity<AiCreateRoadmapResponse> createRoadmap(
    @RequestParam("memberId") Integer memberId,
    @RequestParam("purposes") List<String> purposes,
    @RequestParam("skills") List<String> skills,
    @RequestParam("studyHours") int studyHours) {

      AiCreateRoadmapResponse result = learningService.createLearning(memberId, purposes, skills, studyHours);
      return ResponseEntity.ok(result);
    }

  
  // AI 학습 로드맵 DB 저장
  @PostMapping("/roadmap-save")
  public ResponseEntity<AiCreateRoadmapResponse> saveRoadmap(@RequestBody AiCreateRoadmapResponse roadmap) {
    AiCreateRoadmapResponse result = learningService.saveRoadmap(roadmap);
    return ResponseEntity.ok(result);
  }
  
  

  // 사용자가 올린 문서 / 직군,직무로 AI가 추천해주는 기술
  @PostMapping("/recommend-skills")
  public ResponseEntity<RecommendSkillResponse> recommendSkill(@RequestParam("memberId") Integer memberId) {
    RecommendSkillResponse result = learningService.recommendSkillsFromFeedback(memberId);
    return ResponseEntity.ok(result); 
  }
  

  // 사용자가 선택한 기술 DB 저장



  // 학습 리스트 조회
  @GetMapping("/list")
  public ResponseEntity<List<LearningResponse>> getLearningList(@RequestParam("memberId") Integer memberId, @RequestParam("status") String status) {
    List<LearningResponse> result = learningService.getLearningListByStatus(memberId, status);
    return ResponseEntity.ok(result);
  }
  

  // 학습 상세 조회



  // AI 학습 정리 제출
  @PostMapping("/submit-day-summary")
  public ResponseEntity<LearningDay> submitLearningDaySummary(
    @RequestParam("dayId") int dayId,
    @RequestParam("learningDaySummary") String learningDaySummary
  ) {
      
    return ResponseEntity.ok(learningDayService.submitMemo(dayId, learningDaySummary));
  }
  


}
