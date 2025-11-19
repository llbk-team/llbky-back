package com.example.demo.coverletter.dto;

import lombok.Data;

@Data
public class CoverLetterRequest {
  private Integer coverLetterId;  //ID
  private String supportMotive; //지원 동기
  private String growthExperience;  //성장 배경
  private String jobCapability;  //직무 역량
  private String futurePlan;  //입사 후 포부
}
