package com.example.demo.portfolio.dto.request;

import lombok.Data;

/**
 * 포트폴리오 가이드 생성 요청 DTO
 */
@Data
public class CreatePortfolioGuideRequest {
    private Integer memberId;       // 회원 ID
    private Integer standardId;     // 평가 기준 ID
    private String title;           // 가이드 제목 (optional)
    private Integer totalSteps;     // 전체 단계 수 (기본값: 5)
}
