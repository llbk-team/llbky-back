package com.example.demo.learning.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.learning.entity.Learning;
import com.example.demo.learning.entity.LearningDay;
import com.example.demo.learning.entity.LearningWeek;
import com.example.demo.learning.service.LearningDayService;
import com.example.demo.learning.service.LearningService;
import com.example.demo.learning.service.LearningWeekService;

@RestController
@RequestMapping("/learning")
public class LearningController {

  @Autowired
  private LearningService learningService;

  @Autowired
  private LearningWeekService learningWeekService;

  @Autowired
  private LearningDayService learningDayService;

  
  // 학습 로드맵 생성
  @PostMapping("/create-learning")
  public String createLearning(@RequestBody Learning learning) {
    learningService.createLearning(learning);
    return "생성 완료";
  }

  // 학습 ID로 상세 조회
  @GetMapping("/{learningId}")
  public Learning getLearningById(@PathVariable("learningId") int learningId) {
    return learningService.getLearningById(learningId);
  }

  // 사용자가 가진 전체 학습 리스트 조회
  @GetMapping("/member/{memberId}")
  public List<Learning> getLearningListByMember(@PathVariable("memberId") int memberId) {
    return learningService.getLearningListByMember(memberId);
  }

  // 상태(학습중/완료 등)로 리스트 조회
  @GetMapping("/member/{memberId}/status/{status}")
  public List<Learning> getLearningListByStatus(@PathVariable("memberId") int memberId,
      @PathVariable("status") String status) {
    return learningService.getLearningListByStatus(memberId, status);
  }

  // 사용자별 학습 개수 조회
  @GetMapping("/user/{memberId}/count")
  public int getLearningCount(@PathVariable("memberId") int memberId) {
    return learningService.getLearningCount(memberId);
  }

  // 학습 상태 또는 기타 정보 업데이트
  @PutMapping("/learning-update")
  public int updateLearning(@RequestBody Learning learning) {
    return learningService.updateLearning(learning);
  }

  // ---------------------------------------------------------------

  // 주차 생성
  @PostMapping("create-week")
  public int createWeek(@RequestBody LearningWeek learningWeek) {
    return learningWeekService.createWeek(learningWeek);
  }

  // 주차 상세 조회 (weekId)
  @GetMapping("/{weekId}")
  public LearningWeek getWeekById(@PathVariable("weekId") int weekId) {
    return learningWeekService.getWeekById(weekId);
  }

  // 특정 학습(learningId)의 주차 리스트 조회
  @GetMapping("/list/{learningId}")
  public List<LearningWeek> getWeekListByLearningId(@PathVariable("learningId") int learningId) {
    return learningWeekService.getWeekListByLearningId(learningId);
  }

  // 학습 ID + 주차 번호로 조회
  @GetMapping("/{learningId}/number/{weekNumber}")
  public LearningWeek getWeekByLearningIdAndWeekNumber(@PathVariable("learningId") int learningId, @PathVariable("weekNumber") int weekNumber) {
    return learningWeekService.getWeekByLearningIdAndWeekNumber(learningId, weekNumber);
  }

  // 주차 수정
  @PutMapping("week-update")
  public int updateWeek(@RequestBody LearningWeek learningWeek) {
    return learningWeekService.updateWeek(learningWeek);
  }

  // ---------------------------------------------------------------

  // 일차 생성
  @PostMapping("create-day")
  public int createDay(@RequestBody LearningDay learningDay) {
    return learningDayService.createDay(learningDay);
  }

  // dayId로 상세 조회
  @GetMapping("/{dayId}")
  public LearningDay getDayById(@PathVariable("dayId") int dayId) {
    return learningDayService.getDayById(dayId);
  }

  // 특정 주차의 전체 일차 리스트 조회
  @GetMapping("/week/{weekId}")
  public List<LearningDay> getDayListByWeekId(@PathVariable("weekId") int weekId) {
    return learningDayService.getDayListByWeekId(weekId);
  }

  // 특정 학습 플랜 전체 일차 리스트 조회
  @GetMapping("/learning/{learningId}")
  public List<LearningDay> getDayListByLearningId(@PathVariable("learningId") int learningId) {
    return learningDayService.getDayListByLearningId(learningId);
  }

  // 특정 주차 + 일차 번호로 조회
  @GetMapping("/week/{weekId}/number/{dayNumber}")
  public LearningDay getDayByWeekIdAndDayNumber(@PathVariable("weekId") int weekId, @PathVariable("dayNumber") int dayNumber) {
    return learningDayService.getDayByWeekIdAndDayNumber(weekId, dayNumber);
  }

  // 일차 업데이트
  @PutMapping("day-update")
  public int updateDay(@RequestBody LearningDay learningDay) {
    return learningDayService.updateDay(learningDay);
  }

}
