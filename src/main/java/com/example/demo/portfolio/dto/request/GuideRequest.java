package com.example.demo.portfolio.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GuideRequest {
    /**
     * 가이드 ID
     */
    private Integer guideId;
    
    /**
     * 회원 ID
     */
    private Integer memberId;
    
    /**
     * 평가 기준 ID (어떤 standard로 평가할지)
     */
    private Integer standardId;
    
    /**
     * 현재 단계 (1-5단계)
     */
    private Integer currentStep;
    
    /**
     * 사용자가 입력한 내용
     */
    private String userInput;
    
    /**
     * 입력 필드 타입 (제목, 기간, 목적 등)
     */
    private String inputFieldType;
    
    /**
     * 직무/직군 정보 (AI 코칭 맞춤화용)
     */
    private String jobGroup;
    private String jobRole;
    private Integer careerYears;
}
