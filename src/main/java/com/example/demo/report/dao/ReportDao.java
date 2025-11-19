package com.example.demo.report.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.report.dto.Report;

@Mapper
public interface ReportDao {
    int insert(Report report);
    Report findById(@Param("reportId") Long reportId);
    List<Report> findByMemberId(@Param("memberId") Long memberId);
}
