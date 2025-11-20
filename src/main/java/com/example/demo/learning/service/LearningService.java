package com.example.demo.learning.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.learning.dao.LearningDao;
import com.example.demo.learning.entity.Learning;

@Service
public class LearningService {

    @Autowired
    private LearningDao learningDao;

    // 학습 로드맵 생성
    public int createLearning(Learning learning) {
        return learningDao.insert(learning);
    }

    // 학습 ID로 상세 조회
    public Learning getLearningById(int learningId) {
        return learningDao.selectedByLearningId(learningId);
    }

    // 사용자가 가진 전체 학습 리스트 조회
    public List<Learning> getLearningListByMember(int memberId) {
        return learningDao.selectListByMemberId(memberId);
    }

    // 상태(학습중/완료 등)로 리스트 조회
    public List<Learning> getLearningListByStatus(int memberId, String status) {
        return learningDao.selectListByStatus(memberId, status);
    }

    // 사용자별 학습 개수 조회
    public int getLearningCount(int memberId) {
        return learningDao.countByMemberId(memberId);
    }

    // 학습 상태 또는 기타 정보 업데이트
    public int updateLearning(Learning learning) {
        return learningDao.update(learning);
    }
}
