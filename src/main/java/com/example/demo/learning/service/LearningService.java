package com.example.demo.learning.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ai.learning.CreateRoadmapAgent;
import com.example.demo.learning.dao.LearningDao;
import com.example.demo.learning.dao.LearningDayDao;
import com.example.demo.learning.dao.LearningWeekDao;
import com.example.demo.learning.dto.request.RoadmapRequest;
import com.example.demo.learning.dto.response.AiCreateDayResponse;
import com.example.demo.learning.dto.response.AiCreateRoadmapResponse;
import com.example.demo.learning.dto.response.AiCreateWeekResponse;
import com.example.demo.learning.dto.response.LearningResponse;
import com.example.demo.learning.entity.Learning;
import com.example.demo.learning.entity.LearningDay;
import com.example.demo.learning.entity.LearningWeek;
import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.dto.Member;

@Service
public class LearningService {

    @Autowired
    private LearningDao learningDao;
    @Autowired
    private LearningWeekDao learningWeekDao;
    @Autowired
    private LearningDayDao learningDayDao;
    @Autowired
    private MemberDao memberDao;

    @Autowired
    private CreateRoadmapAgent createRoadmapAgent;

    // 학습 로드맵 생성
    public AiCreateRoadmapResponse createLearning(Integer memberId, List<String> purposes, List<String> skills, int studyHours) {
       
        Member member = memberDao.findById(memberId);
        
        RoadmapRequest request = new RoadmapRequest();
        request.setMemberId(memberId);
        request.setJobRole(member.getJobRole());
        request.setPurposes(purposes);
        request.setSkills(skills);
        request.setStudyHours(studyHours);

        return createRoadmapAgent.generateRoadmap(request);
    }

    // 학습 로드맵 저장
    @Transactional
    public AiCreateRoadmapResponse saveRoadmap(AiCreateRoadmapResponse roadmap) {
        // 1. Learning 저장
        Learning learning = new Learning();
        learning.setMemberId(roadmap.getMemberId());
        learning.setTitle(roadmap.getTitle());
        learning.setStatus("진행중");
        
        learningDao.insert(learning);
        Integer learningId = learning.getLearningId();

        // 2. Week 저장
        int weekNum = 1;
        for (AiCreateWeekResponse weekData : roadmap.getWeeks()) {
            LearningWeek week = new LearningWeek();
            week.setLearningId(learningId);
            week.setWeekNumber(weekNum++);
            week.setTitle(weekData.getTitle());
            week.setStatus("예정");
            week.setLearningWeekSummary(weekData.getLearningWeekSummary());

            learningWeekDao.insert(week);
            Integer weekId = week.getWeekId();


            // 3. Day 저장
            int dayNum = 1;
            for (AiCreateDayResponse dayData : weekData.getDays()) {
                LearningDay day = new LearningDay();
                day.setWeekId(weekId);
                day.setDayNumber(dayNum++);
                day.setTitle(dayData.getTitle());
                day.setContent(dayData.getContent());
                day.setStatus("예정");

                learningDayDao.insert(day);
            }
        }

        return roadmap;
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
    public List<LearningResponse> getLearningListByStatus(int memberId, String status) {
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
