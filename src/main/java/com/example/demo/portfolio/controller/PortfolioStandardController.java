package com.example.demo.portfolio.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.portfolio.entity.PortfolioStandard;
import com.example.demo.portfolio.service.PortfolioStandardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 평가 기준 API 컨트롤러
 * - 프론트엔드에서 평가 기준 정보를 조회하는 API 제공
 */
@RestController
@RequestMapping("/portfolio-standard")
@RequiredArgsConstructor
@Slf4j
public class PortfolioStandardController {

    @Autowired
    private PortfolioStandardService portfolioStandardService;

    /**
     * 모든 평가 기준 조회
     * GET /portfolio-standard
     */
    @GetMapping
    public ResponseEntity<List<PortfolioStandard>> getAllStandards() {
        log.info("모든 평가 기준 조회 요청");
        List<PortfolioStandard> standards = portfolioStandardService.getAllStandards();
        return ResponseEntity.ok(standards);
    }

    /**
     * 특정 평가 기준 조회 (ID로)
     * GET /portfolio-standard/{standardId}
     */
    @GetMapping("/{standardId}")
    public ResponseEntity<PortfolioStandard> getStandardById(@PathVariable int standardId) {
        log.info("평가 기준 조회 요청 - standardId: {}", standardId);
        PortfolioStandard standard = portfolioStandardService.getStandardById(standardId);
        
        if (standard == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(standard);
    }

    /**
     * 직군 + 직무로 평가 기준 조회
     * GET /portfolio-standard/by-job?jobGroup=백엔드&jobRole=백엔드 
     */
    @GetMapping("/by-job")
    public ResponseEntity<List<PortfolioStandard>> getStandardsByJob(
            @RequestParam String jobGroup,
            @RequestParam String jobRole) {
        log.info("직군/직무별 평가 기준 조회 - jobGroup: {}, jobRole: {}", jobGroup, jobRole);
        
        List<PortfolioStandard> standards = portfolioStandardService.getStandardsByJobInfo(jobGroup, jobRole);
        return ResponseEntity.ok(standards);
    }

    /**
     * 직군으로만 평가 기준 조회
     * GET /portfolio-standard/by-group?jobGroup=백엔드
     */
    @GetMapping("/by-group")
    public ResponseEntity<List<PortfolioStandard>> getStandardsByJobGroup(
            @RequestParam String jobGroup) {
        log.info("직군별 평가 기준 조회 - jobGroup: {}", jobGroup);
        
        List<PortfolioStandard> standards = portfolioStandardService.getStandardsByJobGroup(jobGroup);
        return ResponseEntity.ok(standards);
    }

    /**
     * 평가 기준명으로 조회
     * GET /portfolio-standard/by-name?name=프로젝트 개요
     */
    @GetMapping("/by-name")
    public ResponseEntity<PortfolioStandard> getStandardByName(@RequestParam String name) {
        log.info("평가 기준명으로 조회 - name: {}", name);
        
        PortfolioStandard standard = portfolioStandardService.getStandardByName(name);
        
        if (standard == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(standard);
    }

    /**
     * 최소 가중치 이상의 평가 기준 조회
     * GET /portfolio-standard/by-weight?minWeight=15
     */
    @GetMapping("/by-weight")
    public ResponseEntity<List<PortfolioStandard>> getStandardsByMinWeight(
            @RequestParam int minWeight) {
        log.info("최소 가중치 이상 평가 기준 조회 - minWeight: {}", minWeight);
        
        List<PortfolioStandard> standards = portfolioStandardService.getStandardsByMinWeight(minWeight);
        return ResponseEntity.ok(standards);
    }

    /**
     * 평가 기준 총 개수 조회
     * GET /portfolio-standard/count
     */
    @GetMapping("/count")
    public ResponseEntity<Integer> getStandardsCount() {
        log.info("평가 기준 총 개수 조회 요청");
        int count = portfolioStandardService.getStandardsCount();
        return ResponseEntity.ok(count);
    }
}
