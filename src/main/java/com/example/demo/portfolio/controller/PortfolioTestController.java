package com.example.demo.portfolio.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.portfolio.dao.PortfolioDao;

import com.example.demo.portfolio.dao.PortfolioGuideDao;
import com.example.demo.portfolio.dao.PortfolioImageDao;
import com.example.demo.portfolio.dao.PortfolioStandardDao;
import com.example.demo.portfolio.entity.Portfolio;

import org.springframework.web.bind.annotation.PostMapping;



@RestController
@RequestMapping("/test/portfolio")
public class PortfolioTestController {

  @Autowired
  private PortfolioGuideDao portfolioGuideDao;

  @Autowired
  private PortfolioDao portfolioDao;

  @Autowired
  private PortfolioStandardDao criteriaDao;

  @Autowired
  private PortfolioImageDao portfolioImageDao;


  @PostMapping("/insert")
  public String insert() {
      Portfolio portfolio = new Portfolio();
      portfolio.setMemberId(1);
      portfolio.setTitle("테스트2");
      portfolio.setPortfolioFeedback("{\"message\": \"피드백 내용\"}");
      
      portfolioDao.insertPortfolio(portfolio);
      return "포트폴리오 생성ok"+portfolio.getPortfolioId();
  }
  



}
