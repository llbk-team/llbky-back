package com.example.demo.newstrend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.ai.newstrend.GrowthAnalysisAgent;
import com.example.demo.ai.newstrend.JobRelatedInsightAgent;
import com.example.demo.newstrend.dao.JobInsightDao;
import com.example.demo.newstrend.dto.response.GrowthAnalysisResponse;
import com.example.demo.newstrend.dto.response.JobInsightListResponse;
import com.example.demo.newstrend.entity.JobInsight;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JobInsightService {
  @Autowired
  private JobRelatedInsightAgent jobRelatedInsightAgent;
  @Autowired
  private GrowthAnalysisAgent growthAnalysisAgent;
  @Autowired
  private JobInsightDao jobInsightDao;
  @Autowired
  private ObjectMapper mapper;

  public JobInsight createJobInsight(int memberId) throws Exception {
    // 성장 제안 생성 에이전트 호출
    GrowthAnalysisResponse growthAdviceJson = growthAnalysisAgent.generateGrowthAdvice(memberId);

    // 직무 인사이트 카드 생성하는 에이전트 호출
    JobInsightListResponse cards = jobRelatedInsightAgent.relatedJobs(memberId);

    // DB 저장
    JobInsight entity = new JobInsight();
    entity.setMemberId(memberId);
    entity.setAnalysisJson(mapper.writeValueAsString(growthAdviceJson));
    entity.setRelatedJobsJson(mapper.writeValueAsString(cards));

    jobInsightDao.insertJobInsight(entity);

    return entity;
  }

  // 최근 인사이트 1개 조회
  public JobInsight getLatestInsight(int memberId) {
    return jobInsightDao.selectLatestJobInsight(memberId);
  }

  // 조회 후 없으면 생성
  public JobInsight getOrCreateInsight(int memberId) throws Exception {
    JobInsight latest = jobInsightDao.selectLatestJobInsight(memberId);
    if (latest != null) {
      return latest;
    }

    return createJobInsight(memberId);
  }

  // 키워드 저장 / 삭제시 성장 제안 수정
  public JobInsight modifyGrowthAnalysis(int memberId) throws Exception {
    // 최신 인사이트 조회
    JobInsight latest = getLatestInsight(memberId);
    if (latest == null) {
      return createJobInsight(memberId);
    }

    // 성장 제안 생성 에이전트 호출
    GrowthAnalysisResponse growthAdviceJson = growthAnalysisAgent.generateGrowthAdvice(memberId);

    // INSERT (직무 카드는 이전 값 그대로)
    JobInsight entity = new JobInsight();
    entity.setMemberId(memberId);
    entity.setAnalysisJson(mapper.writeValueAsString(growthAdviceJson));
    entity.setRelatedJobsJson(latest.getRelatedJobsJson());

    jobInsightDao.insertJobInsight(entity);

    return entity;

  }

}
