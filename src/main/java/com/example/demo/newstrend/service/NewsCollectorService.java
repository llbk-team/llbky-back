package com.example.demo.newstrend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.member.dao.MemberDao;
import com.example.demo.member.entity.Member;
import com.example.demo.newstrend.dao.NewsSummaryDao;
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

    @Autowired
    private NewsSummaryDao newsSummaryDao;

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    public NewsCollectorService(WebClient.Builder webClientBuilder, NewsSummaryService newsSummaryService) {
        this.webClient = webClientBuilder
                .baseUrl("https://openapi.naver.com/v1/search")
                .build();

    }

    public String getNaverNews(String keyword) {
        return getNaverNews(keyword, 10); // 기본 10개
    }

    public String getNaverNews(String keyword, int display) {
        log.info("네이버 뉴스 검색 - 키워드: {}, display: {}", keyword, display);

        String result = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/news.json")
                        .queryParam("query", keyword)
                        .queryParam("display", Math.min(display, 50))
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
     * 검색 키워드 기반 뉴스 수집 (순수 수집만 담당)
     * 
     * @param keywords 검색 키워드 목록
     * @param memberId 회원 ID (null이면 1로 설정)
     * @return 수집된 뉴스 리스트
     */
    public List<NewsAnalysisRequest> collectNews(List<String> keywords, Integer memberId, int limit) throws Exception {
        log.info("뉴스 수집 시작 - 키워드: {}, memberId: {}, limit: {}", keywords, memberId, limit);

        List<NewsAnalysisRequest> allNews = new ArrayList<>();
        Set<String> urls = new HashSet<>();
        int successCount = 0;
        int errorCount = 0;
        int sessionDuplicateCount = 0; // ✅ 이번 수집에서 중복
        int dbDuplicateCount = 0; // ✅ DB에 이미 있음

        Member member = memberDao.findById(memberId);
        if (member == null) {
            throw new RuntimeException("회원 정보를 찾을 수 없습니다.");
        }

        // ✅ 키워드당 가져올 개수 계산
        int perKeyword = Math.max(1, limit / keywords.size());

        for (String keyword : keywords) {
            // ✅ 이미 limit 도달하면 중단
            if (allNews.size() >= limit) {
                log.info("수집 limit 도달 - {}건 수집 완료", allNews.size());
                break;
            }

            try {
                String naverResponse = getNaverNews(keyword.trim(), perKeyword);
                List<NewsAnalysisRequest> news = parseNaverNews(naverResponse);

                for (NewsAnalysisRequest newsItem : news) {
                    // ✅ limit 체크
                    if (allNews.size() >= limit) {
                        break;
                    }

                    String url = newsItem.getSourceUrl();

                    if (urls.contains(url)) {
                        sessionDuplicateCount++;
                        continue;
                    }

                    if (newsSummaryDao.selectNewsSummaryBySourceUrl(url) != null) {
                        dbDuplicateCount++;
                        log.debug("DB 중복 제외: {}", newsItem.getTitle());
                        continue;
                    }

                    newsItem.setMemberId(memberId != null ? memberId : 1);
                    allNews.add(newsItem);
                    urls.add(url);
                    successCount++;
                }

                Thread.sleep(500);

            } catch (Exception e) {
                log.error("뉴스 수집 오류 - 키워드: {}", keyword, e);
                errorCount++;
            }
        }

        log.info("뉴스 수집 완료 - 성공: {}건, 오류: {}건", successCount, errorCount);

        return allNews;
    }

    /**
     * 키워드 리스트로 네이버 뉴스 검색 (단순 검색, 저장 안함)
     * 
     * @param keywords        검색할 키워드 리스트
     * @param limitPerKeyword 각 키워드당 가져올 기사 수
     * @return 기사 제목, URL, 설명이 담긴 리스트
     */
    public List<Map<String, String>> searchNewsByKeywords(List<String> keywords, int limitPerKeyword) {

        log.info("키워드 기반 뉴스 검색 시작 - 키워드: {}, limit: {}", keywords, limitPerKeyword);
        // 검색 시작 로그 출력 (디버깅 및 모니터링용)

        List<Map<String, String>> results = new ArrayList<>();
        // 최종적으로 반환할 뉴스 목록 저장 리스트

        Set<String> addedUrls = new HashSet<>();
        // 이미 추가한 뉴스 URL을 저장해서 중복 방지

        for (String keyword : keywords) {
            // 전달받은 키워드를 하나씩 반복하며 검색

            try {

                String naverResponse = getNaverNews(keyword);
                // 네이버 뉴스 API 호출하여 JSON 응답(String)을 받아옴

                JSONObject json = new JSONObject(naverResponse);
                // 받아온 JSON 문자열을 실제 JSON 객체로 변환

                JSONArray items = json.getJSONArray("items");
                // JSON에서 "items" 배열 꺼냄 → 뉴스 목록

                int added = 0;
                // 한 키워드당 limitPerKeyword 개수만큼만 가져가기 위해 카운트 변수 선언

                for (int i = 0; i < items.length() && added < limitPerKeyword; i++) {
                    // items 배열을 순회하지만 limit 개수만큼까지만 처리

                    JSONObject item = items.getJSONObject(i);
                    // 현재 뉴스 아이템(JSON) 가져오기

                    String url = item.optString("link", "");
                    // 뉴스 링크(URL) 추출, 없으면 빈 문자열

                    if (!addedUrls.contains(url)) {
                        // 이미 추가한 뉴스(URL)인지 체크

                        Map<String, String> article = new HashMap<>();
                        // 결과에 담을 뉴스 정보를 저장할 Map 생성

                        article.put("title", cleanHtml(item.optString("title", "")));
                        // 제목 추출 + HTML 태그 제거

                        article.put("description", cleanHtml(item.optString("description", "")));
                        // 본문(설명) 추출 + HTML 태그 제거

                        article.put("url", url);
                        // 뉴스 원문 URL 저장

                        article.put("keyword", keyword);
                        // 어떤 키워드로 검색된 뉴스인지 저장

                        results.add(article);
                        // 최종 결과 리스트에 추가

                        addedUrls.add(url);
                        // 중복 방지를 위해 URL 기록

                        added++;
                        // 현재 키워드에서 수집한 뉴스 개수 증가
                    }
                }

                Thread.sleep(500);
                // API 호출 과다 방지를 위한 딜레이(0.5초)

            } catch (Exception e) {
                log.warn("키워드 '{}' 검색 실패", keyword, e);
                // API 오류, JSON 오류 등 문제 발생 시 경고 로그 출력
            }
        }

        log.info("키워드 기반 뉴스 검색 완료 - {}건 반환", results.size());
        // 검색 종료 로그 출력

        return results;
        // 결과 반환
    }

    /**
     * 네이버 뉴스 API 응답 파싱
     * 
     * @param jsonResponse 네이버 API JSON 응답
     * @return 뉴스 요청 목록
     *         item.optString("title") NAVER API 파싱에 필수로 필요한 코드
     *         item.optString("description")
     *         item.optString("link")
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

                // ✅ 날짜 파싱 및 저장
                String pubDate = item.optString("pubDate", "");
                // log.info("📅 원본 pubDate: [{}]", pubDate);

                if (!pubDate.isEmpty()) {
                    LocalDateTime parseDate = parsePubDate(pubDate);
                    news.setPublishedAt(parseDate);
                    // log.info("✅ 파싱된 날짜 저장: [{}]", parseDate);
                } else {
                    news.setPublishedAt(LocalDateTime.now());
                    log.warn("⚠️ pubDate 없음, 현재 날짜 사용");
                }

                if (news.getSourceUrl() != null && !news.getSourceUrl().isEmpty()) {
                    newsList.add(news);
                }
            }

        } catch (Exception e) {
            log.error("네이버 뉴스 파싱 오류", e);
        }

        return newsList;
    }

    private LocalDateTime parsePubDate(String pubDateStr) {
        try {
            // "Mon, 02 Dec 2024 14:30:00 +0900" 형식
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                    "EEE, dd MMM yyyy HH:mm:ss Z",
                    Locale.ENGLISH);
            ZonedDateTime zdt = ZonedDateTime.parse(pubDateStr, formatter);
            LocalDateTime result = zdt.toLocalDateTime();
            // log.info("✅ parsePubDate 성공 - 결과: [{}]", result); // ✅ INFO 레벨로 추가
            return result;

        } catch (Exception e) {
            log.error("❌ pubDate 파싱 실패: [{}], 에러: {}", pubDateStr, e.getMessage(), e); // ✅ ERROR로 변경 + 스택트레이스 추가
            return LocalDateTime.now();
        }
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

    // /**
    // * ✅ 직군별 특화 관련성 판단
    // */
    // private boolean isJobGroupRelated(String title, String content, String
    // jobGroup) {
    // if (title == null)
    // return false;
    // // 제목이 없으면 직군 관련성을 판단할 수 없으므로 false 반환

    // String text = (title + " " + (content != null ? content : "")).toLowerCase();
    // // 제목 + 내용(본문)을 하나의 문자열로 합치고 모두 소문자로 변환해서 비교를 쉽게 만듦

    // // 1. 공통 제외 키워드 체크
    // for (String excludeKeyword : COMMON_EXCLUDE_KEYWORDS) {
    // if (text.contains(excludeKeyword.toLowerCase())) {
    // return false;
    // }
    // }
    // // 공통 제외 키워드 목록에 있는 단어가 포함되어 있으면 이 뉴스는 직군과 무관하다고 판단하여 즉시 false

    // // 2. 해당 직군 필터링 키워드 점수 계산
    // Set<String> jobFilters = JOB_GROUP_FILTERS.getOrDefault(jobGroup, Set.of());
    // // 직군(jobGroup)별 필터링 키워드 목록을 가져옴. 없으면 빈 Set 반환

    // int score = 0;
    // // 관련성 점수를 계산하기 위한 변수

    // for (String filter : jobFilters) {
    // if (text.contains(filter.toLowerCase())) {
    // // 뉴스 텍스트에 해당 직군의 필터 단어가 포함되면

    // if (title.toLowerCase().contains(filter.toLowerCase())) {
    // score += 2;
    // } else {
    // score += 1;
    // }
    // // 제목에 포함되면 가중치 2점, 본문에만 있으면 1점 추가
    // }
    // }

    // // 3. 채용 관련 키워드 가산점
    // if (text.matches(".*채용|구인|모집|입사|취업|면접.*")) {
    // score += 2;
    // }
    // // 텍스트 안에 '채용, 구인, 모집, 입사, 취업, 면접' 같은 단어가 있으면 +2점 추가
    // // 즉 "채용 관련 뉴스" 가능성이 있으면 강하게 가산점

    // // 4. 직군별 임계점 설정 (개발직군은 더 까다롭게)
    // int threshold = "개발".equals(jobGroup) ? 3 : 2;
    // // 개발 직군이면 임계점을 3점으로 설정해 더 엄격하게,
    // // 그 외 직군은 2점 이상이면 관련된 것으로 판단

    // boolean isRelated = score >= threshold;
    // // 최종 점수가 임계값 이상이면 직군 관련 뉴스라고 판단

    // log.debug("'{}' 직군 관련성 판단 - 제목: '{}', 점수: {}/{}, 결과: {}",
    // jobGroup,
    // title.length() > 40 ? title.substring(0, 40) + "..." : title,
    // score, threshold, isRelated);
    // // 디버깅용 로그: 어떤 직군으로 평가했는지, 제목 일부, 점수/임계치, 최종결과 출력

    // return isRelated;
    // // 최종 판단 결과 반환
    // }

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
