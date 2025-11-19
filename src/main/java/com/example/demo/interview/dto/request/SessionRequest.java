package com.example.demo.interview.dto.request;

import java.util.List;

import lombok.Data;

// 모의면접 시작 시 면접세션 생성 요청
@Data
public class SessionRequest {
  private Integer memberId; //사용자 ID
  private String type;  //면접 유형(종합/직무)
  private String targetCompany; //희망 기업
  private List<String> documents;  //선택한 서류들
  private List<String> aiQuestions; //AI가 생성한 질문들
  private List<String> customQuestions; //사용자가 추가한 질문들
}
