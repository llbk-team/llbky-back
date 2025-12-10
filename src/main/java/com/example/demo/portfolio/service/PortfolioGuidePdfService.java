package com.example.demo.portfolio.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.example.demo.portfolio.dto.GuideContentData;
import com.example.demo.portfolio.dto.GuideItemData;
import com.example.demo.portfolio.dto.GuideStepData;
import com.example.demo.portfolio.entity.PortfolioGuide;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.exceptions.IOException;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ 포트폴리오 가이드 PDF 생성 서비스 (iText 사용)
 */
@Service
@Slf4j
public class PortfolioGuidePdfService {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private PdfFont koreanFont;
    private PdfFont koreanBoldFont;
    
    // 색상 팔레트
    private static final Color PRIMARY_COLOR = new DeviceRgb(41, 128, 185);      // 진한 파랑
    private static final Color SECONDARY_COLOR = new DeviceRgb(52, 73, 94);      // 진한 회색
    private static final Color ACCENT_COLOR = new DeviceRgb(46, 204, 113);       // 초록 (완료)
    private static final Color LIGHT_BG = new DeviceRgb(236, 240, 241);          // 연한 회색 배경
    private static final Color SECTION_BG = new DeviceRgb(189, 195, 199);        // 섹션 배경
    
    /**
     * 한글 폰트 초기화
     */
    private void initFonts() throws Exception {
        if (koreanFont == null) {
            ClassPathResource fontResource = new ClassPathResource("fonts/NanumGothic.ttf");
            koreanFont = PdfFontFactory.createFont(
                fontResource.getInputStream().readAllBytes(),
                PdfEncodings.IDENTITY_H,
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
            );
            koreanBoldFont = koreanFont; // 볼드 폰트가 별도로 있으면 사용
            log.info("✅ 한글 폰트 로드 성공");
        }
    }
    



    /**
     * ⭐ 포트폴리오 PDF 생성 (메인 메서드)
     */
    public void generateGuidePdf(PortfolioGuide guide, HttpServletResponse response) throws IOException {
        log.info("포트폴리오 PDF 생성 시작 - guideId: {}", guide.getGuideId());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            initFonts();
            
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);
            
            // 여백 설정
            document.setMargins(40, 40, 60, 40);
            
            // 페이지 이벤트 핸들러 (헤더/푸터)
            pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, 
                new PortfolioHeaderFooterHandler(guide.getTitle(), koreanFont));
            
            // 1. 표지
            addPortfolioCover(document, guide);
            
            // 2. 프로젝트 개요
            addProjectOverview(document, guide);
            
            // 3. 가이드 콘텐츠
            addPortfolioContent(document, guide);
            
            document.close();
            
            // HTTP 응답
            String fileName = String.format("Portfolio_%s.pdf", 
                guide.getTitle().replaceAll("[^a-zA-Z0-9가-힣]", "_"));
            
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", 
                "attachment; filename=\"" + java.net.URLEncoder.encode(fileName, "UTF-8") + "\"");
            response.setContentLength(baos.size());
            
            baos.writeTo(response.getOutputStream());
            response.getOutputStream().flush();
            
            log.info("포트폴리오 PDF 생성 완료");
            
        } catch (Exception e) {
            log.error("PDF 생성 실패", e);
            throw new IOException("PDF 생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * ⭐ 포트폴리오 표지
     */
    private void addPortfolioCover(Document document, PortfolioGuide guide) {
        // 상단 장식 바
        Table topBar = new Table(UnitValue.createPercentArray(1))
            .setWidth(UnitValue.createPercentValue(100))
            .setMarginTop(100);
        
        Cell topCell = new Cell()
            .add(new Paragraph(""))
            .setHeight(10)
            .setBackgroundColor(PRIMARY_COLOR)
            .setBorder(null);
        topBar.addCell(topCell);
        document.add(topBar);
        
        // 메인 제목
        Paragraph title = new Paragraph("PORTFOLIO")
            .setFont(koreanBoldFont)
            .setFontSize(36)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(40)
            .setMarginBottom(20)
            .setFontColor(PRIMARY_COLOR);
        document.add(title);
        
        // 프로젝트명
        Paragraph projectTitle = new Paragraph(guide.getTitle())
            .setFont(koreanFont)
            .setFontSize(24)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(60)
            .setFontColor(SECONDARY_COLOR);
        document.add(projectTitle);
        
        // 진행 상태 박스
        Table statusBox = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
            .setWidth(UnitValue.createPercentValue(70))
            .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)
            .setMarginTop(80)
            .setMarginBottom(80);
        
        statusBox.addCell(createInfoCell("진행률", String.format("%d%%", guide.getCompletionPercentage())));
        statusBox.addCell(createInfoCell("현재 단계", String.format("%d / %d", guide.getCurrentStep(), guide.getTotalSteps())));
        statusBox.addCell(createInfoCell("상태", guide.getIsCompleted() ? "완료" : "진행 중"));
        statusBox.addCell(createInfoCell("생성일", guide.getCreatedAt() != null ? 
            guide.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")) : "-"));
        
        document.add(statusBox);
        
        // 하단 장식 바
        Table bottomBar = new Table(UnitValue.createPercentArray(1))
            .setWidth(UnitValue.createPercentValue(100))
            .setMarginTop(100);
        
        Cell bottomCell = new Cell()
            .add(new Paragraph(""))
            .setHeight(10)
            .setBackgroundColor(PRIMARY_COLOR)
            .setBorder(null);
        bottomBar.addCell(bottomCell);
        document.add(bottomBar);
        
        // 새 페이지
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
    }
    
    /**
     * 정보 셀 생성 헬퍼
     */
    private Cell createInfoCell(String label, String value) {
        Table innerTable = new Table(UnitValue.createPercentArray(1))
            .setWidth(UnitValue.createPercentValue(100));
        
        innerTable.addCell(new Cell()
            .add(new Paragraph(label)
                .setFont(koreanFont)
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY))
            .setBorder(null)
            .setPadding(5));
        
        innerTable.addCell(new Cell()
            .add(new Paragraph(value)
                .setFont(koreanBoldFont)
                .setFontSize(14)
                .setBold())
            .setBorder(null)
            .setPadding(5));
        
        return new Cell()
            .add(innerTable)
            .setBackgroundColor(LIGHT_BG)
            .setPadding(10)
            .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1));
    }

    /**
     * ⭐ 프로젝트 개요 (PPT 스타일로 수정)
     */
    private void addProjectOverview(Document document, PortfolioGuide guide) {
        // 챕터 페이지
        Paragraph chapterTitle = new Paragraph("프로젝트 기본 정보")
            .setFont(koreanBoldFont)
            .setFontSize(36)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(200)
            .setMarginBottom(40);
        document.add(chapterTitle);
        
        Table divider = new Table(UnitValue.createPercentArray(1))
            .setWidth(UnitValue.createPercentValue(50))
            .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
        
        Cell dividerCell = new Cell()
            .add(new Paragraph(""))
            .setHeight(3)
            .setBackgroundColor(PRIMARY_COLOR)
            .setBorder(null);
        divider.addCell(dividerCell);
        document.add(divider);
        
        // 새 페이지로 상세 정보
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        
        // 좌측 번호
        Paragraph bigNumber = new Paragraph("01")
            .setFont(koreanBoldFont)
            .setFontSize(120)
            .setBold()
            .setFontColor(new DeviceRgb(200, 200, 200))
            .setMarginTop(80)
            .setMarginLeft(60);
        document.add(bigNumber);
        
        Paragraph pageTitle = new Paragraph("프로젝트 기본 정보")
            .setFont(koreanBoldFont)
            .setFontSize(28)
            .setBold()
            .setMarginTop(-60)
            .setMarginLeft(60)
            .setMarginBottom(40);
        document.add(pageTitle);
        
        // 정보 박스들 (PPT 스타일)
        addInfoBoxRow(document, "개발 기간", guide.getCreatedAt() != null ? 
            guide.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) + " ~ " +
            (guide.getUpdatedAt() != null ? guide.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) : "진행중")
            : "-");
        
        addInfoBoxRow(document, "개발 목적", guide.getTitle());
        
        addInfoBoxRow(document, "현재 단계", String.format("%d/%d", guide.getCurrentStep(), guide.getTotalSteps()));
        
        addInfoBoxRow(document, "프로젝트 목표", 
            String.format("진행률 %d%%", guide.getCompletionPercentage()));
    }

    /**
     * 정보 박스 행 (PPT 스타일)
     */
    private void addInfoBoxRow(Document document, String label, String value) {
        Table infoBox = new Table(UnitValue.createPercentArray(new float[]{25, 75}))
            .setWidth(UnitValue.createPercentValue(85))
            .setMarginLeft(80)
            .setMarginBottom(20);
        
        // 레이블
        Cell labelCell = new Cell()
            .add(new Paragraph(label)
                .setFont(koreanFont)
                .setFontSize(12)
                .setFontColor(ColorConstants.GRAY))
            .setBackgroundColor(LIGHT_BG)
            .setPadding(15)
            .setBorder(null)
            .setVerticalAlignment(VerticalAlignment.MIDDLE);
        infoBox.addCell(labelCell);
        
        // 값
        Cell valueCell = new Cell()
            .add(new Paragraph(value)
                .setFont(koreanFont)
                .setFontSize(12))
            .setPadding(15)
            .setBorder(null)
            .setVerticalAlignment(VerticalAlignment.MIDDLE);
        infoBox.addCell(valueCell);
        
        // 전체 박스 테두리
        infoBox.setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1));
        
        document.add(infoBox);
    }

    /**
     * 요약 행 추가
     */
    private void addSummaryRow(Table table, String label, String value) {
        table.addCell(new Cell()
            .add(new Paragraph(label)
                .setFont(koreanFont)
                .setFontSize(11)
                .setFontColor(ColorConstants.DARK_GRAY))
            .setBackgroundColor(LIGHT_BG)
            .setPadding(10)
            .setBorder(new SolidBorder(ColorConstants.WHITE, 2)));
        
        table.addCell(new Cell()
            .add(new Paragraph(value)
                .setFont(koreanFont)
                .setFontSize(11))
            .setPadding(10)
            .setBorder(new SolidBorder(ColorConstants.WHITE, 2)));
    }
    
    /**
     * ⭐ 포트폴리오 콘텐츠 (핵심)
     */
    private void addPortfolioContent(Document document, PortfolioGuide guide) {
        if (guide.getGuideContent() == null || guide.getGuideContent().trim().isEmpty()) {
            Paragraph emptyMsg = new Paragraph("작성된 내용이 없습니다.")
                .setFont(koreanFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(100);
            document.add(emptyMsg);
            return;
        }
        
        try {
            GuideContentData content = objectMapper.readValue(
                guide.getGuideContent(), GuideContentData.class);
            
            for (int i = 0; i < content.getSteps().size(); i++) {
                GuideStepData step = content.getSteps().get(i);
                
                // 각 단계마다 새 페이지 (첫 단계 제외)
                if (i > 0) {
                    document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                }
                
                // ✅ 새로운 addStepSection 호출
                addStepSection(document, step);
            }
            
        } catch (Exception e) {
            log.error("콘텐츠 파싱 실패", e);
            Paragraph errorMsg = new Paragraph("콘텐츠를 불러올 수 없습니다.")
                .setFont(koreanFont)
                .setFontColor(ColorConstants.RED);
            document.add(errorMsg);
        }
    }
    
    /**
     * ⭐ 단계별 섹션 (PPT 형식)
     */
    private void addStepSection(Document document, GuideStepData step) {
        // ✅ 단계 구분 페이지 (01, 02, 03 큰 글씨)
        addStepDividerPage(document, step);
        
        // ✅ 항목들은 각각 별도 페이지
        if (step.getItems() != null && !step.getItems().isEmpty()) {
            for (int i = 0; i < step.getItems().size(); i++) {
                document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                addPPTStyleItemPage(document, step, step.getItems().get(i), i + 1);
            }
        }
    }
    
    /**
     * ⭐ 단계 구분 페이지 (PPT 챕터 페이지)
     */
    private void addStepDividerPage(Document document, GuideStepData step) {
        // 좌측 큰 번호
        Paragraph bigNumber = new Paragraph(String.format("%02d", step.getStepNumber()))
            .setFont(koreanBoldFont)
            .setFontSize(120)
            .setBold()
            .setFontColor(new DeviceRgb(200, 200, 200))  // 연한 회색
            .setMarginTop(150)
            .setMarginLeft(60);
        document.add(bigNumber);
        
        // 단계 제목
        Paragraph stepTitle = new Paragraph(step.getStepTitle())
            .setFont(koreanBoldFont)
            .setFontSize(36)
            .setBold()
            .setMarginTop(-80)
            .setMarginLeft(60)
            .setMarginBottom(30);
        document.add(stepTitle);
        
        // 구분선
        Table line = new Table(UnitValue.createPercentArray(1))
            .setWidth(UnitValue.createPercentValue(80))
            .setMarginLeft(60);
        
        Cell lineCell = new Cell()
            .add(new Paragraph(""))
            .setHeight(3)
            .setBackgroundColor(PRIMARY_COLOR)
            .setBorder(null);
        line.addCell(lineCell);
        document.add(line);
        
        // 우측 상단 페이지 번호 표시 (선택사항)
        Paragraph stepInfo = new Paragraph(String.format("%02d. %s", 
                step.getStepNumber(), step.getStepTitle()))
            .setFont(koreanFont)
            .setFontSize(11)
            .setTextAlignment(TextAlignment.RIGHT)
            .setFontColor(ColorConstants.GRAY)
            .setMarginTop(-40)
            .setMarginRight(40);
        document.add(stepInfo);
    }
    
    /**
     * ⭐ PPT 스타일 항목 페이지 (수정된 버전)
     */
    private void addPPTStyleItemPage(Document document, GuideStepData step, 
                                      GuideItemData item, int itemNumber) {
        
        // 우측 상단 네비게이션
        Paragraph navigation = new Paragraph(String.format("%02d %s", 
                step.getStepNumber(), step.getStepTitle()))
            .setFont(koreanFont)
            .setFontSize(10)
            .setTextAlignment(TextAlignment.RIGHT)
            .setFontColor(ColorConstants.GRAY)
            .setMarginTop(30)
            .setMarginRight(40)
            .setMarginBottom(20);
        document.add(navigation);
        
        // 좌측 단계 번호 (작게)
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{15, 85}))
            .setWidth(UnitValue.createPercentValue(100))
            .setMarginBottom(30);
        
        // 좌측: 단계 번호
        Cell numberCell = new Cell()
            .add(new Paragraph(String.format("%02d", step.getStepNumber()))
                .setFont(koreanBoldFont)
                .setFontSize(48)
                .setBold()
                .setFontColor(new DeviceRgb(200, 200, 200)))
            .setBorder(null)
            .setVerticalAlignment(VerticalAlignment.TOP);
        headerTable.addCell(numberCell);
        
        // 우측: 제목 + 구분선
        Table titleBlock = new Table(UnitValue.createPercentArray(1))
            .setWidth(UnitValue.createPercentValue(100));
        
        // 제목
        Cell titleCell = new Cell()
            .add(new Paragraph(item.getTitle())
                .setFont(koreanBoldFont)
                .setFontSize(20)
                .setBold())
            .setBorder(null)
            .setPaddingBottom(10);
        titleBlock.addCell(titleCell);
        
        // 구분선
        Cell lineCell = new Cell()
            .add(new Paragraph(""))
            .setHeight(2)
            .setBackgroundColor(PRIMARY_COLOR)
            .setBorder(null);
        titleBlock.addCell(lineCell);
        
        headerTable.addCell(new Cell().add(titleBlock).setBorder(null));
        document.add(headerTable);
        
        // 상태 표시
        String statusText = "완료".equals(item.getStatus()) ? "✓ 완료" : "○ 작성 중";
        Color statusColor = "완료".equals(item.getStatus()) ? ACCENT_COLOR : ColorConstants.GRAY;
        
        Paragraph status = new Paragraph(statusText)
            .setFont(koreanFont)
            .setFontSize(12)
            .setFontColor(statusColor)
            .setMarginLeft(80)
            .setMarginBottom(30);
        document.add(status);
        
        // 내용 영역 (박스 형태)
        if (item.getContent() != null && !item.getContent().trim().isEmpty()) {
            Table contentBox = new Table(UnitValue.createPercentArray(1))
                .setWidth(UnitValue.createPercentValue(85))
                .setMarginLeft(80)
                .setMarginTop(20);
            
            Cell contentCell = new Cell()
                .add(new Paragraph(item.getContent())
                    .setFont(koreanFont)
                    .setFontSize(11)
                    .setFixedLeading(18))  // 줄간격
                .setBackgroundColor(LIGHT_BG)
                .setPadding(20)
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1));
            
            contentBox.addCell(contentCell);
            document.add(contentBox);
        } else {
            // 미작성 상태
            Paragraph emptyContent = new Paragraph("(작성된 내용이 없습니다)")
                .setFont(koreanFont)
                .setFontSize(11)
                .setItalic()
                .setFontColor(ColorConstants.LIGHT_GRAY)
                .setMarginLeft(80)
                .setMarginTop(40)
                .setTextAlignment(TextAlignment.CENTER);
            document.add(emptyContent);
        }
    }
    
    /**
     * ⭐ 헤더/푸터 이벤트 핸들러
     */
    private class PortfolioHeaderFooterHandler implements IEventHandler {
        private String projectTitle;
        private PdfFont font;
        
        public PortfolioHeaderFooterHandler(String projectTitle, PdfFont font) {
            this.projectTitle = projectTitle;
            this.font = font;
        }
        
        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdfDoc = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int pageNumber = pdfDoc.getPageNumber(page);
            
            Rectangle pageSize = page.getPageSize();
            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);
            
            try (Canvas canvas = new Canvas(pdfCanvas, pageSize)) {
                // 헤더 (1페이지 제외)
                if (pageNumber > 1) {
                    canvas.showTextAligned(
                        new Paragraph(projectTitle)
                            .setFont(font)
                            .setFontSize(9)
                            .setFontColor(ColorConstants.GRAY),
                        40, pageSize.getTop() - 30,
                        TextAlignment.LEFT
                    );
                }
                
                // 푸터 (페이지 번호)
                canvas.showTextAligned(
                    new Paragraph(String.valueOf(pageNumber))
                        .setFont(font)
                        .setFontSize(9)
                        .setFontColor(ColorConstants.GRAY),
                    pageSize.getWidth() / 2, 30,
                    TextAlignment.CENTER
                );
            } catch (Exception e) {
                log.error("헤더/푸터 렌더링 실패", e);
            }
        }
    }
    
    /**
     * ⭐ 회원별 전체 포트폴리오 (멀티 프로젝트)
     */
    public void generateMemberGuidesPdf(List<PortfolioGuide> guides, HttpServletResponse response) throws IOException {
        log.info("회원 포트폴리오 PDF 생성 - 프로젝트 수: {}", guides.size());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            initFonts();
            
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);
            document.setMargins(40, 40, 60, 40);
            
            // 통합 표지
            addMemberCoverPage(document, guides.size());
            
            // 각 프로젝트별 섹션
            for (int i = 0; i < guides.size(); i++) {
                if (i > 0) {
                    document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                }
                
                PortfolioGuide guide = guides.get(i);
                
                // 프로젝트 구분 페이지
                addProjectDividerPage(document, i + 1, guide);
                
                // 프로젝트 내용
                addProjectOverview(document, guide);
                addPortfolioContent(document, guide);
            }
            
            document.close();
            
            String fileName = String.format("Portfolio_Collection_%s.pdf",
                DateTimeFormatter.ofPattern("yyyyMMdd").format(java.time.LocalDateTime.now()));
            
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", 
                "attachment; filename=\"" + java.net.URLEncoder.encode(fileName, "UTF-8") + "\"");
            response.setContentLength(baos.size());
            
            baos.writeTo(response.getOutputStream());
            response.getOutputStream().flush();
            
            log.info("회원 포트폴리오 PDF 생성 완료");
            
        } catch (Exception e) {
            log.error("PDF 생성 실패", e);
            throw new IOException("PDF 생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * 회원 포트폴리오 표지
     */
    private void addMemberCoverPage(Document document, int projectCount) {
        Paragraph title = new Paragraph("PROJECT PORTFOLIO")
            .setFont(koreanBoldFont)
            .setFontSize(36)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(200)
            .setMarginBottom(40)
            .setFontColor(PRIMARY_COLOR);
        document.add(title);
        
        Paragraph subtitle = new Paragraph(String.format("총 %d개의 프로젝트", projectCount))
            .setFont(koreanFont)
            .setFontSize(18)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20);
        document.add(subtitle);
        
        Paragraph date = new Paragraph(java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")))
            .setFont(koreanFont)
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(ColorConstants.GRAY);
        document.add(date);
        
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
    }
    
    /**
     * 프로젝트 구분 페이지
     */
    private void addProjectDividerPage(Document document, int projectNumber, PortfolioGuide guide) {
        Paragraph projectLabel = new Paragraph(String.format("PROJECT %d", projectNumber))
            .setFont(koreanBoldFont)
            .setFontSize(48)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(250)
            .setMarginBottom(40)
            .setFontColor(PRIMARY_COLOR);
        document.add(projectLabel);
        
        Paragraph projectName = new Paragraph(guide.getTitle())
            .setFont(koreanFont)
            .setFontSize(24)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20);
        document.add(projectName);
        
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
    }
}