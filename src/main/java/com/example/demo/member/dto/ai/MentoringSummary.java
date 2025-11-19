package com.example.demo.member.dto.ai;

import java.util.List;

import lombok.Data;

@Data
public class MentoringSummary {
    private String overview;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> thisWeekAction;
    private List<String> nextWeekStudy;
}
