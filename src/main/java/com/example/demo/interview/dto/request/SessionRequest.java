package com.example.demo.interview.dto.request;

import java.util.List;

import lombok.Data;

// 모의면접 시작 시 면접세션 생성 요청
@Data
public class SessionRequest {
  private Integer memberId; //사용자 ID
  private String type;  //면접 유형(종합/직무)
  private String targetCompany; //희망 기업
  private List<String> keywords;  //선택된 키워드들

  // private String documentFileName;  //서류 파일 이름
  // private String documentFileType;  //서류 파일 타입
  // private byte[] documentFileData;  //서류 바이너리 데이터

  private List<String> aiQuestions; //AI가 생성한 질문들
  private List<String> customQuestions; //사용자가 추가한 질문들
}
