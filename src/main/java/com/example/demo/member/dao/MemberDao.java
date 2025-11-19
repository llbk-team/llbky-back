package com.example.demo.member.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.member.dto.Member;

@Mapper
public interface MemberDao {
    int insert(Member member);
    Member findById(@Param("memberId") Long memberId);
    Member findByLoginId(@Param("loginId") String loginId);
    int update(Member member);
}
