package com.example.demo.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 가이드 단계 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuideStepData {
    
    private Integer stepNumber;//단계 번호
    private String stepTitle;//단계 제목
    private Integer stepProgress;//단계별 진행률 (0-100%)
    private List<GuideItemData> items;// 단계 내 항목들
}
