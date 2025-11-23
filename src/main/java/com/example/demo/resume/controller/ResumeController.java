package com.example.demo.resume.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.resume.dto.response.ResumeReportResponse;
import com.example.demo.resume.entity.Resume;
import com.example.demo.resume.service.ResumeService;

@RestController
@RequestMapping("/resume")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    // 이력서 생성
    @PostMapping("/create")
    public ResponseEntity<Integer> createResume(@RequestBody Resume resume) {
        int newId = resumeService.createResume(resume);
        return ResponseEntity.ok(newId);
    }

    // 이력서 조회
    @GetMapping("/{resumeId}")
    public ResponseEntity<Resume> getResume(@PathVariable("resumeId") int resumeId) {
        return ResponseEntity.ok(resumeService.getResume(resumeId));
    }

    // AI 분석
    @PostMapping("/analyze")
    public ResponseEntity<ResumeReportResponse> analyze(
            @RequestParam("memberId") int memberId,
            @RequestParam("resumeId") int resumeId) throws Exception {

        ResumeReportResponse result = resumeService.analyzeResume(memberId, resumeId);
        return ResponseEntity.ok(result);
    }

    // AI 피드백 반영
    @PutMapping("/{resumeId}/rewrite-career")
    public ResponseEntity<?> applyCareer(
            @PathVariable("resumeId") int resumeId,
            @RequestParam("index") int index) throws Exception {

        int result = resumeService.applyCareerRewrite(resumeId, index);
        return ResponseEntity.ok(result);
    }
}
