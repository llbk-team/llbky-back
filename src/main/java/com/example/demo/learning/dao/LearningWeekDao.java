package com.example.demo.learning.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.learning.entity.LearningWeekEntity;

@Mapper
public interface LearningWeekDao {
  // 학습 로드맵 생성 - 주차별
  public int insert(LearningWeekEntity learningWeekEntity);

  // weekId로 학습 조회
  public LearningWeekEntity selectedByWeekId(int weekId);

  // 학습별 주차 리스트 조회
  public List<LearningWeekEntity> selectListByLearningId(int learningId);

  // 학습 상태 변경
  public int update(LearningWeekEntity learningWeekEntity);
}
