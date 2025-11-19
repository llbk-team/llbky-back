package com.example.demo.report.dto.response;

import lombok.Data;

@Data
public class ReportResponse {
    private Long reportId;
    private Long memberId;
    private String title;
    private String memo;
    private String snapshotJson;
    private String reportSummary;
    private String createdAt; // 문자열 포맷으로 변환해서 주는 것도 가능
}
