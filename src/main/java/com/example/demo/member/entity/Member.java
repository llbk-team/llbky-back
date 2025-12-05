package com.example.demo.member.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Member {
    private Integer memberId;
    private String member_name;
    private String loginId;
    private String member_password;
    private String member_email;
    private String jobGroup;
    private String jobRole;
    private Integer careerYears;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
