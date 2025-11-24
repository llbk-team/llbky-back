package com.example.demo.portfolio.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.portfolio.dto.response.PortfolioListResponse;
import com.example.demo.portfolio.entity.Portfolio;

@Mapper
public interface PortfolioDao {

  public int insertPortfolio(Portfolio portfolio); //포트폴리오 생성 

  public Portfolio selectPortfolioById(Integer portfolioId); // 포트폴리오 조회
 
  public List<PortfolioListResponse> selectPortfoliosByMemberId(Integer memberId); // 회원별 포트폴리오 목록 조회

  public int updatePortfolioFeedback(@Param("portfolioId") Integer portfolioId, @Param("portfolioFeedback") String protfolioFeedback);
  public int updatePortfolioPageCount(@Param("portfolioId") Integer portfolioId, @Param("pageCount") int pageCount);

  public int deletePortfolio(Integer portfolioId); //포트폴리오 삭제

}
