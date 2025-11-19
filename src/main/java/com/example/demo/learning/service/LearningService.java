package com.example.demo.learning.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.learning.dao.LearningDao;
import com.example.demo.learning.entity.Learning;

@Service
public class LearningService {

  @Autowired
  private LearningDao learningDao;

  public int createLearning(Learning learning) {
        return learningDao.insert(learning);
    }
}
