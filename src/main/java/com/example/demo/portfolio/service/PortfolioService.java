package com.example.demo.portfolio.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.portfolio.dao.PortfolioDao;
import com.example.demo.portfolio.dao.PortfolioImageDao;
import com.example.demo.portfolio.dto.request.PortfolioCreateRequest;
import com.example.demo.portfolio.dto.response.PortfolioCreateResponse;
import com.example.demo.portfolio.entity.Portfolio;
import com.example.demo.portfolio.entity.PortfolioImage;
import com.example.demo.portfolio.util.PdfToImageConverter;

@Service
public class PortfolioService {

  @Autowired
  private PortfolioDao portfolioDao;

  @Autowired
  private PortfolioImageDao portfolioImageDao;

  public PortfolioCreateResponse createPortfolio(PortfolioCreateRequest request) throws Exception {

    MultipartFile pdfFile = request.getPdfFile();

    // 포트폴리오 저장
    Portfolio portfolio = new Portfolio();
    portfolio.setMemberId(request.getMemberId());
    portfolio.setTitle(request.getTitle());
    portfolio.setOriginalFilename(pdfFile.getOriginalFilename());
    portfolio.setContentType(pdfFile.getContentType());
    portfolio.setPdfFile(pdfFile.getBytes());

    portfolioDao.insertPortfolio(portfolio);

    // PDF 이미지 변환
    List<byte[]> images = PdfToImageConverter.convert(pdfFile, 200);

    // 페이지별로 이미지 저장
    int pageNo = 1;
    for (byte[] imageBytes : images) {
      PortfolioImage portfolioImage = new PortfolioImage();
      portfolioImage.setPortfolioId(portfolio.getPortfolioId());
      portfolioImage.setPageNo(pageNo);
      portfolioImage.setFilename("page_" + pageNo + ".png");
      portfolioImage.setFiletype("image/png");
      portfolioImage.setFiledata(imageBytes);

      portfolioImageDao.insertPortfolioImage(portfolioImage);
      pageNo++;
    }

    // 응답 반환
    PortfolioCreateResponse response = new PortfolioCreateResponse();
    response.setPortfoliId(portfolio.getPortfolioId());
    response.setImageCount(images.size());
    response.setMessage("PDF 업로드 완료");

    return response;
  }


  // 이미지 조회
  


}
