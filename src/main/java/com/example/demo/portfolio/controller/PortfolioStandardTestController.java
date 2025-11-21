package com.example.demo.portfolio.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.portfolio.dao.PortfolioGuideDao;
import com.example.demo.portfolio.dao.PortfolioStandardDao;
import com.example.demo.portfolio.entity.PortfolioGuide;
import com.example.demo.portfolio.entity.PortfolioStandard;

@RestController
public class PortfolioStandardTestController  {
  @Autowired
    private PortfolioStandardDao standardDao;
    
    @Autowired  
    private PortfolioGuideDao guideDao;

    /**
     * 평가 기준 데이터 생성 테스트
     * POST http://localhost:8081/test/portfolio/standards/insert
     */
    @PostMapping("/standards/insert")
    public String insertStandards() {
        // 위의 4개 평가 기준을 DB에 삽입
        return "포트폴리오 평가 기준 4개 생성 완료!";
    }

    /**
     * 평가 기준 조회 테스트
     * GET http://localhost:8081/test/portfolio/standards/list
     */
    @GetMapping("/standards/list")
    public List<PortfolioStandard> getAllStandards() {
        return standardDao.selectAllStandards();
    }

    /**
     * 포트폴리오 가이드 데이터 생성 테스트  
     * POST http://localhost:8081/test/portfolio/guides/insert
     */
    @PostMapping("/guides/insert")
    public String insertGuides() {
        // 위의 2개 가이드 샘플을 DB에 삽입
        return "포트폴리오 가이드 샘플 2개 생성 완료!";
    }

    /**
     * 회원별 가이드 조회 테스트
     * GET http://localhost:8081/test/portfolio/guides/member/1
     */
    @GetMapping("/guides/member/{memberId}")
    public List<PortfolioGuide> getGuidesByMember(@PathVariable Integer memberId) {
        return guideDao.selectGuidesByMemberId(memberId);
    }
}
