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
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.example.demo.portfolio.dao.PortfolioDao;
import com.example.demo.portfolio.dao.PortfolioImageDao;
import com.example.demo.portfolio.dto.response.PortfolioPageFeedbackResponse;
import com.example.demo.portfolio.entity.PortfolioImage;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PortfolioPageAnalysisService {

  // 필드
  private ChatClient chatClient;

  @Autowired
  private PortfolioDao portfolioDao;

  @Autowired
  private PortfolioImageDao portfolioImageDao;

  @Autowired
  private MemberDao memberDao;

  // AI 응답 DTO → JSON 문자열로 직렬화하기 위한 ObjectMapper
  @Autowired
  private ObjectMapper objectMapper;

  // 생성자
  public PortfolioPageAnalysisService(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  // Agent
  public List<PortfolioPageFeedbackResponse> analyzePortfolio(byte[] pdfBytes, Integer portfolioId, Integer memberId)
      throws Exception {

    // Bean 객체 -> JSON 출력 변환기 생성
    BeanOutputConverter<PortfolioPageFeedbackResponse> converter = new BeanOutputConverter<>(
        PortfolioPageFeedbackResponse.class);

    // DTO 구조 제공 -> JSON 출력 포맷 지정
    String format = converter.getFormat();

    PDDocument document = PDDocument.load(pdfBytes); // PDFBox를 이용해 PDF 파일을 로드
    PDFRenderer renderer = new PDFRenderer(document); // PDF 페이지를 하나씩 이미지를 그릴 수 있는 도구 생성
    int pageCount = document.getNumberOfPages(); // PDF 페이지 수 가져오기
    portfolioDao.updatePortfolioPageCount(portfolioId, pageCount);

    List<PortfolioPageFeedbackResponse> feedbackList = new ArrayList<>(); // 피드백 결과를 반환하기 위한 리스트 생성

    // 사용자의 직무, 직군을 불러와서 포트폴리오 프롬프트에 전달
    Member member = memberDao.findById(memberId);
    String jobGroup = member.getJobGroup();
    String jobRole = member.getJobRole();

    for (int i = 0; i < pageCount; i++) {

      // PDF 페이지를 이미지로 렌더링
      // dpi: 이미지 해상도를 결정하는 숫자. 200-250이 이미지 변환 시 가장 많이 사용
      BufferedImage image = renderer.renderImageWithDPI(i, 96); // 토큰 제한으로 최소 dpi로 최대 효율을 낼 수 있는 96으로 수정

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
                당신은 전문 취업 컨설턴트이며 포트폴리오 전문 평가자입니다.
                톤은 따뜻하거나 무난한 조언이 아니라, 지원자를 성장시키기 위한 ‘냉정하고 직설적인 전문가 코멘트’ 스타일로 답변해야 합니다.
                지나친 감정 표현, 공감 문구, 부드러운 문장 사용을 절대 포함하지 않습니다.

                평가 규칙:
                - 각 항목 점수는 0~100점 사이 정수로 계산한다.
                - 점수는 명확한 근거 기반으로 설명해야 한다.
                - 불명확한 추상적 표현을 금지한다.
                - JSON 형식 이외의 불필요한 문장을 출력하지 않는다.

                부적절한 입력 처리 규칙:
                입력 이미지 또는 텍스트가 다음 중 하나라도 해당하면 “평가 불가”로 처리합니다:
                - 의미 없는 단어 나열 (예: asdf, ㄱㄱㄱ, random text 등)
                - 의미 없는 이미지, 깨진 페이지, 내용 식별 불가
                - 욕설, 비속어, 공격적 표현
                - 항목 전체가 비어 있거나 분석할 수 있는 정보가 없는 경우

                모든 문장은 공식 보고서 톤인 ‘~합니다’ 형태로 작성하며, ‘~한다’와 같은 평서형은 절대 사용하지 않습니다.

                아래 JSON 형식을 정확히 준수해 답변할 것:
                %s
              """.formatted(format))
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
          .text("""
              다음은 포트폴리오 %d번 페이지 이미지입니다.
              이 이미지를 분석하여 format에 맞는 JSON을 생성합니다.

              [참고 정보]
              - 직군: %s
              - 역할: %s
              - 이전 페이지 요약: %s
              - 현재 페이지 번호: %d

              [평가 시 고려사항]
              1) 페이지 핵심 메시지가 시각적으로 명확하게 드러나는지
              2) 구성 요소들의 배치, 정렬, 여백 등 레이아웃 완성도
              3) 텍스트와 시각 요소가 직군/직무와 일관적인지
              4) 개선해야 하는 구체적인 포인트는 무엇인지
              5) 메시지 전달에 방해되는 요소는 무엇인지

              반드시 format에 있는 JSON key만 사용해 정확히 JSON만 출력합니다.""".formatted(pageNo, jobGroup, jobRole, previousFeedbackText, pageNo))
          .media(media)
          .build();

      // LLM 호출
      String feedback = chatClient.prompt()
          .messages(systemMessage, userMessage)
          .call()
          .content();

      // JSON -> DTO 변환
      PortfolioPageFeedbackResponse response = converter.convert(feedback);
      log.info("페이지 {} 분석 결과: {}", pageNo, response);

      // DTO -> JSON으로 다시 직렬화해서 DB 업데이트
      String responseJson = objectMapper.writeValueAsString(response);

      // DB 저장
      PortfolioImage portfolioImage = new PortfolioImage();
      portfolioImage.setPortfolioId(portfolioId);
      portfolioImage.setPageNo(pageNo);
      portfolioImage.setPageFeedback(responseJson);
      portfolioImageDao.insertPortfolioImage(portfolioImage);

      feedbackList.add(response);

      // 1초간 sleep
      Thread.sleep(5000);
    }

    document.close();
    return feedbackList;
  }

}
