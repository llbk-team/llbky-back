package com.example.demo.member.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.member.dto.MemberProfile;

@Mapper
public interface MemberProfileDao {
    MemberProfile findByMemberId(@Param("memberId") Long memberId);
    int upsert(MemberProfile memberProfile);
}
