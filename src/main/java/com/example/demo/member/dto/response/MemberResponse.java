package com.example.demo.member.dto.response;

import lombok.Data;

@Data
public class MemberResponse {
    private Integer memberId;
    private String member_name;
    private String loginId;
    private String member_password;
    private String member_email;
    private String jobGroup;
    private String jobRole;
    private Integer careerYears;
}
