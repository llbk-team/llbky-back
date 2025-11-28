package com.example.demo.portfolio.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.portfolio.dto.GuideStepData;

import lombok.Data;

/**
 * 가이드 진행상황 응답 DTO
 * 저장/조회 결과를 프론트엔드에 반환
 */
@Data
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

    //===== AI 피드백 정보 =====
    private String coachingMessage;              // AI 코칭 메시지
    private Integer appropriatenessScore;        // 입력 내용 적절성 점수 (1-10)
    private List<String> suggestions;            // 개선 제안사항
    private List<String> examples;               // 예시 문장/내용
    private String nextStepGuide;                // 다음 단계 가이드
    private String errorMessage;                 // 오류 메시지 (있는 경우)

    //===== 시간 정보 =====
    private LocalDateTime lastUpdated;  // 마지막 업데이트 시간
    

    /**
     * 단계별 진행상황 요약 정보
     */
    @Data
    public static class StepProgress {
        private Integer stepNumber;      // 단계 번호
        private String stepTitle;        // 단계 제목
        private Integer progress;        // 단계 진행률 (0-100%)
        private Integer completedItems;  // 완료된 항목 수
        private Integer totalItems;      // 전체 항목 수
    }
}
