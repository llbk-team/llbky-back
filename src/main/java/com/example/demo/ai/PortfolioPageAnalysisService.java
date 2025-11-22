package com.example.demo.ai;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

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

  // 생성자
  public PortfolioPageAnalysisService(ChatClient.Builder chatClientBuilder) {
    ChatMemory chatMemory = MessageWindowChatMemory.builder()
        .maxMessages(100)
        .build();

    this.chatClient = chatClientBuilder
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(),
            new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE))
        .build();
  }

  // Agent
  public List<String> analyzePortfolio(byte[] pdfBytes, Integer portfolioId) throws IOException {
    String conversationId = "portfolio-" + portfolioId;

    PDDocument document = PDDocument.load(pdfBytes); // PDFBox를 이용해 PDF 파일을 로드
    PDFRenderer renderer = new PDFRenderer(document); // PDF 페이지를 하나씩 이미지를 그릴 수 있는 도구 생성
    int pageCount = document.getNumberOfPages(); // PDF 페이지 수 가져오기

    List<String> feedbackList = new ArrayList<>(); // 피드백 결과를 반환하기 위한 리스트 생성

    for (int i = 0; i < pageCount; i++) {

      // PDF 페이지를 이미지로 렌더링
      // dpi: 이미지 해상도를 결정하는 숫자. 200-250이 이미지 변환 시 가장 많이 사용
      BufferedImage image = renderer.renderImageWithDPI(i, 200);

      // PNG 형식 byte[] 로 변환
      // 파일로 저장하면 다시 읽어서 byte[]로 변환 → 비효율적. 메모리에서 바로 byte[] 얻기
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "png", baos); // 이미지 파일 저장 기능

      byte[] imageBytes = baos.toByteArray();
      int pageNo = i + 1;

      // SystemMessage 생성
      SystemMessage systemMessage = SystemMessage.builder()
          .text("""
              너는 포트폴리오 분석을 전문으로 하는 AI다.
              각 페이지를 분석하되, 이전 페이지 분석 내용을 기억하고
              전체적인 흐름과 맥락을 유지하며 피드백을 제공해라.
              """)
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
          .text("포트폴리오 %d 페이지를 분석해줘".formatted(pageNo))
          .media(media)
          .build();

      // LLM 호출
      String feedback = chatClient.prompt()
          .messages(systemMessage, userMessage)
          .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
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
