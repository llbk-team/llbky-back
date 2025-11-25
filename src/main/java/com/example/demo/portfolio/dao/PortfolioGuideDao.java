package com.example.demo.portfolio.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.portfolio.entity.PortfolioGuide;

/*


-- 2. 사용자별 포트폴리오 가이드 테이블
CREATE TABLE portfolio_guide (
    guide_id SERIAL PRIMARY KEY,
    member_id INT NOT NULL REFERENCES member(member_id) ON DELETE CASCADE,
    standard_id INT NOT NULL REFERENCES portfolio_standard(standard_id) ON DELETE CASCADE,  -- ✅ 올바른 FK 설정
    title VARCHAR(200) NOT NULL DEFAULT '새 포트폴리오 가이드',
    
    -- 가이드 작성 내용 (JSONB)
    guide_content JSONB,                         -- 단계별 가이드 작성 내용
    
    -- 진행 상태
    completion_percentage INTEGER DEFAULT 0 CHECK (completion_percentage >= 0 AND completion_percentage <= 100),
    is_completed BOOLEAN DEFAULT FALSE,
    current_step INTEGER DEFAULT 1,             -- 현재 작성 중인 단계
    total_steps INTEGER DEFAULT 5,              -- 전체 단계 수
    
    -- AI 코칭 결과
    guide_feedback JSONB,                        -- AI 가이드 코칭 결과
    
    -- 시간 정보
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

*/


@Mapper
public interface PortfolioGuideDao {
  
    /**
     * 포트폴리오 가이드 생성
     */
    public int insertGuide(PortfolioGuide guide);
    
    /**
     * 가이드 ID로 조회
     */
    public PortfolioGuide selectGuideById(@Param("guideId") int guideId);
    
    /**
     * 회원별 가이드 목록 조회
     */
    public List<PortfolioGuide> selectGuidesByMemberId(@Param("memberId") int memberId);
    
   
    /**
     * 🔥 전체 가이드 진행상황 업데이트 (Map 파라미터)
     * @param params - guideId, guideContent, completionPercentage, currentStep, isCompleted 포함
     */
    public int updateGuideProgress(java.util.Map<String, Object> params);
    
    /**
     * 🔥 AI 가이드 피드백 업데이트 (개별 필드)
     */
    public int updateGuideFeedback(
        @Param("guideId") Integer guideId,
        @Param("appropriatenessScore") Integer appropriatenessScore,
        @Param("progressPercentage") Integer progressPercentage,
        @Param("coachingMessage") String coachingMessage,
        @Param("suggestions") String suggestions,
        @Param("examples") String examples,
        @Param("nextStepGuide") String nextStepGuide
    );

     /**
     * 🔥 가이드 콘텐츠 및 진행상황 업데이트 (Map 파라미터)
     * @param params - guideId, guideContent, completionPercentage, currentStep 포함
     */
    public int updateGuideContent(java.util.Map<String, Object> params);

}
