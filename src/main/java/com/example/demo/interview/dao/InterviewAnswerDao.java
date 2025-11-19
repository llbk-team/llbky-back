package com.example.demo.interview.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.interview.entity.InterviewAnswer;

@Mapper
public interface InterviewAnswerDao {

  // 답변 제출
  public int insertInterviewAnswer(InterviewAnswer interviewAnswer);

  // 답변 다시 제출
  public int updateInterviewAnswer(InterviewAnswer interviewAnswer);

  // 면접 질문 선택 시 연관된 답변 조회
  public InterviewAnswer selectInterviewAnswerByQuestionId(int questionId);

}
