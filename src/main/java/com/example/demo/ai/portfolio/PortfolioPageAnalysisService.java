package com.example.demo.ai.portfolio;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.dto.Member;
import com.example.demo.portfolio.dao.PortfolioImageDao;
import com.example.demo.portfolio.entity.PortfolioImage;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PortfolioPageAnalysisService {

  // 필드
  private ChatClient chatClient;

  @Autowired
  private PortfolioImageDao portfolioImageDao;

  @Autowired
  private MemberDao memberDao;

  // 생성자
  public PortfolioPageAnalysisService(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  // Agent
  public List<String> analyzePortfolio(byte[] pdfBytes, Integer portfolioId, Integer memberId) throws Exception {

    PDDocument document = PDDocument.load(pdfBytes); // PDFBox를 이용해 PDF 파일을 로드
    PDFRenderer renderer = new PDFRenderer(document); // PDF 페이지를 하나씩 이미지를 그릴 수 있는 도구 생성
    int pageCount = document.getNumberOfPages(); // PDF 페이지 수 가져오기

    List<String> feedbackList = new ArrayList<>(); // 피드백 결과를 반환하기 위한 리스트 생성

    // 사용자의 직무, 직군을 불러와서 포트폴리오 프롬프트에 전달
    Member member = memberDao.findById(memberId);
    String jobGroup = member.getJobGroup();
    String jobRole = member.getJobRole();

    for (int i = 0; i < pageCount; i++) {

      // PDF 페이지를 이미지로 렌더링
      // dpi: 이미지 해상도를 결정하는 숫자. 200-250이 이미지 변환 시 가장 많이 사용
      BufferedImage image = renderer.renderImageWithDPI(i, 96);

      // PNG 형식 byte[] 로 변환
      // 파일로 저장하면 다시 읽어서 byte[]로 변환 → 비효율적. 메모리에서 바로 byte[] 얻기
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "png", baos); // 이미지 파일 저장 기능

      byte[] imageBytes = baos.toByteArray();
      int pageNo = i + 1;

      // 이전 페이지 피드백 불러오기
      PortfolioImage previousFeedback = portfolioImageDao.selectBeforePage(portfolioId, pageNo);

      String previousFeedbackText = "";
      if (previousFeedback != null) {
        previousFeedbackText = previousFeedback.getPageFeedback();
      }

      // SystemMessage 생성
      SystemMessage systemMessage = SystemMessage.builder()
          .text("""
              너는 포트폴리오 페이지를 분석하는 AI다.
              항상 JSON 형태로 분석문을 작성해라.

              규칙:
              - JSON 외의 텍스트 출력 금지
              - 코드블록( ``` ) 금지
              - 백틱(`) 금지
              - 문자열은 반드시 쌍따옴표로 감싸야 함
              - JSON 길이 제한 없음

              직군: %s / 역할: %s 기준으로 분석한다.""".formatted(jobGroup, jobRole))
          .build();

      // 이미지 bytes -> Resource
      ByteArrayResource resource = new ByteArrayResource(imageBytes) {
        @Override
        public String getFilename() {
          return "page.png";
        }
      };

      // Resource -> Media 객체로 변환
      Media media = Media.builder()
          .mimeType(MimeType.valueOf("image/png"))
          .data(resource)
          .build();

      // userMessage 생성
      UserMessage userMessage = UserMessage.builder()
          .text(
              """
                  다음 포트폴리오 페이지 이미지를 분석해 JSON 형태로 작성해라.
                  JSON 구조는 다음과 같다.

                  {
                  "page_summary": "해당 페이지는 프로젝트의 전체 개요를 간결하게 제시하고 있으며, 시각적으로도 명확한 첫인상을 제공합니다. 프로젝트 목적, 문제 정의, 해결 방향을 한눈에 파악할 수 있도록 구성된 점이 매우 좋습니다. 또한 표기된 주요 기능이나 특징들이 명확하게 분리되어 있어 사용자가 프로젝트의 핵심 포인트를 빠르게 캐치할 수 있습니다. 다만 제목과 본문 사이의 여백이 조금 넓어 시선이 한 번 끊기는 느낌이 있어, 해당 거리만 조금 좁혀도 집중도가 훨씬 좋아질 것으로 보입니다.",
                  "page_comment": "전체적으로 좋은 구성입니다만, 핵심 메시지를 강조하는 부분이 상대적으로 약합니다. 주요 포인트 아래에 간단한 하이라이트 색상 혹은 짧은 키워드를 추가하면 페이지의 정보 전달력이 더 선명해질 것입니다."
                  }

                  이전 페이지 요약: %s
                  현재 페이지 번호: %d"""
                  .formatted(previousFeedbackText.toString(), pageNo))
          .media(media)
          .build();

      // LLM 호출
      String feedback = chatClient.prompt()
          .messages(systemMessage, userMessage)
          .call()
          .content();

      log.info("페이지 {} 분석 결과: {}", pageNo, feedback);

      // DB 저장
      PortfolioImage portfolioImage = new PortfolioImage();
      portfolioImage.setPortfolioId(portfolioId);
      portfolioImage.setPageNo(pageNo);
      portfolioImage.setPageFeedback(feedback);
      portfolioImageDao.insertPortfolioImage(portfolioImage);

      feedbackList.add(feedback);

    }

    document.close();
    return feedbackList;
  }

}
