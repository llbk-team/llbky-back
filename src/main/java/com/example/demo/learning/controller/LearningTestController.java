package com.example.demo.learning.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.learning.dao.LearningDao;
import com.example.demo.learning.dao.LearningDayDao;
import com.example.demo.learning.dao.LearningWeekDao;
import com.example.demo.learning.entity.Learning;
import com.example.demo.learning.entity.LearningDay;
import com.example.demo.learning.entity.LearningWeek;

@RestController
@RequestMapping("/test/learning")
public class LearningTestController {

    @Autowired
    private LearningDao learningDao;

    @Autowired
    private LearningWeekDao learningWeekDao;

    @Autowired
    private LearningDayDao learningDayDao;

    // ============================================================
    // ✔ Learning
    // ============================================================

    // CREATE
    @PostMapping("/create")
    public String createLearning() {
        Learning l = new Learning();
        l.setMemberId(1);
        l.setTitle("테스트 학습 플랜");
        l.setStatus("학습중");

        learningDao.insert(l);
        return "Learning 생성 완료! PK = " + l.getLearningId();
    }

    // DETAIL
    @GetMapping("/detail/{learningId}")
    public Learning getLearning(@PathVariable int learningId) {
        return learningDao.selectedByLearningId(learningId);
    }

    // LIST BY MEMBER
    @GetMapping("/member/{memberId}")
    public List<Learning> listByMember(@PathVariable int memberId) {
        return learningDao.selectListByMemberId(memberId);
    }

    // LIST BY STATUS
    @GetMapping("/member/{memberId}/status/{statusCode}")
    public List<Learning> listByStatus(@PathVariable("memberId") int memberId, @PathVariable("statusCode") int statusCode) {
        String status = null;

        switch (statusCode) {
            case 1 -> status = "예정";
            case 2 -> status = "학습중";
            case 3 -> status = "완료";
            default -> {
                return List.of(); // 혹은 null 반환
            }
        }

        return learningDao.selectListByStatus(memberId, status);
    }

    // COUNT
    @GetMapping("/member/{memberId}/count")
    public int countByMember(@PathVariable int memberId) {
        return learningDao.countByMemberId(memberId);
    }

    // UPDATE
    @PutMapping("/update")
    public String updateLearning(@RequestBody Learning learning) {
        learningDao.update(learning);
        return "Learning 수정 완료";
    }

    // ============================================================
    // ✔ Week
    // ============================================================

    // CREATE
    @PostMapping("/week/create")
    public String createWeek() {

        LearningWeek w = new LearningWeek();
        w.setLearningId(1);
        w.setWeekNumber(1);
        w.setTitle("1주차 테스트");
        w.setGoal("목표 테스트");
        w.setStatus("예정");

        learningWeekDao.insert(w);

        return "Week 생성 완료! PK = " + w.getWeekId();
    }

    // DETAIL
    @GetMapping("/week/detail/{weekId}")
    public LearningWeek getWeek(@PathVariable int weekId) {
        return learningWeekDao.selectedByWeekId(weekId);
    }

    // LIST BY LEARNING
    @GetMapping("/week/list/{learningId}")
    public List<LearningWeek> getWeekList(@PathVariable int learningId) {
        return learningWeekDao.selectListByLearningId(learningId);
    }

    // FIND BY LEARNING & WEEKNUMBER
    @GetMapping("/week/{learningId}/number/{weekNumber}")
    public LearningWeek getWeekByNumber(
            @PathVariable int learningId,
            @PathVariable int weekNumber) {

        return learningWeekDao.selectByLearningIdAndWeekNumber(learningId, weekNumber);
    }

    // UPDATE
    @PutMapping("/week/update")
    public String updateWeek(@RequestBody LearningWeek week) {

        int result = learningWeekDao.update(week);

        return "Week 수정 완료 = " + result;
    }

    // ============================================================
    // ✔ Day
    // ============================================================

    // CREATE
    @PostMapping("/day/create")
    public String createDay() {

        LearningDay d = new LearningDay();
        d.setWeekId(1);
        d.setWeekId(1);
        d.setDayNumber(1);
        d.setContent("테스트 내용");
        d.setStatus("예정");

        learningDayDao.insert(d);

        return "Day 생성 완료! PK = " + d.getDayId();
    }

    // DETAIL
    @GetMapping("/day/detail/{dayId}")
    public LearningDay getDay(@PathVariable int dayId) {
        return learningDayDao.selectedByDayId(dayId);
    }

    // LIST BY WEEK
    @GetMapping("/day/list/week/{weekId}")
    public List<LearningDay> listDayByWeek(@PathVariable int weekId) {
        return learningDayDao.selectListByWeekId(weekId);
    }

    // LIST BY LEARNING
    @GetMapping("/day/list/learning/{learningId}")
    public List<LearningDay> listDayByLearning(@PathVariable int learningId) {
        return learningDayDao.selectListByLearningId(learningId);
    }

    // GET BY WEEK + DAYNUMBER
    @GetMapping("/day/{weekId}/number/{dayNumber}")
    public LearningDay getDayByNumber(
            @PathVariable int weekId,
            @PathVariable int dayNumber) {

        return learningDayDao.selectByWeekIdAndDayNumber(weekId, dayNumber);
    }

    // UPDATE
    @PutMapping("/day/update")
    public String updateDay(@RequestBody LearningDay day) {

        int result = learningDayDao.update(day);

        return "Day 수정 완료 = " + result;
    }

}
