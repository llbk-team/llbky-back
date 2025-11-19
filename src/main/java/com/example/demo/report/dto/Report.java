package com.example.demo.report.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Report {
    private Long reportId;
    private Long memberId;
    private String title;
    private String memo;
    private String snapshotJson;  // Chart.js용 JSON
    private String reportSummary; // LLM 요약 JSON
    private LocalDateTime createdAt;
}
