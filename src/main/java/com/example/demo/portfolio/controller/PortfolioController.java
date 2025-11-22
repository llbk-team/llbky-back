package com.example.demo.portfolio.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.portfolio.dto.request.PortfolioCreateRequest;
import com.example.demo.portfolio.dto.response.PortfolioCreateResponse;
import com.example.demo.portfolio.service.PortfolioService;


@RestController
@RequestMapping("portfolio")
public class PortfolioController {

  @Autowired
  private PortfolioService portfolioService;

  // PDF 업로드
  @PostMapping("/create")
  public ResponseEntity<PortfolioCreateResponse> createPortfolio(
      @RequestParam("memberId") Integer memberId, 
      @RequestParam("title") String title, 
      @RequestParam("pdfFile") MultipartFile pdfFile) throws Exception {

    PortfolioCreateRequest request = new PortfolioCreateRequest();
    request.setMemberId(memberId);
    request.setTitle(title);
    request.setPdfFile(pdfFile);

    PortfolioCreateResponse response =  portfolioService.createPortfolio(request);

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  // 페이지 이미지 조회
  // @GetMapping("/{portfolioId}/image/{pageNo}")
  // public ResponseEntity<byte[]> getPortfolioImage(@PathVariable("portfolioId") Integer portfolioId, @PathVariable("pageNo") int pageNo) {
  //   return portfolioService.getPortfolioImage(portfolioId, pageNo);
  // }
}
