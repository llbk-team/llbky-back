package com.example.demo.portfolio.dto;

import lombok.Data;

/**
 * 가이드 항목 데이터
 */
@Data
public class GuideItemData {
   
    private String title;//항목 제목

    private String content;//사용자가 입력한 내용

    private String status;//항목 상태 ("미작성", "작성 중", "완료")

    private GuideResult feedback;//AI 피드백 (선택사항)
}
