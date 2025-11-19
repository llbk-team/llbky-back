package com.example.demo.member.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MemberLog {
    private Long logId;
    private Long memberId;
    private String logType; // 예: RESUME_CREATE, REPORT_UPDATE 등
    private LocalDate logDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
