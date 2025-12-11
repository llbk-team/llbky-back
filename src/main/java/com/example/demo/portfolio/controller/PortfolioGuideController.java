package com.example.demo.portfolio.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.portfolio.dto.GuideResult;
import com.example.demo.portfolio.dto.request.GuideProgressSaveRequest;
import com.example.demo.portfolio.dto.request.GuideRequest;
import com.example.demo.portfolio.dto.request.GuideSearchRequest;
import com.example.demo.portfolio.entity.PortfolioGuide;
import com.example.demo.portfolio.service.PortfolioGuideService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 가이드 코칭 컨트롤러
 * 
 * - guide_feedback 필드는 선택적 사용
 * - PDF 다운로드 지원
 */
@RestController // Spring REST 컨트롤러
@RequestMapping("/portfolio-guide") // 기본 경로 설정
@Slf4j // Lombok 로깅
public class PortfolioGuideController {

    @Autowired // Spring 의존성 주입
    private PortfolioGuideService portfolioGuideService;

    /**
     * ⭐ 새 가이드 생성
     * POST http://localhost:8080/portfolio-guide/create

     * @param request 가이드 생성 요청 (JSON → @RequestBody로 매핑)
     * @return 생성된 PortfolioGuide 엔티티
     */
    @PostMapping("/create") // POST 요청 매핑
    public ResponseEntity<PortfolioGuide> createGuide(@RequestBody GuideRequest request) {
        try {
            // INFO 로그: 가이드 생성 요청 정보 기록
            log.info("가이드 생성 - memberId: {}, title: {}",
                    request.getMemberId(), request.getTitle());

            // 서비스 계층 호출: 새 가이드 생성
            PortfolioGuide guide = portfolioGuideService.createGuide(request);

            // HTTP 200 OK와 함께 생성된 가이드 반환
            return ResponseEntity.ok(guide);

        } catch (IllegalArgumentException e) { // 잘못된 요청 (예: memberId 없음)
            log.warn("가이드 생성 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().build(); // HTTP 400 Bad Request

        } catch (Exception e) { // 기타 예외 (서버 오류)
            log.error("가이드 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError().build(); // HTTP 500 Internal Server Error
        }
    }

    /**
     * ⭐ 가이드 내용 저장 (사용자 최종 답변)
     * PUT http://localhost:8080/portfolio-guide/save
     * Content-Type: application/json
     * Body: {"guideId": 1, "currentStep": 2, "guideSteps": [...]}
     * 
     * @param request 가이드 저장 요청 (JSON → @RequestBody로 매핑)
     * @return 업데이트된 PortfolioGuide 엔티티
     */
    @PutMapping("/save") // PUT 요청 매핑 (업데이트 용도)
    public ResponseEntity<PortfolioGuide> saveGuide(@RequestBody GuideProgressSaveRequest request) {
        try {
            // INFO 로그: 가이드 저장 요청 정보 기록
            log.info("가이드 저장 - guideId: {}, currentStep: {}",
                    request.getGuideId(), request.getCurrentStep());

            // 서비스 계층 호출: 가이드 내용 저장 (guide_content 업데이트)
            PortfolioGuide savedGuide = portfolioGuideService.saveGuide(request);

            // HTTP 200 OK와 함께 업데이트된 가이드 반환
            return ResponseEntity.ok(savedGuide);

        } catch (IllegalArgumentException e) { // 잘못된 요청 (예: guideId 없음)
            log.warn("가이드 저장 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().build(); // HTTP 400 Bad Request

        } catch (Exception e) { // 기타 예외
            log.error("가이드 저장 중 오류 발생", e);
            return ResponseEntity.internalServerError().build(); // HTTP 500
        }
    }

    /**
     * ⭐ 실시간 AI 코칭
     * POST http://localhost:8080/portfolio-guide/coaching
     * Content-Type: application/json
     * Body: {"memberId": 1, "userInput": "...", "inputFieldType": "프로젝트명"}
     * 
     * @param request AI 코칭 요청 (JSON → @RequestBody로 매핑)
     * @return AI 코칭 결과 (점수, 피드백, 제안사항)
     */
    @PostMapping("/coaching") // POST 요청 매핑
    public ResponseEntity<GuideResult> getRealtimeCoaching(@RequestBody GuideRequest request) {
        try {
            // INFO 로그: AI 코칭 요청 정보 기록
            log.info("AI 코칭 요청 - memberId: {}, fieldType: {}",
                    request.getMemberId(), request.getInputFieldType());

            // 서비스 계층 호출: 실시간 AI 코칭 (DB 저장 안함)
            GuideResult result = portfolioGuideService.getRealtimeCoaching(request);

            // HTTP 200 OK와 함께 AI 코칭 결과 반환
            return ResponseEntity.ok(result);

        } catch (Exception e) { // AI 호출 실패 등
            log.error("AI 코칭 실패", e);
            return ResponseEntity.internalServerError().build(); // HTTP 500
        }
    }

    /**
     * ⭐ 가이드 단일 조회 (@ModelAttribute 사용)
     * GET http://localhost:8080/portfolio-guide/{guideId}?memberId=1
     * 
     * @param guideId 가이드 ID (URL 경로에서 추출)
     * @param request 검색 조건 (쿼리 파라미터 → @ModelAttribute로 매핑)
     * @return 조회된 PortfolioGuide 엔티티
     */
    @GetMapping("/{guideId}") // GET 요청 매핑 (경로 변수 포함)
    public ResponseEntity<PortfolioGuide> getGuide(@PathVariable Integer guideId) { // 쿼리 파라미터 바인딩

        // 서비스 계층 호출: 가이드 조회
        PortfolioGuide guide = portfolioGuideService.getGuideById(guideId);

        // 가이드가 없으면 HTTP 404 Not Found 반환
        if (guide == null) {
            return ResponseEntity.notFound().build();
        }

        // HTTP 200 OK와 함께 가이드 반환
        return ResponseEntity.ok(guide);
    }

    /**
     * ⭐ 회원별 가이드 목록 조회 (@ModelAttribute 사용)
     * GET http://localhost:8080/portfolio-guide/member/{memberId}
     * 
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<PortfolioGuide>> getGuidesByMember(@PathVariable Integer memberId) {
        log.info("회원별 가이드 목록 조회 - memberId: {}", memberId);
        List<PortfolioGuide> guides = portfolioGuideService.getGuidesByMemberId(memberId);
        return ResponseEntity.ok(guides);
    }

    /**
     * ⭐ 저장된 AI 피드백 조회 (@ModelAttribute 사용)
    */
    @GetMapping("/{guideId}/feedback") // GET 요청 매핑
    public ResponseEntity<GuideResult> getGuideFeedback(
            @PathVariable Integer guideId, // URL 경로에서 guideId 추출
            @ModelAttribute GuideSearchRequest request) { // 쿼리 파라미터 바인딩

        try {
            // INFO 로그: 저장된 피드백 조회 요청
            log.info("저장된 피드백 조회 - guideId: {}", guideId);

            // PathVariable 값을 request에 설정
            request.setGuideId(guideId);

            // 서비스 계층 호출: guide_feedback 필드 조회
            GuideResult feedback = portfolioGuideService.getStoredFeedback(request.getGuideId());

            // 저장된 피드백이 없으면 HTTP 404 Not Found 반환
            if (feedback == null) {
                return ResponseEntity.notFound().build();
            }

            // HTTP 200 OK와 함께 피드백 반환
            return ResponseEntity.ok(feedback);

        } catch (Exception e) { // JSON 파싱 실패 등
            log.error("피드백 조회 실패", e);
            return ResponseEntity.internalServerError().build(); // HTTP 500
        }
    }

    /**
     * ⭐ PDF 다운로드 (@ModelAttribute 사용)
     * GET http://localhost:8080/portfolio-guide/{guideId}/pdf?memberId=1
     * 
     * @param guideId  가이드 ID (URL 경로에서 추출)
     * @param request  검색 조건 (쿼리 파라미터 → @ModelAttribute로 매핑)
     * @param response HTTP 응답 객체 (PDF 파일 스트림 전송용)
     */
    @GetMapping("/{guideId}/pdf") // GET 요청 매핑
    public void downloadGuidePdf(
            @PathVariable Integer guideId, // URL 경로에서 guideId 추출
            HttpServletResponse response) { // HTTP 응답 객체 (파일 다운로드용)

        try {
            // INFO 로그: PDF 다운로드 요청
            log.info("PDF 다운로드 - guideId: {}", guideId);


            // 서비스 계층 호출: 가이드 조회
            PortfolioGuide guide = portfolioGuideService.getGuideById(guideId);

            // 가이드가 없으면 HTTP 404 Not Found 설정
            if (guide == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404 상태 코드
                return;
            }

            // PDF 생성 및 다운로드 (iText 서비스 호출)
            // response 객체에 PDF 파일 스트림 작성
            portfolioGuideService.generatePdf(guide, response);

        } catch (Exception e) { // PDF 생성 실패
            log.error("PDF 다운로드 실패", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 상태 코드
        }
    }

    /**
     * ⭐ 회원별 전체 가이드 PDF 다운로드
     * GET http://localhost:8080/portfolio-guide/member/{memberId}/pdf
     * 
     * @param memberId 회원 ID (URL 경로에서 추출)
     * @param request  검색 조건 (쿼리 파라미터 → @ModelAttribute로 매핑)
     * @param response HTTP 응답 객체 (PDF 파일 스트림 전송용)
     */
    @GetMapping("/member/{memberId}/pdf") // GET 요청 매핑
    public void downloadMemberGuidesPdf(
            @PathVariable Integer memberId, // URL 경로에서 memberId 추출        
            HttpServletResponse response) { // HTTP 응답 객체

        try {
            // INFO 로그: 회원별 전체 PDF 다운로드 요청
            log.info("회원별 전체 PDF 다운로드 - memberId: {}", memberId);

            // 서비스 계층 호출: 회원별 가이드 목록 조회
            List<PortfolioGuide> guides = portfolioGuideService.getGuidesByMemberId(memberId);

            // 가이드가 없으면 HTTP 404 Not Found 설정
            if (guides.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404 상태 코드
                return;
            }
            log.info("가이드를 pdf로 출력");
            // 다중 가이드 PDF 생성 (모든 가이드를 하나의 PDF로 병합)
            portfolioGuideService.generateMemberPdf(guides, response);

        } catch (Exception e) { // PDF 생성 실패
            log.error("회원별 PDF 다운로드 실패", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 상태 코드
        }
    }
}
