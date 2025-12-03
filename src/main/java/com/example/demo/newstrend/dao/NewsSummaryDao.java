package com.example.demo.newstrend.dao;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.newstrend.entity.NewsSummary;

@Mapper
public interface NewsSummaryDao {
  // 뉴스 저장 (Service: saveNewsSummary)
  public int insertNewsSummary(NewsSummary newsSummary);

  // 상세보기 - 단일 뉴스 조회 (Controller: /detail/{summaryId})
  public NewsSummary selectNewsSummaryById(int summaryId);

  // URL 중복 체크용 (Service: saveNewsSummary 내부)
  public NewsSummary selectNewsSummaryBySourceUrl(String sourceUrl);

  // 회원별 최신 뉴스 조회 (Controller: /member/{memberId}/latest)
  public List<NewsSummary> selectLatestNewsByMemberId(
    @Param("memberId") int memberId,
    @Param("limit") int limit);

  // 오늘 뉴스 조회 (Controller: /member/{memberId}/today)
  public List<NewsSummary> selectNewsByMemberAndDate(
    @Param("memberId") int memberId,
    @Param("date") LocalDate date,
    @Param("limit") int limit);

  // 검색창 검색 (memberId 무관, 단일 키워드) - Controller: /search
  List<NewsSummary> searchNewsByKeywordsAndDate(
      @Param("keyword") String keyword,
      @Param("startDate") LocalDate startDate,
      @Param("limit") int limit
  );

  // 피드 조회 (직군 기반 키워드, memberId 필터) - Controller: /member/{memberId}/feed
  List<NewsSummary> findByJobGroupKeywords(
      @Param("keywords") List<String> keywords,
      @Param("memberId") Integer memberId,
      @Param("startDate") LocalDate startDate,
      @Param("limit") int limit
  );


  // 뉴스 삭제
  public int deleteNewsSummary(int summaryId);


  
}
