package com.example.demo.learning.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.learning.entity.Learning;

@Mapper
public interface LearningDao {
  // 학습 로드맵 생성
  public int insert(Learning LearningEntity);

  // LearningId로 학습 조회
  public Learning selectedByLearningId(int LearningId);

  // 사용자별 학습 전체 리스트 조회
  public List<Learning> selectListByMemberId(int MemberId);

  // 사용자별 학습 개수 조회
  public int countByMemberId(int memberId);

  // 학습 상태 변경
  public int update(Learning LearningEntity);
}
