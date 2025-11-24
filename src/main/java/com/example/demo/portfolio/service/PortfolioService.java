package com.example.demo.portfolio.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.ai.portfolio.PortfolioPageAnalysisService;
import com.example.demo.portfolio.dao.PortfolioDao;
import com.example.demo.portfolio.dao.PortfolioImageDao;
import com.example.demo.portfolio.dto.request.PortfolioCreateRequest;
import com.example.demo.portfolio.entity.Portfolio;

@Service
public class PortfolioService {

  @Autowired
  private PortfolioDao portfolioDao;

  @Autowired
  private PortfolioImageDao portfolioImageDao;

  @Autowired
  private PortfolioPageAnalysisService portfolioPageAnalysisService;

  @Transactional
  public Integer createPortfolio(PortfolioCreateRequest request) throws Exception {

    MultipartFile pdfFile = request.getPdfFile();

    // 포트폴리오 저장
    Portfolio portfolio = new Portfolio();
    portfolio.setMemberId(request.getMemberId());
    portfolio.setTitle(request.getTitle());
    portfolio.setOriginalFilename(pdfFile.getOriginalFilename());
    portfolio.setContentType(pdfFile.getContentType());
    portfolio.setPdfFile(pdfFile.getBytes());

    portfolioDao.insertPortfolio(portfolio);

    return portfolio.getPortfolioId();
  }

  // 페이지별로 분석하는 에이전트 호출
  public List<String> analyzePortfolio(Integer portfolioId) throws Exception {
    Portfolio portfolio = portfolioDao.selectPortfolioById(portfolioId);
    return portfolioPageAnalysisService.analyzePortfolio(portfolio.getPdfFile(), portfolioId, portfolio.getMemberId());
  }


  // 이미지 조회
  


}
