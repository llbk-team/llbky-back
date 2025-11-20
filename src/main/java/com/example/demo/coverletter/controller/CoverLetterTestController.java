package com.example.demo.coverletter.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.coverletter.dao.CoverLetterDao;
import com.example.demo.coverletter.entity.CoverLetter;

@RestController
@RequestMapping("/test/coverletter")
public class CoverLetterTestController {

    @Autowired
    private CoverLetterDao coverLetterDao;

    // =============================
    // 1) INSERT 테스트 (자소서 생성)
    // =============================
    @PostMapping("/insert")
    public String insertCoverLetter() {
        CoverLetter c = new CoverLetter();
        c.setMemberId(1);
        c.setSupportMotive("저는 개발자로 성장하고 싶습니다.");
        c.setGrowthExperience("어릴 때부터 문제 해결을 좋아했습니다.");
        c.setJobCapability("Java, Spring Boot, MyBatis");
        c.setFuturePlan("백엔드 시니어로 성장하겠습니다.");
        c.setCoverFeedback("{\"strength\":\"문장 구성 좋음\",\"score\":90}");

        coverLetterDao.insertCoverLetter(c);

        return "INSERT OK! PK = " + c.getCoverletterId();
    }

    // =============================
    // 2) 특정 회원의 자소서 목록
    // =============================
    @GetMapping("/list")
    public List<CoverLetter> selectAll(@RequestParam("memberId") int memberId) {
        return coverLetterDao.selectAllCoverLetters(memberId);
    }

    // =============================
    // 3) 자소서 상세 조회
    // =============================
    @GetMapping("/detail")
    public CoverLetter selectOne(@RequestParam("coverletterId") int coverletterId) {
        return coverLetterDao.selectOneCoverLetter(coverletterId);
    }

    // =============================
    // 4) 자소서 수정
    // =============================
    @PostMapping("/update")
    public String updateCoverLetter() {

        CoverLetter c = new CoverLetter();
        c.setCoverletterId(1);   // 수정할 자소서 PK
        c.setMemberId(1);        // 작성자 ID
        c.setSupportMotive("수정된 지원동기");
        c.setGrowthExperience("업데이트된 성장과정");
        c.setJobCapability("Spring / React / PostgreSQL");
        c.setFuturePlan("더 성장하겠습니다.");
        c.setCoverFeedback("{\"strength\":\"업데이트됨\",\"score\":95}");

        int result = coverLetterDao.updateCoverLetter(c);

        return "UPDATE OK = " + result;
    }

    // =============================
    // 5) 자소서 삭제
    // =============================
    @DeleteMapping("/delete")
    public String deleteCoverLetter(@RequestParam("coverletterId") int coverletterId, @RequestParam("memberId") int memberId) {
        int result = coverLetterDao.deleteCoverLetter(coverletterId, memberId);
        return "DELETE OK = " + result;
    }
}
