package com.example.demo.portfolio.dto.request;

import com.example.demo.portfolio.dto.GuideStepData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
}
