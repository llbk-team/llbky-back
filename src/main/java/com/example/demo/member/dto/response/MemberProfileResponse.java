package com.example.demo.member.dto.response;

import com.example.demo.member.dto.ai.CoreScores;
import com.example.demo.member.dto.ai.MentoringSummary;

import lombok.Data;

@Data
public class MemberProfileResponse {
    private Long memberId;
    private CoreScores coreScores;
    private MentoringSummary mentoring;
}
