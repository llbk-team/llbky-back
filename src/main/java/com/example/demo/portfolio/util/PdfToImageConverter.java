package com.example.demo.portfolio.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.web.multipart.MultipartFile;

public class PdfToImageConverter {


  // PDF 파일 → 이미지 byte[] 리스트로 변환
  // PDF를 이미지로 변환하는 작업은 도메인 로직이 아니고 그냥 기술적 유틸리티로 사용되어서 따로 빼둠
  public static List<byte[]> convert(MultipartFile pdfFile, int dpi) throws Exception {
    // dpi: 이미지 해상도를 결정하는 숫자. 200-250이 이미지 변환 시 가장 많이 사용
    return convert(pdfFile.getBytes(), dpi);
  }


  // PDF byte[] → 이미지 byte[] 리스트로 변환
  public static List<byte[]> convert(byte[] pdfBytes, int dpi) throws Exception {
    
    List<byte[]> images = new ArrayList<>();
    PDDocument document = PDDocument.load(pdfBytes); // PDFBox를 이용해 PDF 파일을 로드

    PDFRenderer renderer = new PDFRenderer(document); // PDF 페이지를 하나씩 이미지를 그릴 수 있는 도구 생성
    int pageCount = document.getNumberOfPages(); // PDF 페이지 수 가져오기

    for (int i = 0; i < pageCount; i++) {

      // PDF 페이지를 이미지로 렌더링
      BufferedImage image = renderer.renderImageWithDPI(i, dpi);

      // PNG 형식 byte[] 로 변환
      // 파일로 저장하면 다시 읽어서 byte[]로 변환 → 비효율적. 메모리에서 바로 byte[] 얻기
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "png", baos); // 이미지 파일 저장 기능

      images.add(baos.toByteArray());
    }
    
    return images;
  }

}
