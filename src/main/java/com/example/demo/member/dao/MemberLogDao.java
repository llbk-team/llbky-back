package com.example.demo.member.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.member.dto.MemberLog;

@Mapper
public interface MemberLogDao {
    int insert(MemberLog memberLog);
}
