package com.example.demo.member.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.dto.request.MemberRegisterRequest;
import com.example.demo.member.dto.request.MemberUpdateRequest;
import com.example.demo.member.entity.Member;
import com.example.demo.newstrend.dao.JobInsightDao;
import com.example.demo.newstrend.dao.NewsSummaryDao;
import com.example.demo.newstrend.dao.TrendInsightDao;

import lombok.RequiredArgsConstructor;

@Service
public class MemberService {
  @Autowired
  private MemberDao memberDao;
  @Autowired
  private JobInsightDao jobInsightDao;
  @Autowired 
  private NewsSummaryDao newsSummaryDao;
  @Autowired
  private TrendInsightDao trendInsightDao;

  /**
   * 회원가입
   */
  @Transactional
  public int register(MemberRegisterRequest request) {

    // 1) 로그인 ID 중복 체크
    Member exist = memberDao.findByLoginId(request.getLoginId());
    if (exist != null) {
      throw new RuntimeException("이미 사용 중인 아이디입니다.");
    }

    // 2) 엔티티 생성 및 값 매핑
    Member member = new Member();
    member.setMember_name(request.getName());
    member.setLoginId(request.getLoginId());
    member.setMember_password(request.getPassword()); // ★ 현재는 평문. 추후 BCrypt 적용 예정
    member.setMember_email(request.getEmail());
    member.setJobGroup(request.getJobGroup());
    member.setJobRole(request.getJobRole());
    member.setCareerYears(request.getCareerYears());

    // 3) DB 저장
    return memberDao.insert(member);
  }

  /**
   * 로그인 용 검색
   */
  public Member findByLoginId(String loginId) {
    return memberDao.findByLoginId(loginId);
  }

  /**
   * 회원 정보 수정
   */
  @Transactional
  public int updateMember(Integer memberId, MemberUpdateRequest request) {

    // 1) 기존 회원 찾기
    Member member = memberDao.findById(memberId);
    if (member == null) {
      throw new RuntimeException("회원을 찾을 수 없습니다.");
    }

    boolean jobChanged = false;

    // 직군 변경 감지
    if (request.getJobGroup() != null && !request.getJobGroup().equals(member.getJobGroup())) {
      member.setJobGroup(request.getJobGroup());
      jobChanged = true;
    }

    // 직무 변경 감지
    if (request.getJobRole() != null && !request.getJobRole().equals(member.getJobRole())) {
      member.setJobRole(request.getJobRole());
      jobChanged = true;
    }

    if (request.getEmail() != null)
      member.setMember_email(request.getEmail());

    if (request.getCareerYears() != null)
      member.setCareerYears(request.getCareerYears());

    if(jobChanged){
      jobInsightDao.deleteJobInsightByMember(memberId);
      trendInsightDao.deleteTrendInsightByMember(memberId);
      newsSummaryDao.deleteAllNews(memberId);
    }

    // 업데이트 실행
    return memberDao.update(member);
  }

}
