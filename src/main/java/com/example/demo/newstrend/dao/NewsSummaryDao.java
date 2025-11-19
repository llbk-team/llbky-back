package com.example.demo.newstrend.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.newstrend.entity.NewsSummary;

@Mapper
public interface NewsSummaryDao {
  // 뉴스 요약 저장
  public int insertNewsSummary(NewsSummary newsSummary);

  // URL 중복 체크
  public NewsSummary selectBySourceUrl(String sourceUrl);

  // 뉴스 상세보기
  public NewsSummary selectNewsById(Integer summaryId);

  // 희망 직무로만 뉴스 추천
  public List<NewsSummary> selectNewsByKeyword(String keyword);

  // 희망 직무 + 관심 키워드
  public List<NewsSummary> selectNewsByKeywordList(List<String> keywords);
}
