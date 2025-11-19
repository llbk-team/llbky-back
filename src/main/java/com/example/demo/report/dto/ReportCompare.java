package com.example.demo.report.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ReportCompare {
    private Long compareId;
    private Long reportId1;
    private Long reportId2;
    private String content; // JSONB
    private LocalDateTime createdAt;
}
