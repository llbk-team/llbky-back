package com.example.demo.portfolio.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.portfolio.dao.PortfolioGuideDao;
import com.example.demo.portfolio.dao.PortfolioStandardDao;
import com.example.demo.portfolio.entity.PortfolioGuide;
import com.example.demo.portfolio.entity.PortfolioStandard;

@RestController
@RequestMapping("/test/portfolio")
public class PortfolioStandardTestController  {
    @Autowired
    private PortfolioStandardDao standardDao;
    
    @Autowired  
    private PortfolioGuideDao guideDao;

    /**
     * 평가 기준 데이터 생성 테스트
     * POST http://localhost:8081/test/portfolio/standards/insert
     */
    @PostMapping("/standards/insert")
    public String insertStandards() {
       
       // 1. 시각적 완성도 평가 기준
    PortfolioStandard visualDesign = new PortfolioStandard();
    visualDesign.setStandardName("시각적 완성도");
    visualDesign.setStandardDescription("색상, 타이포그래피, 레이아웃의 조화와 완성도");
    visualDesign.setPromptTemplate(
        "다음 포트폴리오의 시각적 완성도를 평가해주세요. " +
        "색상 조합의 조화로움, 타이포그래피의 가독성과 적절성, " +
        "레이아웃의 균형과 시각적 흐름을 중심으로 분석하고 " +
        "개선점을 제시해주세요. 내용: {content}"
    );
    visualDesign.setWeightPercentage(25);
    standardDao.insertStandard(visualDesign);

    // 2. UX/UI 설계 능력 평가 기준  
    PortfolioStandard uxuiDesign = new PortfolioStandard();
    uxuiDesign.setStandardName("UX/UI 설계 능력");
    uxuiDesign.setStandardDescription("사용자 경험과 인터페이스 설계의 논리성");
    uxuiDesign.setPromptTemplate(
        "다음 포트폴리오의 UX/UI 설계 능력을 평가해주세요. " +
        "사용자 플로우의 자연스러움, 인터페이스의 직관성, " +
        "사용성과 접근성 고려사항을 중심으로 분석하고 " +
        "구체적인 개선 방안을 제안해주세요. 내용: {content}"
    );
    uxuiDesign.setWeightPercentage(30);
    standardDao.insertStandard(uxuiDesign);

    // 3. 브랜딩 및 아이덴티티 평가 기준
    PortfolioStandard branding = new PortfolioStandard();
    branding.setStandardName("브랜딩 및 아이덴티티");
    branding.setStandardDescription("브랜드 아이덴티티 표현과 일관성");
    branding.setPromptTemplate(
        "다음 포트폴리오의 브랜딩 및 아이덴티티 표현을 평가해주세요. " +
        "브랜드 컨셉의 명확성, 시각적 아이덴티티의 일관성, " +
        "타겟 오디언스와의 적합성을 중심으로 분석하고 " +
        "브랜딩 강화 방안을 제시해주세요. 내용: {content}"
    );
    branding.setWeightPercentage(20);
    standardDao.insertStandard(branding);

    // 4. 창의성 및 혁신성 평가 기준
    PortfolioStandard creativity = new PortfolioStandard();
    creativity.setStandardName("창의성 및 혁신성");
    creativity.setStandardDescription("독창적 아이디어와 혁신적 표현 방식");
    creativity.setPromptTemplate(
        "다음 포트폴리오의 창의성과 혁신성을 평가해주세요. " +
        "아이디어의 독창성, 표현 방식의 참신함, " +
        "기존 관습에서 벗어난 시도들을 중심으로 분석하고 " +
        "더욱 창의적인 발전 방향을 제안해주세요. 내용: {content}"
    );
    creativity.setWeightPercentage(15);
    standardDao.insertStandard(creativity);

    // 5. 프로젝트 프로세스 평가 기준
    PortfolioStandard designProcess = new PortfolioStandard();
    designProcess.setStandardName("프로젝트 프로세스");
    designProcess.setStandardDescription("디자인 프로세스의 체계성과 문제 해결 과정");
    designProcess.setPromptTemplate(
        "다음 포트폴리오의 디자인 프로세스를 평가해주세요. " +
        "문제 정의와 리서치의 충실성, 아이디어 발전 과정의 논리성, " +
        "반복적 개선과 검증 과정을 중심으로 분석하고 " +
        "프로세스 개선 방안을 제시해주세요. 내용: {content}"
    );
    designProcess.setWeightPercentage(10);
    standardDao.insertStandard(designProcess);

    return "디자이너용 포트폴리오 평가 기준 5개 생성 완료! " +
           "(시각적 완성도, UX/UI 설계, 브랜딩, 창의성, 프로세스)";
  }
@PostMapping("/guides/insert-designer")
public String insertDesignerGuides() {
    
    // 1. UI/UX 디자이너용 가이드
    PortfolioGuide uiuxGuide = new PortfolioGuide();
    uiuxGuide.setMemberId(1); // 테스트 회원 ID
   
    uiuxGuide.setTitle("UI/UX 디자이너 포트폴리오 완성 가이드");
    uiuxGuide.setGuideContent(
        "{\n" +
        "  \"sections\": [\n" +
        "    {\n" +
        "      \"title\": \"프로젝트 개요\",\n" +
        "      \"description\": \"프로젝트 목적과 해결하고자 한 문제\",\n" +
        "      \"tips\": [\"사용자 페르소나 명시\", \"핵심 문제 정의\", \"프로젝트 목표 설정\"]\n" +
        "    },\n" +
        "    {\n" +
        "      \"title\": \"리서치 및 분석\",\n" +
        "      \"description\": \"사용자 조사와 경쟁사 분석 결과\",\n" +
        "      \"tips\": [\"사용자 인터뷰 결과\", \"경쟁사 벤치마킹\", \"시장 동향 분석\"]\n" +
        "    },\n" +
        "    {\n" +
        "      \"title\": \"아이디어 발전\",\n" +
        "      \"description\": \"스케치부터 와이어프레임까지의 과정\",\n" +
        "      \"tips\": [\"아이디어 스케치\", \"사용자 여정 맵\", \"정보 구조도\"]\n" +
        "    },\n" +
        "    {\n" +
        "      \"title\": \"프로토타입\",\n" +
        "      \"description\": \"인터랙션과 사용성 검증\",\n" +
        "      \"tips\": [\"클릭 가능한 프로토타입\", \"사용성 테스트 결과\", \"피드백 반영 과정\"]\n" +
        "    },\n" +
        "    {\n" +
        "      \"title\": \"최종 디자인\",\n" +
        "      \"description\": \"완성된 디자인과 디자인 시스템\",\n" +
        "      \"tips\": [\"최종 화면 디자인\", \"컴포넌트 시스템\", \"스타일 가이드\"]\n" +
        "    }\n" +
        "  ]\n" +
        "}"
    );
    
    guideDao.insertGuide(uiuxGuide);

    // 2. 브랜드 디자이너용 가이드
    PortfolioGuide brandGuide = new PortfolioGuide();
    brandGuide.setMemberId(1); // 테스트 회원 ID
    
    brandGuide.setTitle("브랜드 디자이너 포트폴리오 전략 가이드");
    brandGuide.setGuideContent(
        "{\n" +
        "  \"sections\": [\n" +
        "    {\n" +
        "      \"title\": \"브랜드 전략\",\n" +
        "      \"description\": \"브랜드 포지셔닝과 핵심 메시지\",\n" +
        "      \"tips\": [\"브랜드 아이덴티티 정의\", \"타겟 오디언스 분석\", \"브랜드 차별화 포인트\"]\n" +
        "    },\n" +
        "    {\n" +
        "      \"title\": \"로고 개발\",\n" +
        "      \"description\": \"로고 컨셉부터 최종 완성까지\",\n" +
        "      \"tips\": [\"로고 컨셉 스케치\", \"타이포그래피 선택\", \"심볼 의미 설명\"]\n" +
        "    },\n" +
        "    {\n" +
        "      \"title\": \"브랜드 시스템\",\n" +
        "      \"description\": \"일관된 브랜드 경험을 위한 시스템\",\n" +
        "      \"tips\": [\"컬러 팔레트\", \"폰트 시스템\", \"그래픽 요소\"]\n" +
        "    },\n" +
        "    {\n" +
        "      \"title\": \"어플리케이션\",\n" +
        "      \"description\": \"다양한 매체에서의 브랜드 적용\",\n" +
        "      \"tips\": [\"명함/스테이셔너리\", \"웹/모바일 적용\", \"패키징 디자인\"]\n" +
        "    }\n" +
        "  ]\n" +
        "}"
    );
    
    guideDao.insertGuide(brandGuide);

    return "디자이너용 포트폴리오 가이드 2개 생성 완료! " +
           "(UI/UX 디자이너용, 브랜드 디자이너용)";
}
}
