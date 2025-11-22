package com.example.demo.portfolio.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PortfolioGuideResult {
     
    private boolean success;//코칭 성공 여부

    private String coachingMessage;//AI 코칭 메시지
     
    private Integer appropriatenessScore;//입력 내용 적절성 점수 (1-10)
       
    private List<String> suggestions;//개선 제안사항
  
    private List<String> examples;//예시 문장/내용
   
    private String nextStepGuide;//다음 단계 가이드
    
    private Integer progressPercentage;//현재 진행률 (%)
        
    private Long processingTimeMs;//처리 시간 (밀리초)
    
     private LocalDateTime coachingAt;//코칭 생성 시간
 
    private String errorMessage;//오류 메시지
    
}
