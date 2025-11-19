package com.example.demo.member.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MemberProfile {
    private Long profileId;
    private Long memberId;
    private String coreScores;      // JSONB
    private String memberMentoring; // JSONB
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
