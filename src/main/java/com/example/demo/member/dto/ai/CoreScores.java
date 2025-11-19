package com.example.demo.member.dto.ai;

import lombok.Data;

@Data
public class CoreScores {
    private String[] labels;
    private int[] scores;
}
