package com.example.demo.newstrend.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.newstrend.entity.NewsSummary;

@Mapper
public interface NewsSummaryDao {
  // 뉴스 요약 저장
  public int insertNewsSummary(NewsSummary newsSummary);

  // 단일 뉴스 조회
  public NewsSummary selectNewsSummaryById(int summaryId);

  // URL 중복 체크
  public NewsSummary selectNewsSummaryBySourceUrl(String sourceUrl);

  // 특정 멤버의 뉴스 조회
  public List<NewsSummary> selectLateNewsSummaryByMemberId(int memberId, int limit);

  // 뉴스 삭제
  public int deletNewsSummary(int summaryId);
}
