package com.example.demo.learning.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.learning.entity.LearningDayEntity;

@Mapper
public interface LearningDayDao {
  // 학습 로드맵 생성 - 일차별
  public int insert(LearningDayEntity learningDayEntity);

  // dayId로 학습 조회
  public LearningDayEntity selectedByDayId(int dayId);

  // 주차별 전체 리스트 조회
  public List<LearningDayEntity> selectListByWeekId(int weekId);

  // 학습 상태 변경
  public int update(LearningDayEntity learningDayEntity);
}
