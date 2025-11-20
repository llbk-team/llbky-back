package com.example.demo.newstrend.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.newstrend.entity.JobInsight;

@Mapper
public interface JobInsightDao {
  // LLM 생성 직무 인사이트 저장
  public int insertJobInsight(JobInsight jobInsight);

  // 특정 멤버의 최신 직무 인사이트 1개 조회
  public JobInsight selectLatestJobInsight(int memberId);

  // 삭제
  public int deleteJobInsight(int insightId);

}
