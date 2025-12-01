package com.example.demo.newstrend.service;

import java.util.*;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.dto.Member;
import com.example.demo.newstrend.dto.request.NewsAnalysisRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * 직군별 특화 뉴스 수집 서비스
 * - 10개 직군별로 다른 키워드 세트 적용
 * - 각 직군의 특성에 맞는 필터링 로직
 */
@Service
@Slf4j
public class NewsCollectorService {

    private WebClient webClient;
    
    @Autowired
    private MemberDao memberDao;

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    // ✅ 직군별 키워드 매핑
    private static final Map<String, List<String>> JOB_GROUP_KEYWORDS = Map.of(
        "개발", Arrays.asList(
            "개발자 채용", "백엔드 채용", "프론트엔드 채용", "풀스택 개발자",
            "소프트웨어 엔지니어", "프로그래머", "코딩", "Java", "Python", "React", "Spring"
        ),
        
        "AI/데이터", Arrays.asList(
            "데이터 사이언티스트", "데이터 엔지니어", "AI 개발자", "머신러닝 엔지니어",
            "빅데이터", "데이터 분석가", "인공지능", "딥러닝", "ML"
        ),
        
        "디자인", Arrays.asList(
            "UI 디자이너", "UX 디자이너", "웹디자인", "그래픽 디자이너", "프로덕트 디자이너",
            "디자인 채용", "포토샵", "피그마", "일러스트", "브랜딩"
        ),
        
        "기획", Arrays.asList(
            "기획자 채용", "서비스 기획", "상품 기획", "사업 기획", "전략 기획",
            "기획 업무", "기획 직무", "비즈니스 분석"
        ),
        
        "PM", Arrays.asList(
            "프로덕트 매니저", "프로젝트 매니저", "PM 채용", "PO", "프로덕트 오너",
            "애자일", "스크럼", "프로젝트 관리"
        ),
        
        "마케팅", Arrays.asList(
            "마케팅 매니저", "디지털 마케팅", "퍼포먼스 마케팅", "콘텐츠 마케팅",
            "브랜드 마케팅", "마케팅 기획", "광고", "SNS 마케팅", "SEO"
        ),
        
        "영업", Arrays.asList(
            "영업 대표", "세일즈", "비즈니스 개발", "B2B 영업", "고객 관리",
            "영업 기획", "계정 관리", "Sales"
        ),
        
        "장성", Arrays.asList(  // 경영으로 추정
            "경영", "경영관리", "경영기획", "전략", "경영지원", "임원", "관리자"
        ),
        
        "교육", Arrays.asList(
            "교육 기획", "강사", "교육 콘텐츠", "이러닝", "교육 프로그램", "연수", "교육생"
        ),
        
        "기타", Arrays.asList(
            "인사", "총무", "재무", "회계", "법무", "운영", "고객서비스", "품질관리"
        )
    );

     private static final Map<String, Set<String>> JOB_GROUP_FILTERS = Map.of(
        "개발", Set.of("개발", "프로그래밍", "코딩", "시스템", "소프트웨어", "앱", "웹", "API"),
        "AI/데이터", Set.of("데이터", "분석", "AI", "머신러닝", "딥러닝", "빅데이터", "알고리즘"),
        "디자인", Set.of("디자인", "UI", "UX", "그래픽", "브랜딩", "시각", "창작"),
        "기획", Set.of("기획", "전략", "분석", "리서치", "컨셉"),
        "PM", Set.of("관리", "매니저", "리드", "PM", "프로젝트", "제품"),
        "마케팅", Set.of("마케팅", "광고", "프로모션", "브랜드", "고객", "캠페인"),
        "영업", Set.of("영업", "세일즈", "판매", "고객", "계약", "B2B", "B2C"),
        "장성", Set.of("경영", "관리", "전략", "임원", "리더십"),
        "교육", Set.of("교육", "강의", "연수", "학습", "강사"),
        "기타", Set.of("인사", "총무", "재무", "회계", "법무", "운영", "지원")
    );

    // 공통 제외 키워드
    private static final Set<String> COMMON_EXCLUDE_KEYWORDS = Set.of(
        "운전", "배달", "서빙", "매장", "판매원", "아르바이트", "알바",
        "카페", "식당", "마트", "편의점", "주유소", "택시", "버스",
        "건설", "제조", "생산", "공장", "청소", "경비"
    );

    public NewsCollectorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl("https://openapi.naver.com/v1/search")
            .build();
    }

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

    /**
     * ✅ 사용자 직군에 맞는 키워드 생성
     */
    private List<String> generateJobGroupKeywords(List<String> inputKeywords, Integer memberId) {
        List<String> keywords = new ArrayList<>();
        
        // 1. 입력된 키워드 추가
        if (inputKeywords != null && !inputKeywords.isEmpty()) {
            keywords.addAll(inputKeywords);
        }
        
        // 2. 사용자 직군별 키워드 추가
        if (memberId != null) {
            Member member = memberDao.findById(memberId);
            if (member != null && member.getJobGroup() != null) {
                String jobGroup = member.getJobGroup();
                
                // 직군별 특화 키워드 추가
                List<String> jobKeywords = JOB_GROUP_KEYWORDS.get(jobGroup);
                if (jobKeywords != null) {
                    keywords.addAll(jobKeywords);
                    log.info("직군 '{}' 키워드 {}개 추가", jobGroup, jobKeywords.size());
                } else {
                    log.warn("매핑되지 않은 직군: {}", jobGroup);
                    // 기본 키워드 추가
                    keywords.addAll(Arrays.asList(jobGroup + " 채용", jobGroup + " 모집"));
                }
            }
        }
        
        // 3. 키워드가 없으면 개발 직군 기본값 사용
        if (keywords.isEmpty()) {
            keywords.addAll(JOB_GROUP_KEYWORDS.get("개발"));
        }
        
        return keywords.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 키워드 기반 뉴스 수집 (순수 수집만 담당)
     * 
     * @param keywords 검색 키워드 목록
     * @param memberId 회원 ID (null이면 1로 설정)
     * @return 수집된 뉴스 리스트
     */
    public List<NewsAnalysisRequest> collectNews(List<String> keywords, Integer memberId) throws Exception {
        log.info("뉴스 수집 시작 - 키워드: {}, memberId: {}", keywords, memberId);
        
        List<String> jobGroupKeywords = generateJobGroupKeywords(keywords, memberId);
        int filteredCount = 0;
        int errorCount = 0;
        
        // 사용자 직군 파악
        String userJobGroup = "개발"; // 기본값
        if (memberId != null) {
            Member member = memberDao.findById(memberId);
            if (member != null && member.getJobGroup() != null) {
                userJobGroup = member.getJobGroup();
            }
        }
        
        Map<String, NewsAnalysisRequest> uniqueNewsMap = new HashMap<>();
        
        // 각 키워드별로 뉴스 수집
        for (String keyword : jobGroupKeywords) {
            try {
                String naverResponse = getNaverNews(keyword);
                List<NewsAnalysisRequest> naverNews = parseNaverNews(naverResponse);
                
                for (NewsAnalysisRequest news : naverNews) {
                    if (!uniqueNewsMap.containsKey(news.getSourceUrl())) {
                        
                        // ✅ 직군별 특화 필터링
                        if (isJobGroupRelated(news.getTitle(), news.getContent(), userJobGroup)) {
                            news.setMemberId(memberId != null ? memberId : 1);
                            uniqueNewsMap.put(news.getSourceUrl(), news);
                            log.debug("'{}' 직군 관련 뉴스 추가: {}", userJobGroup, news.getTitle());
                        } else {
                            filteredCount++;
                            log.debug("'{}' 직군과 무관한 뉴스 필터링: {}", userJobGroup, news.getTitle());
                        }
                    }
                }
                
                Thread.sleep(500);
                
            } catch (Exception e) {
                log.warn("키워드 '{}' 수집 실패", keyword, e);
                errorCount++;
            }
        }
        
        List<NewsAnalysisRequest> collectedNews = new ArrayList<>(uniqueNewsMap.values());
        
        log.info("'{}' 직군 뉴스 수집 완료 - 수집: {}건, 필터링 제외: {}건, 최종: {}건, 오류: {}건",
            userJobGroup, uniqueNewsMap.size() + filteredCount, filteredCount, collectedNews.size(), errorCount);
        
        return collectedNews;
    }

    /**
     * 키워드 리스트로 네이버 뉴스 검색 (단순 검색, 저장 안함)
     * 
     * @param keywords 검색할 키워드 리스트
     * @param limitPerKeyword 각 키워드당 가져올 기사 수
     * @return 기사 제목, URL, 설명이 담긴 리스트
     */
    public List<Map<String, String>> searchNewsByKeywords(List<String> keywords, int limitPerKeyword) {
        
        log.info("키워드 기반 뉴스 검색 시작 - 키워드: {}, limit: {}", keywords, limitPerKeyword);
        
        List<Map<String, String>> results = new ArrayList<>();
        Set<String> addedUrls = new HashSet<>();  // 중복 URL 방지
        
        for (String keyword : keywords) {
            try {
                // 네이버 API 호출
                String naverResponse = getNaverNews(keyword);
                
                // JSON 파싱
                JSONObject json = new JSONObject(naverResponse);
                JSONArray items = json.getJSONArray("items");
                
                int added = 0;
                for (int i = 0; i < items.length() && added < limitPerKeyword; i++) {
                    JSONObject item = items.getJSONObject(i);
                    
                    String url = item.optString("link", "");
                    
                    // 중복 URL 체크
                    if (!addedUrls.contains(url)) {
                        Map<String, String> article = new HashMap<>();
                        article.put("title", cleanHtml(item.optString("title", "")));
                        article.put("description", cleanHtml(item.optString("description", "")));
                        article.put("url", url);
                        article.put("keyword", keyword);  // 어떤 키워드로 검색됐는지
                        
                        results.add(article);
                        addedUrls.add(url);
                        added++;
                    }
                }
                
                // API 호출 제한
                Thread.sleep(500);
                
            } catch (Exception e) {
                log.warn("키워드 '{}' 검색 실패", keyword, e);
            }
        }
        
        log.info("키워드 기반 뉴스 검색 완료 - {}건 반환", results.size());
        return results;
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
                if (news.getSourceUrl() != null && !news.getSourceUrl().isEmpty()) {
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

    /**
     * ✅ 직군별 특화 관련성 판단
     */
    private boolean isJobGroupRelated(String title, String content, String jobGroup) {
        if (title == null) return false;
        
        String text = (title + " " + (content != null ? content : "")).toLowerCase();
        
        // 1. 공통 제외 키워드 체크
        for (String excludeKeyword : COMMON_EXCLUDE_KEYWORDS) {
            if (text.contains(excludeKeyword.toLowerCase())) {
                return false;
            }
        }
        
        // 2. 해당 직군 필터링 키워드 점수 계산
        Set<String> jobFilters = JOB_GROUP_FILTERS.getOrDefault(jobGroup, Set.of());
        int score = 0;
        
        for (String filter : jobFilters) {
            if (text.contains(filter.toLowerCase())) {
                // 제목에 있으면 가중치 2배
                if (title.toLowerCase().contains(filter.toLowerCase())) {
                    score += 2;
                } else {
                    score += 1;
                }
            }
        }
        
        // 3. 채용 관련 키워드 가산점
        if (text.matches(".*채용|구인|모집|입사|취업|면접.*")) {
            score += 2;
        }
        
        // 4. 직군별 임계점 설정 (개발직군은 더 까다롭게)
        int threshold = "개발".equals(jobGroup) ? 3 : 2;
        boolean isRelated = score >= threshold;
        
        log.debug("'{}' 직군 관련성 판단 - 제목: '{}', 점수: {}/{}, 결과: {}", 
            jobGroup, 
            title.length() > 40 ? title.substring(0, 40) + "..." : title, 
            score, threshold, isRelated);
            
        return isRelated;
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
