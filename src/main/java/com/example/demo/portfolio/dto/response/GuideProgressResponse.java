package com.example.demo.portfolio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.example.demo.portfolio.dto.GuideStepData;

/**
 * 가이드 진행상황 응답 DTO
 * 저장/조회 결과를 프론트엔드에 반환
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuideProgressResponse {
   
    //===== 응답 상태 =====
    private Boolean success;        // 성공 여부
    private String message;         // 응답 메시지
    
    //===== 가이드 기본 정보 =====
    private Integer guideId;        // 가이드 ID
    private Integer memberId;       // 회원 ID
    private String title;           // 가이드 제목
    
    //===== 진행 상태 =====
    private Integer completionPercentage;  // 전체 진행률 (0-100%)
    private Boolean isCompleted;           // 완료 여부
    private Integer currentStep;           // 현재 단계 번호
    private Integer totalSteps;            // 전체 단계 수
    
    private List<GuideStepData> guideContent;//전체 가이드 내용 (단계별 데이터)
    
   
    //===== 시간 정보 =====
    private LocalDateTime lastUpdated;  // 마지막 업데이트 시간
    
    /**
     * 단계별 진행상황 요약 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepProgress {
        private Integer stepNumber;      // 단계 번호
        private String stepTitle;        // 단계 제목
        private Integer progress;        // 단계 진행률 (0-100%)
        private Integer completedItems;  // 완료된 항목 수
        private Integer totalItems;      // 전체 항목 수
    }
}
