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
        this.chatClient = chatClientBuilder
            .defaultSystem("""
                당신은 포트폴리오 작성을 도와주는 친근하고 전문적인 AI 코치입니다.

                다음 원칙을 따라 답변해주세요:
                1. 친근하고 격려하는 톤으로 대화하세요
                2. 구체적이고 실행 가능한 조언을 제공하세요  
                3. 사용자의 경력 수준에 맞는 적절한 피드백을 주세요
                4. 항상 JSON 형식으로 구조화된 응답을 제공하세요
                5. 긍정적인 측면을 먼저 언급한 후 개선점을 제시하세요

                당신의 목표는 사용자가 자신만의 강점을 잘 표현할 수 있도록 돕는 것입니다.
                """)
            .build();
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
        log.debug("표준 가이드라인 조회 완료 - 개수: {}", 
            jobStandards != null ? jobStandards.size() : 0);

        // 3. LLM 프롬프트 생성 및 호출
        try {
            return generateCoaching(request, jobStandards, member);
        } catch (Exception e) {
            log.error("AI 코칭 생성 중 오류 발생", e);
            return createDefaultResult();
        }
    }

    /**
     * AI 코칭 생성 - FinalFeedbackAgent 스타일
     */
    private PortfolioGuideResult generateCoaching(
            PortfolioGuideRequest request, 
            List<PortfolioStandard> standards, 
            Member member) throws Exception {
        
        // 1. Bean 객체 -> JSON 출력 변환기 생성
        BeanOutputConverter<PortfolioGuideResult> converter = 
            new BeanOutputConverter<>(PortfolioGuideResult.class);
        
        // DTO 구조 제공 -> JSON 출력 포맷 지정
        String format = converter.getFormat();

        // 2. 회원 정보 추출
        String jobGroup = extractJobGroup(request, member);
        String jobRole = extractJobRole(request, member);
        Integer careerYears = extractCareerYears(request, member);
        
        // 3. 표준 가이드라인 구성
        String standardsGuidelines = buildStandardsGuidelines(standards);
        
        // 4. 프롬프트 구성 - String.formatted() 방식
        String prompt = """
            # 포트폴리오 가이드 실시간 코칭

            사용자가 입력하는 내용을 실시간으로 분석하여 직군/직무 기준에 맞는 구체적인 피드백을 제공하세요.

            ## 사용자 정보
            - 직군: %s
            - 직무: %s
            - 경력: %d년

            ## 현재 작성 중인 항목
            - 입력 필드: %s
            - 현재 단계: %d단계
            - 사용자 입력 내용:
            "%s"

            ## 직무별 표준 가이드라인 및 평가 지침
            %s

            ## 출력 형식 (엄격한 JSON)

            반드시 아래 JSON 형식을 그대로 채워서 출력해라:
            %s

            **주의**: JSON 형식을 정확히 지켜주세요. 다른 텍스트는 포함하지 마세요.
            """.formatted(
                jobGroup,
                jobRole, 
                careerYears,
                request.getInputFieldType(),
                request.getCurrentStep(),
                request.getUserInput() != null ? request.getUserInput() : "",
                standardsGuidelines,
                format
            );

        // 5. LLM 호출
        String json = chatClient.prompt()
            .user(prompt)
            .call()
            .content();

        // 6. JSON -> DTO 변환
        PortfolioGuideResult result = converter.convert(json);

        return result != null ? result : createDefaultResult();
    }

    /**
     * 직무별 표준 가이드라인 조회
     */
    private List<PortfolioStandard> loadStandards(
            PortfolioGuideRequest request, Member member) {
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
        List<PortfolioStandard> standards = 
            portfolioStandardDao.selectStandardsByJobInfo(jobGroup, jobRole);
        
        // 없으면 전체 가이드라인 사용
        if (standards == null || standards.isEmpty()) {
            standards = portfolioStandardDao.selectAllStandards();
            log.info("직무별 가이드라인 없음 - 기본 가이드 사용 (직군: {}, 직무: {})", 
                jobGroup, jobRole);
        } else {
            log.info("직무별 가이드라인 조회 성공 (직군: {}, 직무: {}, 개수: {})", 
                jobGroup, jobRole, standards.size());
        }
        
        return standards;
    }

    /**
     * 표준 가이드라인 텍스트 구성
     */
    private String buildStandardsGuidelines(List<PortfolioStandard> standards) {
        if (standards == null || standards.isEmpty()) {
            return """
                ## 일반 평가 지침
                
                입력된 내용을 다음 기준으로 평가하세요:
                - 전체 적절성 점수 (1-10점)
                - 구체적인 개선 제안 3가지
                - 해당 직무에 적합한 예시 2-3개
                - 다음 단계 작성 가이드
                """;
        }

        StringBuilder sb = new StringBuilder();
        for (PortfolioStandard standard : standards) {
            sb.append(String.format("### %s\n", standard.getStandardName()));
            sb.append(String.format("%s\n\n", standard.getStandardDescription()));
            
            if (standard.getPromptTemplate() != null && 
                !standard.getPromptTemplate().isEmpty()) {
                sb.append(standard.getPromptTemplate());
                sb.append("\n\n");
            }
        }
        return sb.toString();
    }

    // =============== 헬퍼 메서드 ===============

    private String extractJobGroup(PortfolioGuideRequest request, Member member) {
        if (request.getJobGroup() != null) return request.getJobGroup();
        if (member != null && member.getJobGroup() != null) 
            return member.getJobGroup();
        return "개발자";
    }

    private String extractJobRole(PortfolioGuideRequest request, Member member) {
        if (request.getJobRole() != null) return request.getJobRole();
        if (member != null && member.getJobRole() != null) 
            return member.getJobRole();
        return "일반";
    }

    private Integer extractCareerYears(PortfolioGuideRequest request, Member member) {
        if (request.getCareerYears() != null) return request.getCareerYears();
        if (member != null && member.getCareerYears() != null) 
            return member.getCareerYears();
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
