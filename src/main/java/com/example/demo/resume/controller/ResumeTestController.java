package com.example.demo.resume.controller;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.resume.dao.ResumeDao;
import com.example.demo.resume.entity.Resume;

@RestController
@RequestMapping("/test/resume")
public class ResumeTestController {

    @Autowired
    private ResumeDao resumeDao;

    // ======================================================
    // 1) CREATE 
    // ======================================================
    @PostMapping("/create")
    public String createResume() {

        Resume r = new Resume();
        r.setMemberId(1);
        r.setTitle("테스트 이력서");
        r.setCareerInfo("""
                [
                    { "company": "네이버", "position": "백엔드 인턴", "period": "3개월" },
                    { "company": "카카오", "position": "서버 개발자", "period": "6개월" }
                ]
                """);
        r.setEducationInfo("""
                [
                    { "school": "한국IT대학교", "major": "소프트웨어", "status": "졸업" }
                ]
                """);
        r.setSkills("""
                {
                    "languages": ["Java", "Python"],
                    "backend": ["Spring Boot", "MyBatis"],
                    "database": ["PostgreSQL"]
                }
                """);
        r.setCertificates("""
                [
                    { "name": "정보처리기사", "date": "2024-05" }
                ]
                """);
        r.setAwards("""
                [
                    { "name": "ICT 공모전 장려상", "year": "2024" }
                ]
                """);
        r.setActivities("""
            [
                { "name": "AI 학습 코치 프로젝트", "role": "백엔드 개발" }
            ]
        """);

        resumeDao.insertResume(r);

        return "이력서 생성 완료! PK = " + r.getResumeId();
    }

    // ======================================================
    // 2) UPDATE 
    // ======================================================
    @PutMapping("/update")
    public String updateResume() {

        Resume r = new Resume();
        r.setResumeId(1);  // 수정할 이력서 ID
        r.setTitle("업데이트된 테스트 이력서");
        r.setCareerInfo("""
                [
                    { "company": "직방", "position": "백엔드 개발자", "period": "1년" }
                ]
                """);
        r.setSkills("""
                {
                    "languages": ["Java", "Kotlin", "Python"],
                    "backend": ["Spring Boot", "JPA"],
                    "database": ["PostgreSQL", "Redis"]
                }
                """);
        r.setResumeFeedback("""
                {
                    "aiScore": 95,
                    "summary": "업데이트 후 더 좋아짐"
                }
                """);
        int result = resumeDao.updateResume(r);

        return "이력서 수정 완료 (row=" + result + ")";
    }

    // ======================================================
    // 3) FEEDBACK만 업데이트
    // ======================================================
    @PutMapping("/feedback")
    public String updateFeedback() {

        String feedbackJson = """
                {
                    "aiScore": 99,
                    "strength": "적극적인 프로젝트 경험",
                    "weakness": "문서화 부족"
                }
                """;

        int result = resumeDao.updateResumeFeedback(1, feedbackJson);

        return "AI 피드백 수정 완료 (row=" + result + ")";
    }

    // ======================================================
    // 4) DETAIL 조회
    // ======================================================
    @GetMapping("/{resumeId}")
    public Resume getDetail(@PathVariable int resumeId) {
        return resumeDao.selectResumeById(resumeId);
    }

    // ======================================================
    // 5) MEMBER별 목록 조회
    // ======================================================
    @GetMapping("/member/{memberId}")
    public List<Resume> getList(@PathVariable int memberId) {
        return resumeDao.selectResumesByMemberId(memberId);
    }
}

