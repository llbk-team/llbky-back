package com.example.demo.newstrend.dao;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.newstrend.dto.response.NewsAnalysisResponse;
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
  public List<NewsSummary> selectLatestNewsByMemberId(
    @Param("memberId") int memberId,
    @Param("limit") int limit);

  // 특정 멤버의 특정 날짜 뉴스 조회
  public List<NewsSummary> selectNewsByMemberAndDate(
    @Param("memberId") int memberId,
    @Param("date") LocalDate date,
    @Param("limit") int limit);

    //검색용 memberId로 검색하는거가 에바
  public List<NewsSummary> findByKeywords(
    @Param("keywords") List<String> keywords,
    @Param("memberId") Integer memberId,
    @Param("startDate") LocalDate startDate,
    @Param("limit") int limit
);

  // 검색창 검색용 (memberId 필터 없음, 단일 키워드)
  List<NewsSummary> searchNewsByKeywordsAndDate(
      @Param("keyword") String keyword,
      @Param("startDate") LocalDate startDate,
      @Param("limit") int limit
  );

  // 기본 피드용 (직군 기반 여러 키워드, memberId 필터링)
  List<NewsSummary> findByJobGroupKeywords(
      @Param("keywords") List<String> keywords,
      @Param("memberId") Integer memberId,
      @Param("startDate") LocalDate startDate,
      @Param("limit") int limit
  );


  // 뉴스 삭제
  public int deleteNewsSummary(int summaryId);


  
}
