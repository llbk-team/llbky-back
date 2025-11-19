package com.example.demo.report.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.report.dto.ReportCompare;

@Mapper
public interface ReportCompareDao {
    ReportCompare findByPair(@Param("reportId1") Long reportId1, @Param("reportId2") Long reportId2);
    int insert(ReportCompare reportCompare);
}
