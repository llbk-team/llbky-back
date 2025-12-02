package com.example.demo.learning.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.learning.dto.response.AiCreateRoadmapResponse;
import com.example.demo.learning.service.LearningService;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("learning")
public class LearningController {

  @Autowired
  private LearningService learningService;

  // AI 학습 로드맵 생성
  @PostMapping("/roadmap/create")
  public ResponseEntity<AiCreateRoadmapResponse> createRoadmap(
    @RequestParam("memberId") Integer memberId,
    @RequestParam("purposes") List<String> purposes,
    @RequestParam("skills") List<String> skills,
    @RequestParam("studyHours") int studyHours) {

      AiCreateRoadmapResponse result = learningService.createLearning(memberId, purposes, skills, studyHours);
      return ResponseEntity.ok(result);
    }

  
  // AI 학습 로드맵 DB 저장
  @PostMapping("/roadmap/save")
  public ResponseEntity<AiCreateRoadmapResponse> saveRoadmap(@RequestBody AiCreateRoadmapResponse roadmap) {
    AiCreateRoadmapResponse result = learningService.saveRoadmap(roadmap);
    return ResponseEntity.ok(result);
  }
  
  

  // 사용자가 올린 문서 / 직군,직무로 AI가 추천해주는 기술

  // 사용자가 선택한 기술 DB 저장



  // 학습 리스트 조회

  // 학습 상세 조회



  // AI 학습 정리 검증

  // AI 데일리 학습 정리 작성


}
