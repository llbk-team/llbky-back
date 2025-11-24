package com.example.demo.portfolio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 가이드 진행상황 응답 DTO
 * 저장/조회 결과를 프론트엔드에 반환
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuideProgressResponse {
    /**
     * 성공 여부
     */
    private boolean success;
    
    /**
     * 응답 메시지
     */
    private String message;
    
    /**
     * 가이드 ID
     */
    private Integer guideId;
    
    /**
     * 회원 ID
     */
    private Integer memberId;
    
    /**
     * 전체 진행률 (0-100%)
     */
    private Integer completionPercentage;
    
    /**
     * 현재 단계 번호
     */
    private Integer currentStep;
    
    /**
     * 전체 단계 수
     */
    private Integer totalSteps;
    
    /**
     * 가이드 내용 (JSONB에서 파싱된 맵)
     */
    private Map<String, Object> guideContent;
    
    /**
     * 단계별 진행상황
     */
    private List<StepProgress> stepProgress;
    
    /**
     * 마지막 업데이트 시간
     */
    private String lastUpdated;
    
    /**
     * 단계별 진행상황 상세 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepProgress {
        /**
         * 단계 번호
         */
        private Integer stepNumber;
        
        /**
         * 단계 제목
         */
        private String stepTitle;
        
        /**
         * 단계 진행률 (0-100%)
         */
        private Integer progress;
        
        /**
         * 완료된 항목 수
         */
        private Integer completedItems;
        
        /**
         * 전체 항목 수
         */
        private Integer totalItems;
    }
}
