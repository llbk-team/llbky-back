package com.example.demo.learning.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.learning.entity.Learning;
import com.example.demo.learning.service.LearningService;


@RestController
@RequestMapping("/learning")
public class LearningController {

  @Autowired
  private LearningService learningService;

  @PostMapping("/create")
  public String createLearning(@RequestBody Learning learning) {
    learningService.createLearning(learning);
    return "생성 완료";
  }
  

}
