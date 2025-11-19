package com.example.demo.member.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Member {
    private Long memberId;
    private String name;
    private String loginId;
    private String password;
    private String email;
    private String jobGroup;
    private String jobRole;
    private Integer careerYears;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
