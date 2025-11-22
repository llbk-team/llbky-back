package com.example.demo.ai.portfolioguide;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.dto.Member;
import com.example.demo.portfolio.dao.PortfolioStandardDao;
import com.example.demo.portfolio.dto.PortfolioGuideResult;
import com.example.demo.portfolio.dto.request.PortfolioGuideRequest;
import com.example.demo.portfolio.entity.PortfolioStandard;

import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 가이드 코칭을 담당하는 에이전트
 * - 자동으로 MemberDao에서 회원 정보 조회
 * - 자동으로 PortfolioStandardDao에서 직무별 표준 가이드라인 조회
 * - LLM을 이용하여 실시간 코칭 제공
 * - 디자이너의 경우 5단계 필수 항목 평가
 */
@Component
@Slf4j
public class PortfolioGuideAgent {

    private final ChatClient chatClient;
    private final MemberDao memberDao;
    private final PortfolioStandardDao portfolioStandardDao;

    public PortfolioGuideAgent(
            ChatClient.Builder chatClientBuilder,
            MemberDao memberDao,
            PortfolioStandardDao portfolioStandardDao) {
        this.chatClient = chatClientBuilder.build();
        this.memberDao = memberDao;
        this.portfolioStandardDao = portfolioStandardDao;
    }

    /**
     * 포트폴리오 가이드 코칭 평가 수행
     * @param request 코칭 요청 정보
     * @return 코칭 결과
     */
    public PortfolioGuideResult evaluate(PortfolioGuideRequest request) {
        // 1. 회원 정보 자동 조회
        Member member = null;
        if (request.getMemberId() != null) {
            member = memberDao.findById(request.getMemberId());
            log.debug("회원 정보 조회 완료 - ID: {}, 직군: {}, 직무: {}", 
                request.getMemberId(), 
                member != null ? member.getJobGroup() : "null",
                member != null ? member.getJobRole() : "null");
        }

        // 2. 직무별 표준 가이드라인 자동 조회
        List<PortfolioStandard> jobStandards = loadStandards(request, member);
        log.debug("표준 가이드라인 조회 완료 - 개수: {}", jobStandards != null ? jobStandards.size() : 0);

        // 3. LLM 프롬프트 생성 및 호출
        String prompt = buildPrompt(request, jobStandards, member);
        
        BeanOutputConverter<PortfolioGuideResult> converter = 
            new BeanOutputConverter<>(PortfolioGuideResult.class);

        PortfolioGuideResult result = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(converter);

        return result != null ? result : createDefaultResult();
    }

    /**
     * 직무별 표준 가이드라인 조회
     */
    private List<PortfolioStandard> loadStandards(PortfolioGuideRequest request, Member member) {
        // 직군 결정
        String jobGroup = "개발자"; // 기본값
        if (request.getJobGroup() != null) {
            jobGroup = request.getJobGroup();
        } else if (member != null && member.getJobGroup() != null) {
            jobGroup = member.getJobGroup();
        }

        // 직무 결정
        String jobRole = "일반"; // 기본값
        if (request.getJobRole() != null) {
            jobRole = request.getJobRole();
        } else if (member != null && member.getJobRole() != null) {
            jobRole = member.getJobRole();
        }

        // 직무별 가이드라인 조회
        List<PortfolioStandard> standards = portfolioStandardDao.selectStandardsByJobInfo(jobGroup, jobRole);
        
        // 없으면 전체 가이드라인 사용
        if (standards == null || standards.isEmpty()) {
            standards = portfolioStandardDao.selectAllStandards();
            log.info("직무별 가이드라인 없음 - 기본 가이드 사용 (직군: {}, 직무: {})", jobGroup, jobRole);
        } else {
            log.info("직무별 가이드라인 조회 성공 (직군: {}, 직무: {}, 개수: {})", jobGroup, jobRole, standards.size());
        }
        
        return standards;
    }

    /**
     * LLM 프롬프트 생성
     * - 사용자 입력 및 컨텍스트 정보
     * - 직무별 표준 가이드라인
     * - 디자이너의 경우 5단계 평가 항목 포함
     */
    private String buildPrompt(PortfolioGuideRequest request, List<PortfolioStandard> standards, Member member) {
        StringBuilder sb = new StringBuilder();

        sb.append("# 포트폴리오 가이드 실시간 코칭 에이전트\n\n");
        sb.append("당신은 포트폴리오 작성을 코칭하는 전문 AI 가이드입니다.\n");
        sb.append("사용자가 입력하는 내용을 실시간으로 분석하여 직군/직무 기준에 맞는 구체적인 피드백을 제공하세요.\n\n");

        // 회원 정보
        String jobGroup = extractJobGroup(request, member);
        String jobRole = extractJobRole(request, member);
        Integer careerYears = extractCareerYears(request, member);

        sb.append(String.format("## 사용자 정보\n- 직군: %s\n- 직무: %s\n- 경력: %d년\n\n", 
            jobGroup, jobRole, careerYears));

        // 현재 입력 상황
        sb.append("## 현재 작성 중인 항목\n");
        sb.append(String.format("- 입력 필드: %s\n", request.getInputFieldType()));
        sb.append(String.format("- 현재 단계: %d단계\n", request.getCurrentStep()));
        sb.append(String.format("- 사용자 입력 내용:\n\"%s\"\n\n", 
            request.getUserInput() != null ? request.getUserInput() : ""));

        // 직무별 표준 가이드라인 (DB에서 조회한 promptTemplate 사용)
        if (standards != null && !standards.isEmpty()) {
            sb.append("## 직무별 표준 가이드라인 및 평가 지침\n\n");
            for (PortfolioStandard s : standards) {
                sb.append(String.format("### %s\n", s.getStandardName()));
                sb.append(String.format("%s\n\n", s.getStandardDescription()));
                
                // DB에 저장된 promptTemplate을 평가 지침으로 사용
                if (s.getPromptTemplate() != null && !s.getPromptTemplate().isEmpty()) {
                    sb.append(s.getPromptTemplate());
                    sb.append("\n\n");
                }
            }
        } else {
            // 표준 가이드라인이 없는 경우 기본 평가 지침
            sb.append("## 일반 평가 지침\n\n");
            sb.append("입력된 내용을 다음 기준으로 평가하세요:\n");
            sb.append("- 전체 적절성 점수 (1-10점)\n");
            sb.append("- 구체적인 개선 제안 3가지\n");
            sb.append("- 해당 직무에 적합한 예시 2-3개\n");
            sb.append("- 다음 단계 작성 가이드\n\n");
        }

        // 출력 형식
        sb.append("## 출력 형식 (엄격한 JSON)\n\n");
        sb.append("반드시 아래 JSON 형식으로만 응답하세요:\n\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"success\": true,\n");
        sb.append("  \"coachingMessage\": \"친근하고 구체적인 코칭 메시지 (2-3문장)\",\n");
        sb.append("  \"appropriatenessScore\": 7,\n");
        sb.append("  \"suggestions\": [\n");
        sb.append("    \"구체적인 개선 제안 1\",\n");
        sb.append("    \"구체적인 개선 제안 2\",\n");
        sb.append("    \"구체적인 개선 제안 3\"\n");
        sb.append("  ],\n");
        sb.append("  \"examples\": [\n");
        sb.append("    \"해당 직무에 맞는 좋은 예시 1\",\n");
        sb.append("    \"해당 직무에 맞는 좋은 예시 2\"\n");
        sb.append("  ],\n");
        sb.append("  \"nextStepGuide\": \"다음 입력 항목에 대한 가이드\",\n");
        sb.append("  \"progressPercentage\": 50\n");
        sb.append("}\n");
        sb.append("```\n\n");
        sb.append("**주의**: JSON 형식을 정확히 지켜주세요. 다른 텍스트는 포함하지 마세요.\n");

        return sb.toString();
    }

    // =============== 헬퍼 메서드 ===============

    private String extractJobGroup(PortfolioGuideRequest request, Member member) {
        if (request.getJobGroup() != null) return request.getJobGroup();
        if (member != null && member.getJobGroup() != null) return member.getJobGroup();
        return "개발자";
    }

    private String extractJobRole(PortfolioGuideRequest request, Member member) {
        if (request.getJobRole() != null) return request.getJobRole();
        if (member != null && member.getJobRole() != null) return member.getJobRole();
        return "일반";
    }

    private Integer extractCareerYears(PortfolioGuideRequest request, Member member) {
        if (request.getCareerYears() != null) return request.getCareerYears();
        if (member != null && member.getCareerYears() != null) return member.getCareerYears();
        return 1;
    }

    private PortfolioGuideResult createDefaultResult() {
        return PortfolioGuideResult.builder()
                .success(true)
                .coachingMessage("입력해주신 내용을 확인했습니다. 계속 작성해주세요.")
                .appropriatenessScore(5)
                .progressPercentage(10)
                .build();
    }
}
