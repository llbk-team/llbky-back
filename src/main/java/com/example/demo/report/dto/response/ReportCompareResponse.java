package com.example.demo.report.dto.response;

import lombok.Data;

@Data
public class ReportCompareResponse {
    private Long compareId;
    private Long reportId1;
    private Long reportId2;
    private String content;
}
