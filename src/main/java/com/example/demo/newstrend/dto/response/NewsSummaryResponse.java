package com.example.demo.newstrend.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class NewsSummaryResponse {
    private String summary;
    private String detailSummary;  
    private String sentiment;
    private Integer trustScore;
    private Boolean biasDetected;
    private String biasType;
    private String category;
    
}
