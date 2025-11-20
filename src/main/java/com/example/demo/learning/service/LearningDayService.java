package com.example.demo.learning.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.learning.dao.LearningDayDao;
import com.example.demo.learning.entity.LearningDay;

@Service
public class LearningDayService {

    @Autowired
    private LearningDayDao learningDayDao;

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

    // 일차 업데이트
    public int updateDay(LearningDay learningDay) {
        return learningDayDao.update(learningDay);
    }
}
