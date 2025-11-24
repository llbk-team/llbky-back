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
     * 완료된 가이드 조회
     */
    public List<PortfolioGuide> selectCompletedGuidesByMemberId(@Param("memberId") int memberId);

    /**
     * 진행 중인 가이드 조회
     */
    public List<PortfolioGuide> selectInProgressGuidesByMemberId(@Param("memberId") int memberId);
    
    /**
     * 특정 평가 기준으로 가이드 조회
     */
    public List<PortfolioGuide> selectGuidesByStandardId(@Param("standardId") int standardId);
   
    /**
     * 가이드 업데이트 (전체 필드)
     */
    public int updateGuide(PortfolioGuide guide);
    
    /**
     * 가이드 진행률만 업데이트
     */
    public int updateGuideProgressOnly(java.util.Map<String, Object> params);
    
    /**
     * 🔥 가이드 콘텐츠 및 진행상황 업데이트 (Map 파라미터)
     * @param params - guideId, guideContent, completionPercentage, currentStep 포함
     */
    public int updateGuideContent(java.util.Map<String, Object> params);
    
    /**
     * 🔥 전체 가이드 진행상황 업데이트 (Map 파라미터)
     * @param params - guideId, guideContent, completionPercentage, currentStep, isCompleted 포함
     */
    public int updateGuideProgress(java.util.Map<String, Object> params);
    
    /**
     * AI 가이드 피드백 업데이트
     */
    public int updateGuideFeedback(@Param("guideId") int guideId, @Param("feedback") String feedback);
    
    /**
     * 가이드 완료 처리
     */
    public int completeGuide(@Param("guideId") int guideId);
    
    /**
     * 가이드 삭제
     */
    public int deleteGuide(@Param("guideId") int guideId);
    
    /**
     * 회원의 모든 가이드 삭제
     */
    public int deleteGuidesByMemberId(@Param("memberId") int memberId);

    /**
     * 가이드 개수 조회
     */
    public int countGuidesByMemberId(@Param("memberId") int memberId);
    
    /**
     * 완료된 가이드 개수 조회
     */
    public int countCompletedGuidesByMemberId(@Param("memberId") int memberId);
  
    /**
     * 평균 완료율 조회
     */
    public double getAverageCompletionByMemberId(@Param("memberId") int memberId);
    
    /**
     * 최근 생성된 가이드 조회 (관리자용)
     */
    public List<PortfolioGuide> selectRecentGuides(@Param("limit") int limit);
    
    /**
     * 특정 평가 기준별 가이드 통계
     */
    public int countGuidesByStandardId(@Param("standardId") int standardId);
    
    /**
     * 특정 평가 기준별 완료된 가이드 수
     */
    public int countCompletedGuidesByStandardId(@Param("standardId") int standardId);
}
