package com.example.demo.portfolio.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.portfolio.entity.PortfolioGuide;

@Mapper
public interface PortfolioGuideDao {

  /**
     * 포트폴리오 가이드 생성
     * @param guide 가이드 정보
     * @return 생성된 행 수
     */
    int insertGuide(PortfolioGuide guide);
    
    /**
     * 가이드 ID로 조회
     * @param guideId 가이드 ID
     * @return 가이드 정보
     */
    PortfolioGuide selectGuideById(@Param("guideId") int guideId);
    
    /**
     * 회원별 가이드 목록 조회
     * @param memberId 회원 ID
     * @return 가이드 목록
     */
    List<PortfolioGuide> selectGuidesByMemberId(@Param("memberId") int memberId);
    
    /**
     * 완료된 가이드 조회
     * @param memberId 회원 ID
     * @return 완료된 가이드 목록
     */
    List<PortfolioGuide> selectCompletedGuidesByMemberId(@Param("memberId") int memberId);
    
    /**
     * 진행 중인 가이드 조회
     * @param memberId 회원 ID
     * @return 진행 중인 가이드 목록
     */
    List<PortfolioGuide> selectInProgressGuidesByMemberId(@Param("memberId") int memberId);
    
    /**
     * 가이드 업데이트
     * @param guide 수정할 가이드 정보
     * @return 수정된 행 수
     */
    int updateGuide(PortfolioGuide guide);
    
    /**
     * 가이드 진행률 업데이트
     * @param guideId 가이드 ID
     * @param completionPercentage 완료율
     * @param currentStep 현재 단계
     * @return 수정된 행 수
     */
    int updateGuideProgress(
        @Param("guideId") int guideId,
        @Param("completionPercentage") int completionPercentage,
        @Param("currentStep") int currentStep
    );
    
    /**
     * 가이드 콘텐츠 업데이트
     * @param guideId 가이드 ID
     * @param content 가이드 콘텐츠 (JSON 문자열)
     * @return 수정된 행 수
     */
    int updateGuideContent(@Param("guideId") int guideId, @Param("content") String content);
    
    /**
     * AI 가이드 피드백 업데이트
     * @param guideId 가이드 ID
     * @param feedback AI 피드백 (JSON 문자열)
     * @return 수정된 행 수
     */
    int updateGuideFeedback(@Param("guideId") int guideId, @Param("feedback") String feedback);
    
    /**
     * 가이드 완료 처리
     * @param guideId 가이드 ID
     * @return 수정된 행 수
     */
    int completeGuide(@Param("guideId") int guideId);
    
    /**
     * 가이드 삭제
     * @param guideId 가이드 ID
     * @return 삭제된 행 수
     */
    int deleteGuide(@Param("guideId") int guideId);
    
    /**
     * 회원의 모든 가이드 삭제
     * @param memberId 회원 ID
     * @return 삭제된 행 수
     */
    int deleteGuidesByMemberId(@Param("memberId") int memberId);
    
    /**
     * 가이드 개수 조회
     * @param memberId 회원 ID
     * @return 가이드 개수
     */
    int countGuidesByMemberId(@Param("memberId") int memberId);
    
    /**
     * 완료된 가이드 개수 조회
     * @param memberId 회원 ID
     * @return 완료된 가이드 개수
     */
    int countCompletedGuidesByMemberId(@Param("memberId") int memberId);
    
    /**
     * 평균 완료율 조회
     * @param memberId 회원 ID
     * @return 평균 완료율 (0.0 - 100.0)
     */
    double getAverageCompletionByMemberId(@Param("memberId") int memberId);
    
    /**
     * 최근 생성된 가이드 조회 (관리자용)
     * @param limit 조회 개수 제한
     * @return 최근 가이드 목록
     */
    List<PortfolioGuide> selectRecentGuides(@Param("limit") int limit);


}
