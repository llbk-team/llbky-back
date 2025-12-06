package com.example.demo.portfolio.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.portfolio.dao.PortfolioStandardDao;
import com.example.demo.portfolio.entity.PortfolioStandard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 평가 기준 서비스
 * - 평가 기준 조회 관련 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioStandardService {

    @Autowired
    private PortfolioStandardDao portfolioStandardDao;

    /**
     * 모든 평가 기준 조회
     */
    public List<PortfolioStandard> getAllStandards() {
        log.info("모든 평가 기준 조회");
        return portfolioStandardDao.selectAllStandards();
    }

    /**
     * ID로 평가 기준 조회
     */
    public PortfolioStandard getStandardById(int standardId) {
        log.info("평가 기준 조회 - standardId: {}", standardId);
        return portfolioStandardDao.selectStandardById(standardId);
    }

    /**
     * 직군 + 직무로 평가 기준 조회
     */
    public List<PortfolioStandard> getStandardsByJobInfo(String jobGroup, String jobRole) {
        log.info("직군/직무별 평가 기준 조회 - jobGroup: {}, jobRole: {}", jobGroup, jobRole);
        
       
        if (jobGroup == null || jobGroup.trim().isEmpty()) {
            log.warn("직군 정보가 비어있음 - 전체 기준 반환");
            return getAllStandards();
        }
        
        if (jobRole == null || jobRole.trim().isEmpty()) {
            log.warn("직무 정보가 비어있음 - 직군별 기준 반환");
            return getStandardsByJobGroup(jobGroup);
        }
        
        List<PortfolioStandard> result = portfolioStandardDao.selectStandardsByJobInfo(jobGroup, jobRole);
        log.info("조회 결과 - 개수: {}", result != null ? result.size() : 0);
        
        return result;
    }

    /**
     * 직군으로만 평가 기준 조회 (개선됨)
     */
    public List<PortfolioStandard> getStandardsByJobGroup(String jobGroup) {
        log.info("직군별 평가 기준 조회 - jobGroup: {}", jobGroup);
        
        // ⭐ null 체크 추가
        if (jobGroup == null || jobGroup.trim().isEmpty()) {
            log.warn("직군 정보가 비어있음 - 전체 기준 반환");
            return getAllStandards();
        }
        
        List<PortfolioStandard> result = portfolioStandardDao.selectStandardsByJobGroup(jobGroup);
        log.info("직군별 조회 결과 - 개수: {}", result != null ? result.size() : 0);
        
        return result;
    }

    /**
     * 평가 기준명으로 조회
     */
    public PortfolioStandard getStandardByName(String standardName) {
        log.info("평가 기준명으로 조회 - name: {}", standardName);
        return portfolioStandardDao.selectStandardByName(standardName);
    }

    /**
     * 최소 가중치 이상의 평가 기준 조회
     */
    public List<PortfolioStandard> getStandardsByMinWeight(int minWeight) {
        log.info("최소 가중치 이상 평가 기준 조회 - minWeight: {}", minWeight);
        return portfolioStandardDao.selectStandardsByMinWeight(minWeight);
    }

    /**
     * 평가 기준 총 개수 조회
     */
    public int getStandardsCount() {
        log.info("평가 기준 총 개수 조회");
        return portfolioStandardDao.countStandards();
    }

    /**
     * 평가 기준명 중복 체크
     */
    public boolean isStandardNameExists(String standardName) {
        log.info("평가 기준명 중복 체크 - name: {}", standardName);
        return portfolioStandardDao.existsStandardByName(standardName);
    }
}
