package com.example.demo.portfolio.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.example.demo.portfolio.dto.PortfolioGuideResult;

/**
 * 전체 가이드 진행상황 저장 요청 DTO
 * 사용자가 "진행상황 저장" 버튼을 클릭했을 때 모든 내용을 저장
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuideProgressSaveRequest {
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
     * 현재 진행 중인 단계 번호
     */
    private Integer currentStep;
    
    /**
     * 전체 가이드 내용 (모든 단계와 항목)
     */
    private List<GuideStepData> guideContent;
    
    /**
     * 각 단계의 데이터
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuideStepData {
        /**
         * 단계 번호
         */
        private Integer stepNumber;
        
        /**
         * 단계 제목
         */
        private String stepTitle;
        
        /**
         * 단계별 진행률 (0-100%)
         */
        private Integer stepProgress;
        
        /**
         * 단계 내 항목들
         */
        private List<GuideItemData> items;
    }
    
    /**
     * 각 항목의 데이터
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuideItemData {
        /**
         * 항목 제목
         */
        private String title;
        
        /**
         * 사용자가 입력한 내용
         */
        private String content;
        
        /**
         * 항목 상태 ("미작성", "작성 중", "완료")
         */
        private String status;
        
        /**
         * AI 피드백 (선택사항)
         */
        private PortfolioGuideResult feedback;
    }
}
