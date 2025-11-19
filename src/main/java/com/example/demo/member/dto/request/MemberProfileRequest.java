package com.example.demo.member.dto.request;

import lombok.Data;

@Data
public class MemberProfileRequest {
    // 프론트에서 Chart.js용 radar 데이터와 멘토링 내용을 JSON으로 만들어 전송한다고 가정
    private String coreScoresJson;
    private String memberMentoringJson;
}
