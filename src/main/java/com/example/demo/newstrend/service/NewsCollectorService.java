package com.example.demo.newstrend.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.ai.newstrend.NewsFilteringAgent;
import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.dto.Member;
import com.example.demo.newstrend.dao.NewsSummaryDao;
import com.example.demo.newstrend.dto.request.NewsAnalysisRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * 뉴스 자동 수집 및 분석 서비스
 * - 매일 정해진 시간에 자동으로 뉴스 수집
 * - 네이버 뉴스 API와 NewsAPI.org에서 뉴스 수집
 * - AI 분석 후 데이터베이스 저장
 */
@Service
@Slf4j
public class NewsCollectorService {

    private WebClient webClient;

    @Autowired
    private NewsAIService newsAIService;

    @Autowired
    private NewsSummaryDao newsSummaryDao;

    @Autowired
    private MemberDao memberDao;

    @Autowired
    private NewsFilteringAgent filteringAgent;

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    public NewsCollectorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://openapi.naver.com/v1/search")
                .build();
    }
   
    // 기본 검색 키워드 목록
    private  static List<String> DEFAULT_KEYWORDS = Arrays.asList(
           
            "AI 개발자",
            "백엔드 채용",
            "프론트엔드 개발자",
            "데이터 엔지니어",
            "클라우드 엔지니어",
            "풀스택 개발자",
            "DevOps",
            "취업 트렌드");

    /**
     * 네이버 뉴스 검색
     * 
     * @param keyword 검색 키워드
     * @return JSON 형식의 뉴스 검색 결과
     */
    public String getNaverNews(String keyword) {
        log.info("네이버 뉴스 검색 - 키워드: {}", keyword);

        String result = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/news.json")
                        .queryParam("query", keyword)
                        .queryParam("display", 10)
                        .queryParam("sort", "date")
                        .build())
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.debug("네이버 뉴스 검색 완료 - 응답 길이: {} bytes",
                result != null ? result.length() : 0);

        return result;
    }
    //사용자 맞춤 키워드 
    private List<String> basicKeywords(List<String> inputKeywords, Integer memberId){
        List<String> keywords= new ArrayList<>();
        //입력된 키워드 있으면 추가
        if(inputKeywords!=null && !inputKeywords.isEmpty()){
            keywords.addAll(inputKeywords);
        }
        //사용자 직무/ 직군 기반 키워드 추가
        if(memberId!=null){
            Member member = memberDao.findById(memberId);

            if(member.getJobGroup()!=null){
                keywords.add(member.getJobGroup()+" 채용");
            }
            if(member.getJobRole()!=null){
                keywords.add(member.getJobRole()+" 취업");
            }
        }
        //유저 직군 직무로 부터 만든 키워드 없으면 기본 키워드사용
        if(keywords.isEmpty()){
            keywords.addAll(DEFAULT_KEYWORDS);
        }

        return keywords.stream().distinct().toList(); //중복 제거

    }






    /**
     * 키워드 기반 뉴스 수집 및 분석
     * 
     * @param keywords 검색 키워드 목록
     * @param memberId 회원 ID (null이면 1로 설정)
     * @return 분석된 뉴스 개수
     */
    @Transactional
    public int collectAndAnalyzeNews(List<String> keywords, Integer memberId) throws Exception {
        log.info("뉴스 수집 시작 - 키워드: {}, memberId: {}", keywords, memberId);

        List<String> firstKeywords = basicKeywords(keywords, memberId);

        int totalAnalyzed = 0;
        int duplicateCount = 0;
        int errorCount = 0;
        int filteredCount = 0; 

        // 중복 제거를 위한 Map (sourceUrl을 키로 사용)
        Map<String, NewsAnalysisRequest> uniqueNewsMap = new HashMap<>();

        // 1. 각 키워드별로 뉴스 수집
        for (String keyword : firstKeywords) {
            log.debug("키워드 '{}' 검색 시작", keyword);
            try {
                // 1-1. 네이버 뉴스 수집
                String naverResponse = getNaverNews(keyword);
                List<NewsAnalysisRequest> naverNews = parseNaverNews(naverResponse);
                log.debug("네이버 뉴스 {}건 수집: {}", naverNews.size(), keyword);

                // Map에 추가 (중복 제거)
                for (NewsAnalysisRequest news : naverNews) {
                    if (!uniqueNewsMap.containsKey(news.getSourceUrl())) {
                        
                        boolean isRelevant = filteringAgent.isRelevant(news.getTitle(), news.getContent(), firstKeywords);

                        if(isRelevant){
                            uniqueNewsMap.put(news.getSourceUrl(), news);
                            log.debug("관련성 있는 뉴스 추가: {}", news.getTitle());
                        }else{
                            filteredCount++;
                            log.debug("관련성 없는 뉴스 필터링: {}", news.getTitle());
                        }
                    }
                }
                // API 호출 제한을 위한 대기
                Thread.sleep(500);

            } catch (Exception e) {
                log.warn("네이버 뉴스 수집 실패: {}", keyword, e);
                errorCount++;
            }
        }

    

        // 2. 수집된 뉴스 분석 및 저장
        for (NewsAnalysisRequest newsRequest : uniqueNewsMap.values()) {
            try {
                // 2-1. URL 중복 체크
                if (newsSummaryDao.selectNewsSummaryBySourceUrl(newsRequest.getSourceUrl()) != null) {
                    log.debug("이미 저장된 뉴스: {}", newsRequest.getSourceUrl());
                    duplicateCount++;
                    continue;
                }

                // 2-2. memberId 설정
                newsRequest.setMemberId(memberId != null ? memberId : 1);

                // 2-3. AI 분석 및 저장
                newsAIService.analyzeAndSaveNews(newsRequest);
                totalAnalyzed++;

                log.debug("뉴스 분석 완료: {}", newsRequest.getTitle());

                // API 호출 제한을 위한 대기
                Thread.sleep(500);

            } catch (Exception e) {
                log.error("뉴스 분석 실패: {}", newsRequest.getTitle(), e);
                errorCount++;
            }
        }

      
        log.info("뉴스 필터링 완료 - 수집: {}건, 필터링 제외: {}건, 최종: {}건", 
            uniqueNewsMap.size() + filteredCount, filteredCount, uniqueNewsMap.size());

        return totalAnalyzed;
    }

    /**
     * 네이버 뉴스 API 응답 파싱
     * 
     * @param jsonResponse 네이버 API JSON 응답
     * @return 뉴스 요청 목록
     */
    private List<NewsAnalysisRequest> parseNaverNews(String jsonResponse) {
        List<NewsAnalysisRequest> newsList = new ArrayList<>();

        try {
            JSONObject json = new JSONObject(jsonResponse);
            JSONArray items = json.getJSONArray("items");

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);

                NewsAnalysisRequest news = new NewsAnalysisRequest();
                news.setTitle(cleanHtml(item.optString("title", "")));
                news.setContent(cleanHtml(item.optString("description", "")));
                news.setSourceUrl(item.optString("link", ""));
                news.setSourceName("네이버뉴스");

                // 유효한 뉴스만 추가
                 if (isJobRelated(news.getTitle()) && news.getSourceUrl() != null && !news.getSourceUrl().isEmpty()) {
                    newsList.add(news);
                }
            }

        } catch (Exception e) {
            log.error("네이버 뉴스 파싱 오류", e);
        }

        return newsList;
    }

    // /**
    // * NewsAPI.org API 응답 파싱
    // * @param jsonResponse NewsAPI JSON 응답
    // * @return 뉴스 요청 목록
    // */
    // private List<NewsAnalysisRequest> parseNewsApi(String jsonResponse) {
    // List<NewsAnalysisRequest> newsList = new ArrayList<>();

    // try {
    // JSONObject json = new JSONObject(jsonResponse);

    // // status 체크
    // String status = json.optString("status", "");
    // if (!"ok".equals(status)) {
    // log.warn("NewsAPI 응답 상태 오류: {}", status);
    // return newsList;
    // }

    // JSONArray articles = json.optJSONArray("articles");
    // if (articles == null) {
    // return newsList;
    // }

    // for (int i = 0; i < articles.length(); i++) {
    // JSONObject article = articles.getJSONObject(i);

    // NewsAnalysisRequest news = new NewsAnalysisRequest();
    // news.setTitle(article.optString("title", ""));
    // news.setContent(article.optString("description", ""));
    // news.setSourceUrl(article.optString("url", ""));

    // // source 객체에서 이름 추출
    // JSONObject source = article.optJSONObject("source");
    // if (source != null) {
    // news.setSourceName(source.optString("name", "NewsAPI"));
    // } else {
    // news.setSourceName("NewsAPI");
    // }

    // // 유효한 뉴스만 추가
    // if (news.getSourceUrl() != null && !news.getSourceUrl().isEmpty()) {
    // newsList.add(news);
    // }
    // }

    // } catch (Exception e) {
    // log.error("NewsAPI 파싱 오류", e);
    // }

    // return newsList;
    // }

    //취업 관련 기본 필터링

    private boolean isJobRelated(String title) {
        if (title == null) return false;
        
        String[] jobKeywords = {
            "채용", "구인", "취업", "입사", "개발자", "엔지니어", 
            "프로그래머", "개발", "코딩", "IT", "기술직", "경력직", "신입"
        };
        
        String lowerTitle = title.toLowerCase();
        for (String keyword : jobKeywords) {
            if (lowerTitle.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * HTML 태그 제거 및 특수 문자 변환
     * 
     * @param text HTML이 포함된 텍스트
     * @return 정제된 텍스트
     */
    private String cleanHtml(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replaceAll("<.*?>", "") // HTML 태그 제거
                .replaceAll("&quot;", "\"") // &quot; → "
                .replaceAll("&amp;", "&") // &amp; → &
                .replaceAll("&lt;", "<") // &lt; → <
                .replaceAll("&gt;", ">") // &gt; → >
                .replaceAll("&nbsp;", " ") // &nbsp; → 공백
                .replaceAll("\\s+", " ") // 연속된 공백 → 하나의 공백
                .trim();
    }

}
