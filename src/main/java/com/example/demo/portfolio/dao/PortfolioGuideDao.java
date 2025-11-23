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
  
    public int insertGuide(PortfolioGuide guide);//포트폴리오 가이드 생성
    
    
    public PortfolioGuide selectGuideById(@Param("guideId") int guideId);// 가이드 ID로 조회
    
    public List<PortfolioGuide> selectGuidesByMemberId(@Param("memberId") int memberId);//회원별 가이드 목록 조회
    
    public List<PortfolioGuide> selectCompletedGuidesByMemberId(@Param("memberId") int memberId);//완료된 가이드 조회

    public List<PortfolioGuide> selectInProgressGuidesByMemberId(@Param("memberId") int memberId);//진행 중인 가이드 조회
   
    public int updateGuide(PortfolioGuide guide);//가이드 업데이트
    
    //가이드 진행 상태 업데이트
    public int updateGuideProgress(
        @Param("guideId") int guideId,
        @Param("completionPercentage") int completionPercentage,
        @Param("currentStep") int currentStep,
        @Param("isCompleted") Boolean isCompleted
    );
    

    public int updateGuideContent(@Param("guideId") int guideId, @Param("content") String content);//가이드 콘텐츠 업데이트
    
    public int updateGuideFeedback(@Param("guideId") int guideId, @Param("feedback") String feedback);
    

    public int completeGuide(@Param("guideId") int guideId);//가이드 완료 처리
    
  
    public int deleteGuide(@Param("guideId") int guideId);//가이드 삭제
    
  
    public int deleteGuidesByMemberId(@Param("memberId") int memberId);//회원의 모든 가이드 삭제

    /* 쓰는 날이 올까?
    public int countGuidesByMemberId(@Param("memberId") int memberId);//가이드 개수 조회
    
    public int countCompletedGuidesByMemberId(@Param("memberId") int memberId);//완료된 가이드 개수 조회
  
    public double getAverageCompletionByMemberId(@Param("memberId") int memberId);//평균 완료율 조회
    
    public List<PortfolioGuide> selectRecentGuides(@Param("limit") int limit);//최근 생성된 가이드 조회 (관리자용)

    //AI 코칭 피드백 저장
    public int updateGuideFeedback(@Param("guideId") Integer guideId,
        @Param("guideFeedback") String guideFeedbackJson);
*/
}
