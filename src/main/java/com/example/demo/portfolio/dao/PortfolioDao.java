package com.example.demo.portfolio.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.portfolio.entity.Portfolio;

@Mapper
public interface PortfolioDao {

  public int insertPortfolio(Portfolio portfolio);//포트폴리오 생성 

  public Portfolio selectPortfolioById(Integer portfolioId); // 포트폴리오 조회
 
  public List<Portfolio> selectPortfoliosByMemberId(Integer memberId);// 회원별 포트폴리오 목록 조회

  // public List<Portfolio> selectAllPortfolios();//모든 포트폴리오 조회

  // public List<Portfolio> selectPortfoliosByTitle(String title);

  public int updatePortfolio(Portfolio portfolio);//포트폴리오 기본 정보 수정


  /*//AI 피드백 업데이트 (JSONB 컬럼)
     @param portfolioId 포트폴리오 ID
   * @param feedback AI 피드백 JSON 데이터
   * @return 수정된 행 수
     */
  public int updatePortfolioFeedback(
    @Param("portfolioId") Integer portfolioId,
    @Param("feedback") String feedback);//AI 피드백 업데이트 (JSONB 컬럼)
  

  public int updatePortfolioTimestamp(Integer portfolioId);//포트폴리오 updated_at 갱신

  public int deletePortfolio(Integer portfolioId);//포트폴리오 삭제

  public int deletePortfoliosByMemberId(Integer memberId);

  public int countAllPortfolios();

  public int countPortfoliosByMemberId(Integer memberId);

  public List<Portfolio> selectRecentPortfolios(Integer limit);
}
