package com.example.demo.resume.dto.request;

import lombok.Data;

@Data
public class ResumeCoachRequest {
  private int memberId;
  private String section;
  private String content;
}
