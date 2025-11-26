package com.example.demo.newstrend.service;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WebScrapingService {

  //사용하는 이유: 서버가 크롬 브라우저에서 온 정상 사용자구나라고 인식-> 
  // 정상HTML반환시켜준다.
  public String extractNewsContent(String url) throws IOException{
    Document doc = Jsoup.connect(url)
    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
    .timeout(10000)
    .get();

    if(url.contains("news.naver.com")){
      Element article=doc.selectFirst("#dic_area, #articeBody,article");
      if(article !=null){
        return article.text();
      }
    }

    Element article = doc.selectFirst("article,.article-content,.news-content");
    if(article!=null){
      return article.text();
    }

    log.warn("본문 추출 실패, description 사용:{}",url);
    return null;


  }

}
