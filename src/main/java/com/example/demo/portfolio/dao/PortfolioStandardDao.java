package com.example.demo.portfolio.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.portfolio.entity.PortfolioStandard;

@Mapper
public interface PortfolioStandardDao {

    // 새로운 평가 기준을 등록하는 메소드
    public int insertStandard(PortfolioStandard standard);

    // standardId(기준 ID)로 하나의 평가 기준을 조회하는 메소드
    public PortfolioStandard selectStandardById(int standardId);

    // 등록된 모든 평가 기준을 조회하는 메소드
    public List<PortfolioStandard> selectAllStandards();

    // 직군 + 직무로 평가 기준을 조회하는 메소드 (일반 기준 포함)
    public List<PortfolioStandard> selectStandardsByJobInfo(String jobGroup, String jobRole);

    // 직군별 기준만 조회하는 메소드 (직무는 무시)
    public List<PortfolioStandard> selectStandardsByJobGroup(String jobGroup);

    // 기준명으로 특정 평가 기준을 조회하는 메소드
    public PortfolioStandard selectStandardByName(String standardName);

    // 평가 기준 내용을 수정하는 메소드
    public int updateStandard(PortfolioStandard standard);

    // 평가 기준을 삭제하는 메소드
    public int deleteStandard(int standardId);

    // 전체 평가 기준 개수를 조회하는 메소드
    public int countStandards();

    // 최소 가중치 이상을 가진 평가 기준 목록을 조회하는 메소드
    public List<PortfolioStandard> selectStandardsByMinWeight(int minWeight);

    // 기준명 존재 여부를 확인하는 메소드 (중복 체크)
    public boolean existsStandardByName(String standardName);

    // 평가 기준 여러 건을 일괄 삽입하는 메소드 (jobGroup, jobRole 포함)
    public int insertStandardsBatch(List<PortfolioStandard> standardList);
}
