package com.example.demo.interview.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.interview.entity.InterviewDocument;

@Mapper
public interface InterviewDocumentDao {

  // 면접 세션 생성될 때 관련 서류 저장
  public int insertInterviewDocument(InterviewDocument interviewDocument);

  // 면접 리포트 상세보기 시 연관된 서류 목록 조회
  public List<InterviewDocument> selectAllDocumentsBySessionId(int sessionId);

}
