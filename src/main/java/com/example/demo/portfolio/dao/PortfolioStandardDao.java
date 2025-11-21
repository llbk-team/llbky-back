package com.example.demo.portfolio.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.portfolio.entity.PortfolioStandard;

@Mapper
public interface PortfolioStandardDao {


    public int insertStandard(PortfolioStandard standard);//평가 기준 템플릿 생성

    public PortfolioStandard selectStandardById(@Param("standardId") int standardId);//평가 기준 ID로 조회

    public List<PortfolioStandard> selectAllStandards();// 모든 평가 기준 조회

    public PortfolioStandard selectStandardByName(@Param("standardName") String standardName);//평가 기준명으로 조회

    public int updateStandard(PortfolioStandard standard);//평가 기준 수정

    public int deleteStandard(@Param("standardId") int standardId);//평가 기준 삭제

    public int countStandards();//평가 기준 개수 조회

    public List<PortfolioStandard> selectStandardsByMinWeight(@Param("minWeight") int minWeight);//특정 가중치 이상의 평가 기준 조회
   
    public boolean existsStandardByName(@Param("standardName") String standardName);//평가 기준명 중복 체크

    public int insertStandardsBatch(List<PortfolioStandard> standards);//평가 기준 일괄 삽입

}
