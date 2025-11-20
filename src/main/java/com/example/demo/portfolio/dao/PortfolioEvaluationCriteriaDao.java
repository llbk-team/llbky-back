package com.example.demo.portfolio.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.portfolio.entity.PortfolioEvaluationCriteria;

@Mapper
public interface PortfolioEvaluationCriteriaDao {

  

  /**
     * 평가 기준 생성
     * @param criteria 평가 기준 정보
     * @return 생성된 행 수
     */
    public int insertCriteria(PortfolioEvaluationCriteria criteria);
    
    /**
     * 평가 기준 ID로 조회
     * @param criteriaId 평가 기준 ID
     * @return 평가 기준 정보
     */
   public PortfolioEvaluationCriteria selectCriteriaById(@Param("criteriaId") int criteriaId);
    
    /**
     * 직군/직무/경력별 평가 기준 조회
     * @param jobGroup 직군
     * @param jobRole 직무 (optional)
     * @param careerLevel 경력 수준
     * @return 평가 기준 목록
     */
    public List<PortfolioEvaluationCriteria> selectCriteriaByJobInfo(
                      @Param("jobGroup") String jobGroup,
                      @Param("jobRole") String jobRole,
                      @Param("careerLevel") String careerLevel
                  );
    
    /**
     * 활성화된 모든 평가 기준 조회
     * @return 활성화된 평가 기준 목록
     */
    public List<PortfolioEvaluationCriteria> selectActiveCriteria();
    
    /**
     * 평가 기준 수정
     * @param criteria 수정할 평가 기준 정보
     * @return 수정된 행 수
     */
   public int updateCriteria(PortfolioEvaluationCriteria criteria);
    
    /**
     * 평가 기준 활성/비활성 토글
     * @param criteriaId 평가 기준 ID
     * @param isActive 활성 상태
     * @return 수정된 행 수
     */
   public int toggleCriteriaStatus(@Param("criteriaId") int criteriaId, @Param("isActive") boolean isActive);
    
    /**
     * 평가 기준 삭제
     * @param criteriaId 평가 기준 ID
     * @return 삭제된 행 수
     */
  public  int deleteCriteria(@Param("criteriaId") int criteriaId);
    
    /**
     * 직군별 평가 기준 개수 조회
     * @param jobGroup 직군
     * @return 평가 기준 개수
     */
  public int countCriteriaByJobGroup(@Param("jobGroup") String jobGroup);
    
    /**
     * 모든 직군 목록 조회
     * @return 직군 목록
     */
  public List<String> selectDistinctJobGroups();
}
