package com.example.demo.newstrend.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class NewsCollectRequest {
   
    private List<String> keywords;
    
    private Integer memberId ;  // 기본값
    
    private Integer limitPerKeyword = 5;  // 키워드별 수집 개수
}
