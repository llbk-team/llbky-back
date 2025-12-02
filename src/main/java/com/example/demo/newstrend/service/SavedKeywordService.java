package com.example.demo.newstrend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.newstrend.dao.SavedKeywordDao;
import com.example.demo.newstrend.dto.request.SavedKeywordRequest;
import com.example.demo.newstrend.entity.SavedKeyword;

@Service
public class SavedKeywordService {
  @Autowired
  private SavedKeywordDao savedKeywordDao;

  // 키워드 저장
  public SavedKeyword saveKeyword(SavedKeywordRequest request) {
    // 중복 체크
    SavedKeyword exists = savedKeywordDao.selectSavedKeyword(request.getMemberId(), request.getKeyword());
    if (exists != null) {
      return exists;
    }

    // 저장
    savedKeywordDao.insertSavedKeyword(request.getMemberId(), request.getKeyword(), request.getSourceLabel());

    return savedKeywordDao.selectSavedKeyword(request.getMemberId(), request.getKeyword());
  }

  // 저장한 키워드 전체 조회
  public List<SavedKeyword> getAllSavedKeyword(int memberId){
    return savedKeywordDao.selectSavedKeywordListByMemberId(memberId);
  }

  // 삭제
  public int removeSavedKeyword(int savedKeywordId){
    return savedKeywordDao.deleteSavedKeyword(savedKeywordId);
  }
}
