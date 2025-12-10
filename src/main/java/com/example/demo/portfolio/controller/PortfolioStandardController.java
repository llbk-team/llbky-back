package com.example.demo.portfolio.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
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
@Slf4j
public class PortfolioStandardController {

    @Autowired
    private PortfolioStandardService portfolioStandardService;

    @Autowired 
    private MemberDao memberDao;
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
     * 직군 + 직무로 평가 기준 조회 (또는 memberId로 자동 조회)
     * GET /portfolio-standard/by-job?memberId=1
     * GET /portfolio-standard/by-job?jobGroup=백엔드&jobRole=백엔드 
     */
    @GetMapping("/by-job")
    public ResponseEntity<List<PortfolioStandard>> getStandardsByJob(
            @RequestParam(required = false) Integer memberId,    
            @RequestParam(required = false) String jobGroup,
            @RequestParam(required = false) String jobRole) {
        
        // memberId가 있으면 회원 정보에서 직군/직무 조회
        if (memberId != null) {
            log.info("회원별 평가 기준 조회 - memberId: {}", memberId);
            Member member = memberDao.findById(memberId);
            
            if (member == null) {
                log.warn("회원을 찾을 수 없음 - memberId: {}", memberId);
                return ResponseEntity.notFound().build();
            }
            
            // ⭐ 핵심 수정: null 체크 및 기본값 설정
            String memberJobGroup = member.getJobGroup();
            String memberJobRole = member.getJobRole();
            
            log.info("회원 직군/직무 정보 - jobGroup: {}, jobRole: {}", memberJobGroup, memberJobRole);
            
            if (memberJobGroup == null || memberJobGroup.trim().isEmpty()) {
                log.warn("회원의 직군 정보가 없음 - memberId: {}. 전체 기준을 조회합니다.", memberId);
                List<PortfolioStandard> allStandards = portfolioStandardService.getAllStandards();
                return ResponseEntity.ok(allStandards);
            }
            
            if (memberJobRole == null || memberJobRole.trim().isEmpty()) {
                log.warn("회원의 직무 정보가 없음 - memberId: {}. 직군별 기준을 조회합니다.", memberId);
                List<PortfolioStandard> standards = portfolioStandardService.getStandardsByJobGroup(memberJobGroup);
                return ResponseEntity.ok(standards);
            }
            
            // 직군/직무 정보가 모두 있는 경우
            List<PortfolioStandard> standards = portfolioStandardService.getStandardsByJobInfo(
                memberJobGroup, 
                memberJobRole
            );
            
            // 결과가 비어있으면 직군별로 재시도
            if (standards == null || standards.isEmpty()) {
                log.warn("직군/직무별 기준을 찾을 수 없음 - jobGroup: {}, jobRole: {}. 직군별 기준을 조회합니다.", 
                        memberJobGroup, memberJobRole);
                standards = portfolioStandardService.getStandardsByJobGroup(memberJobGroup);
            }
            
            // 여전히 비어있으면 전체 기준 반환
            if (standards == null || standards.isEmpty()) {
                log.warn("직군별 기준도 찾을 수 없음 - jobGroup: {}. 전체 기준을 조회합니다.", memberJobGroup);
                standards = portfolioStandardService.getAllStandards();
            }
            
            return ResponseEntity.ok(standards);
        }
        
        // jobGroup과 jobRole이 있으면 직접 조회
        if (jobGroup != null && jobRole != null) {
            log.info("직군/직무별 평가 기준 조회 - jobGroup: {}, jobRole: {}", jobGroup, jobRole);
            List<PortfolioStandard> standards = portfolioStandardService.getStandardsByJobInfo(jobGroup, jobRole);
            
            if (standards == null || standards.isEmpty()) {
                log.warn("직군/직무별 기준을 찾을 수 없음 - jobGroup: {}, jobRole: {}", jobGroup, jobRole);
                return ResponseEntity.noContent().build();
            }
            
            return ResponseEntity.ok(standards);
        }
        
        // jobGroup만 있으면 직군별 조회
        if (jobGroup != null) {
            log.info("직군별 평가 기준 조회 - jobGroup: {}", jobGroup);
            List<PortfolioStandard> standards = portfolioStandardService.getStandardsByJobGroup(jobGroup);
            return ResponseEntity.ok(standards);
        }
        
        // 파라미터가 하나도 없으면 Bad Request
        log.warn("필수 파라미터 누락 - memberId 또는 jobGroup/jobRole 필요");
        return ResponseEntity.badRequest().build();
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
