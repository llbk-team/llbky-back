package com.example.demo.learning.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.ai.learning.MemoCheckAgent;
import com.example.demo.ai.learning.RewriteMemoAgent;
import com.example.demo.learning.dao.LearningDayDao;
import com.example.demo.learning.dto.response.MemoCheckResponse;
import com.example.demo.learning.entity.LearningDay;

@Service
public class LearningDayService {

    // DAO
    @Autowired
    private LearningDayDao learningDayDao;

    // AI Agent
    @Autowired
    private MemoCheckAgent memoCheckAgent;
    @Autowired
    private RewriteMemoAgent rewriteMemoAgent;

    // 일차 생성
    public int createDay(LearningDay learningDay) {
        return learningDayDao.insert(learningDay);
    }

    // dayId로 상세 조회
    public LearningDay getDayById(int dayId) {
        return learningDayDao.selectedByDayId(dayId);
    }

    // 특정 주차의 전체 일차 리스트 조회
    public List<LearningDay> getDayListByWeekId(int weekId) {
        return learningDayDao.selectListByWeekId(weekId);
    }

    // 특정 학습 플랜의 전체 일차 목록 조회
    public List<LearningDay> getDayListByLearningId(int learningId) {
        return learningDayDao.selectListByLearningId(learningId);
    }

    // 주차 ID + 일차 번호로 조회
    public LearningDay getDayByWeekIdAndDayNumber(int weekId, int dayNumber) {
        return learningDayDao.selectByWeekIdAndDayNumber(weekId, dayNumber);
    }

    // 일일 학습 메모 저장
    public LearningDay submitMemo(int dayId, String learningDaySummary) {
        
        // 1. 일일 학습 정보 조회
        LearningDay day = learningDayDao.selectedByDayId(dayId);
        if (day == null) {
            throw new RuntimeException("Day not found");
        }

        // 2. AI 검증
        MemoCheckResponse checkResult = memoCheckAgent.execute(day, learningDaySummary);

        // 3. AI 메모 정리
        return rewriteMemoAgent.execute(day, learningDaySummary, checkResult);
    }

    // 일차 업데이트
    public int updateDay(LearningDay learningDay) {
        return learningDayDao.update(learningDay);
    }
}
