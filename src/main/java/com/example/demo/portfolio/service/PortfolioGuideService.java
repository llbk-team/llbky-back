package com.example.demo.portfolio.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ai.portfolioguide.PortfolioGuideAgent;
import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.dto.entity.Member;
import com.example.demo.portfolio.dao.PortfolioGuideDao;
import com.example.demo.portfolio.dao.PortfolioStandardDao;
import com.example.demo.portfolio.dto.GuideResult;
import com.example.demo.portfolio.dto.request.GuideRequest;
import com.example.demo.portfolio.entity.PortfolioGuide;
import com.example.demo.portfolio.entity.PortfolioStandard;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor // ⭐ 올바른 생성자 주입
@Slf4j
public class PortfolioGuideService {
    
    // ⭐ final 필드들로 생성자 주입
    private final PortfolioGuideDao portfolioGuideDao;
    private final PortfolioGuideAgent portfolioGuideAgent;
    private final ObjectMapper objectMapper;
    private final MemberDao memberDao;
    private final PortfolioStandardDao portfolioStandardDao;

    /**
     * 메인 코칭 메서드
     */
    public GuideResult provideCoaching(GuideRequest request) throws Exception {
        LocalDateTime startTime = LocalDateTime.now();
        
        log.info("코칭 요청 시작 - memberId: {}, inputFieldType: {}, userInput: {}", 
            request.getMemberId(), request.getInputFieldType(), request.getUserInput());
        
        // 1. Member 조회
        Member member = memberDao.findById(request.getMemberId());
        if (member == null) {
            throw new NoSuchElementException("존재하지 않는 회원입니다: " + request.getMemberId());
        }
        
        // 2. PortfolioStandard 조회
        List<PortfolioStandard> standards = loadStandards(request, member);
        
        // 3. AI 에이전트로 코칭 수행
        GuideResult result = portfolioGuideAgent.evaluate(request, member, standards);
        
        Duration duration = Duration.between(startTime, LocalDateTime.now());
        log.info("AI 코칭 완료 - 처리시간: {}ms", duration.toMillis());
        
        // 4. 가이드 ID가 있으면 피드백 저장
        Integer guideId = request.getGuideId();
        if (guideId != null) {
            saveFeedbackToDatabase(guideId, result);
            log.info("피드백 저장 완료 - guideId: {}", guideId);
        }
        
        return result;
    }
    
    /**
     * 직무별 표준 가이드라인 조회
     */
    private List<PortfolioStandard> loadStandards(GuideRequest request, Member member) {
        List<PortfolioStandard> standards = null;
        
        // 1. standardId가 지정되면 해당 표준만 조회
        if (request.getStandardId() != null) {
            PortfolioStandard standard = portfolioStandardDao.selectStandardById(request.getStandardId());
            if (standard != null) {
                standards = List.of(standard);
                log.info("특정 표준 적용: {} (ID: {})", standard.getStandardName(), request.getStandardId());
            }
        }
        
        // 2. 없으면 직군/직무별 가이드라인 사용
        if (standards == null || standards.isEmpty()) {
            standards = portfolioStandardDao.selectStandardsByJobInfo(
                member.getJobGroup(),
                member.getJobRole()
            );
            log.info("직군/직무별 표준 적용: {} {}개", member.getJobGroup(), standards != null ? standards.size() : 0);
        }
        
        // 3. 그것도 없으면 전체 표준 사용
        if (standards == null || standards.isEmpty()) {
            standards = portfolioStandardDao.selectAllStandards();
            log.warn("기본 표준 적용: 전체 {}개", standards != null ? standards.size() : 0);
        }
        
        return standards;
    }

    /**
     * 가이드 ID로 조회
     */
    public PortfolioGuide getGuideById(Integer guideId) {

        

        return portfolioGuideDao.selectGuideById(guideId);
    }

    /**
     * 회원별 가이드 목록 조회
     */
    public List<PortfolioGuide> getGuidesByMemberId(Integer memberId) {
        
        return portfolioGuideDao.selectGuidesByMemberId(memberId);
    }

    /**
     * ⭐ 수정: 저장된 피드백 조회 (JSONB → GuideResult 변환)
     */
    public GuideResult getGuideFeedback(Integer guideId) throws com.fasterxml.jackson.core.JsonProcessingException {
        
        
        PortfolioGuide guide = portfolioGuideDao.selectGuideById(guideId);
        if (guide == null) {
            throw new NoSuchElementException("존재하지 않는 가이드입니다: " + guideId);
        }
        
        String feedbackJson = guide.getGuideFeedback();
        if (feedbackJson == null || feedbackJson.trim().isEmpty()) {
            throw new NoSuchElementException("저장된 피드백이 없습니다: " + guideId);
        }
        
        // JSONB → GuideResult 객체 변환
        GuideResult feedback = objectMapper.readValue(feedbackJson, GuideResult.class);
        log.info("피드백 조회 성공 - 점수: {}", feedback.getAppropriatenessScore());
        
        return feedback;
    }

    /**
     * ⭐ 수정: AI 피드백을 JSONB로 저장
     */
    @Transactional
    private void saveFeedbackToDatabase(Integer guideId, GuideResult feedback) 
            throws com.fasterxml.jackson.core.JsonProcessingException {
        
        log.info("피드백 저장 시작 - guideId: {}", guideId);
        
        // GuideResult → JSON 문자열 변환
        String feedbackJson = objectMapper.writeValueAsString(feedback);
        
        // DB 업데이트를 위한 파라미터 맵 생성
        Map<String, Object> updateParams = new HashMap<>();
        updateParams.put("guideId", guideId);
        updateParams.put("guideFeedback", feedbackJson);
        
        // DB 업데이트 실행
        int updated = portfolioGuideDao.updateGuideFeedback(updateParams);
        
        if (updated == 0) {
            throw new IllegalStateException("가이드를 찾을 수 없습니다: " + guideId);
        }
        
        log.info("피드백 저장 완료 - guideId: {}, 업데이트된 행: {}", guideId, updated);
    }

    /**
     * 가이드 생성 또는 조회 (없으면 새로 생성)
     */
    @Transactional
    public PortfolioGuide getOrCreateGuide(GuideRequest request) throws com.fasterxml.jackson.core.JsonProcessingException {
        // 1. 기존 가이드 조회 시도 (guideId가 있는 경우)
        if (request.getGuideId() != null) {
            PortfolioGuide existing = portfolioGuideDao.selectGuideById(request.getGuideId());
            if (existing != null) {
                return existing;
            }
        }

        // 2. 새 가이드 생성
        PortfolioGuide newGuide = new PortfolioGuide();
        newGuide.setMemberId(request.getMemberId());
        newGuide.setStandardId(request.getStandardId());
        newGuide.setTitle(request.getInputFieldType() + " 작성 가이드");
        newGuide.setCompletionPercentage(0);
        newGuide.setCurrentStep(request.getCurrentStep() != null ? request.getCurrentStep() : 1);
        newGuide.setTotalSteps(5);
        newGuide.setIsCompleted(false);

        // 초기 콘텐츠 설정
        String initialContent = objectMapper.writeValueAsString(
            Map.of(
                "fieldType", request.getInputFieldType(),
                "userInput", request.getUserInput() != null ? request.getUserInput() : "",
                "createdAt", LocalDateTime.now().toString()
            )
        );
        newGuide.setGuideContent(initialContent);

        portfolioGuideDao.insertGuide(newGuide);
        log.info("새 가이드 생성 완료 - guideId: {}, memberId: {}", 
            newGuide.getGuideId(), request.getMemberId());
        
        return newGuide;
    }
}