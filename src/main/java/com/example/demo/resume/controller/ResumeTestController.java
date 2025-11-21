package com.example.demo.resume.controller;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.resume.dao.ResumeDao;
import com.example.demo.resume.entity.Resume;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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


   /**
     * 이력서 생성 테스트
     * POST http://localhost:8081/test/resume/insert
     */
    @PostMapping("/insert")
    public String insertResume() {
        try {
            Resume resume = new Resume();
            resume.setMemberId(1);
            resume.setTitle("백엔드 개발자 이력서");
            
            // JSONB 데이터 샘플 생성
            resume.setCareerInfo("""
                {
                  "careers": [
                    {
                      "company": "네이버",
                      "position": "백엔드 개발자",
                      "department": "플랫폼개발팀",
                      "period": "2022.03 ~ 2024.02",
                      "description": "Spring Boot 기반 API 서버 개발",
                      "achievements": ["서버 응답시간 30% 개선", "MAU 100만 달성"]
                    }
                  ]
                }
                """);
                
            resume.setEducationInfo("""
                {
                  "educations": [
                    {
                      "school": "서울대학교",
                      "major": "컴퓨터공학과", 
                      "degree": "학사",
                      "period": "2018.03 ~ 2022.02",
                      "gpa": "3.8/4.5"
                    }
                  ]
                }
                """);
                
            resume.setSkills("""
                {
                  "technical": {
                    "languages": ["Java", "Python", "JavaScript"],
                    "frameworks": ["Spring Boot", "Vue.js", "React"],
                    "databases": ["PostgreSQL", "MySQL", "Redis"],
                    "tools": ["Git", "Docker", "AWS"]
                  },
                  "soft_skills": ["커뮤니케이션", "문제해결", "팀워크"]
                }
                """);
                
            resume.setCertificates("""
                {
                  "certificates": [
                    {
                      "name": "정보처리기사",
                      "organization": "한국산업인력공단",
                      "date": "2021-11-26"
                    }
                  ],
                  "languages": [
                    {
                      "language": "TOEIC",
                      "score": "850",
                      "date": "2023-06-15"
                    }
                  ]
                }
                """);
                
            resume.setAwards("""
                {
                  "awards": [
                    {
                      "name": "해커톤 대상",
                      "organization": "삼성전자",
                      "date": "2024-03-15",
                      "description": "AI 기반 서비스 개발로 1등 수상"
                    },
                    {
                      "name": "학과 우수상", 
                      "organization": "서울대학교 컴퓨터공학부",
                      "date": "2021-12-20",
                      "grade": "A+",
                      "gpa": "4.2/4.5"
                    }
                  ]
                }
                """);
                
            resume.setActivities("""
                {
                  "activities": [
                    {
                      "type": "프로젝트",
                      "name": "AI 취업 컨설팅 플랫폼",
                      "period": "2024.01 ~ 2024.06",
                      "role": "백엔드 개발자",
                      "technologies": ["Spring Boot", "PostgreSQL", "Spring AI"],
                      "achievements": ["사용자 1000명 달성", "응답시간 50% 단축"],
                      "description": "취업 준비생을 위한 AI 기반 이력서/포트폴리오 분석 서비스"
                    },
                    {
                      "type": "대외활동", 
                      "name": "코딩 동아리 SSAFY",
                      "period": "2021.01 ~ 2021.12",
                      "role": "스터디 리더",
                      "description": "알고리즘 스터디 운영 및 프로젝트 팀장"
                    }
                  ]
                }
                """);
                
            resume.setResumeFeedback("""
                {
                  "overall_score": 85,
                  "grammar_score": 90,
                  "keyword_score": 85,
                  "logic_score": 80,
                  "structure_score": 75,
                  "readability_score": 85,
                  "strengths": [
                    "다양한 기술 스택에 대한 경험을 갖추고 있습니다",
                    "프로젝트 성과를 정량적으로 표현했습니다",
                    "학업과 실무 경험이 균형있게 구성되어 있습니다"
                  ],
                  "weaknesses": [
                    "경력 기술이 다소 추상적입니다",
                    "직무와 관련된 구체적인 기술 깊이가 부족합니다"
                  ],
                  "suggestions": [
                    "경력 설명에 사용한 기술과 해결한 문제를 더 구체적으로 기술하세요",
                    "프로젝트별 본인의 기여도와 역할을 명확히 표현하세요"
                  ]
                }
                """);

            resumeDao.insertResume(resume);
            
            return "이력서 생성 완료! ID: " + resume.getResumeId();
            
        } catch (Exception e) {
            log.error("이력서 생성 오류", e);
            return "이력서 생성 실패: " + e.getMessage();
        }
    }

    /**
     * 이력서 단건 조회 테스트
     * GET http://localhost:8081/test/resume/select/{resumeId}
     */
    @GetMapping("/select/{resumeId}")
    public Resume selectResume(@PathVariable Integer resumeId) {
       
            Resume resume = resumeDao.selectResumeById(resumeId);
           
            return resume;
       
    }

    /**
     * 회원별 이력서 목록 조회 테스트 
     * GET http://localhost:8081/test/resume/list/{memberId}
     */
    @GetMapping("/list/{memberId}")
    public List<Resume> selectResumesByMember(@PathVariable Integer memberId) {
       
            List<Resume> resumes = resumeDao.selectResumesByMemberId(memberId);
         
        
            return resumes;
        
    }

    /**
     * JSON 파싱 테스트 (awards 필드만)
     * GET http://localhost:8081/test/resume/parse-awards/{resumeId}
     */
    @GetMapping("/parse-awards/{resumeId}")
public Object parseAwardsJson(@PathVariable Integer resumeId) {
    try {
        Resume resume = resumeDao.selectResumeById(resumeId);
        if (resume == null) {
            throw new RuntimeException("해당 이력서를 찾을 수 없습니다.");
        }
        
        // ObjectMapper로 JSON 파싱
        com.fasterxml.jackson.databind.ObjectMapper mapper = 
            new com.fasterxml.jackson.databind.ObjectMapper();
        
        // awards JSON을 Map으로 파싱
        if (resume.getAwards() != null) {
            java.util.Map<String, Object> awardsMap = 
                mapper.readValue(resume.getAwards(), java.util.Map.class);
            log.info("Awards 파싱 결과: {}", awardsMap);
            return awardsMap;  // JSON 형태의 Map 반환
        } else {
            return java.util.Collections.emptyMap();  // 빈 Map 반환
        }
        
    } catch (Exception e) {
        log.error("JSON 파싱 오류", e);
        throw new RuntimeException("JSON 파싱 실패: " + e.getMessage());
    }
}

    
}

