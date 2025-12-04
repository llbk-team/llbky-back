package com.example.demo.newstrend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.newstrend.entity.JobInsight;
import com.example.demo.newstrend.service.JobInsightService;


@RestController
@RequestMapping("/jobInsight")
public class JobInsightController {
  @Autowired
  private JobInsightService jobInsightService;

  @PostMapping("/generate")
  public ResponseEntity<JobInsight> generateJobInsight(@RequestParam("memberId") int memberId) throws Exception {
    return ResponseEntity.ok(jobInsightService.createJobInsight(memberId));
  }

  @GetMapping("/latest")
  public ResponseEntity<JobInsight> getJobInsight(@RequestParam("memberId") int memberId) {
    return ResponseEntity.ok(jobInsightService.getLatestInsight(memberId));
  }

  @GetMapping("/today")
  public ResponseEntity<JobInsight> analyze(@RequestParam("memberId") int memberId) throws Exception {
    return ResponseEntity.ok(jobInsightService.getOrCreateInsight(memberId));
  }

  @PostMapping("/refresh")
  public ResponseEntity<JobInsight> updateGrowth(@RequestParam("memberId") int memberId) throws Exception {
    return ResponseEntity.ok(jobInsightService.modifyGrowthAnalysis(memberId));
  }
}
