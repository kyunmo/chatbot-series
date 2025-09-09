package io.moyam.chatbot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * ChatBot 캐시 설정
 * 
 * 각 도메인별 특성에 맞는 캐시 전략 적용:
 * - 시나리오: 자주 변경되지 않는 정적 데이터
 * - 의도분석: 사용자 패턴 변화 반영을 위한 짧은 TTL
 * - 오타교정: 규칙이 안정적이므로 긴 TTL
 * - 대화블록: 운영진 수정 시 빠른 반영을 위한 중간 TTL
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 시나리오 정보 캐시 (1시간)
        cacheManager.registerCustomCache("scenarios", 
            Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofHours(1))
                .recordStats()
                .build());
        
        // 시나리오 단계 캐시 (1시간)
        cacheManager.registerCustomCache("scenarioSteps",
            Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(Duration.ofHours(1))
                .recordStats()
                .build());
        
        // 봇 정보 캐시 (30분)
        cacheManager.registerCustomCache("bots",
            Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats()
                .build());
        
        // 의도분석 결과 캐시 (5분) - 사용자 대화 패턴 변화 반영
        cacheManager.registerCustomCache("intentAnalysis", 
            Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .expireAfterAccess(Duration.ofMinutes(2))  // 2분 미접근 시 만료
                .recordStats()
                .build());
        
        // 오타교정 결과 캐시 (1시간) - 오타 규칙은 자주 바뀌지 않음
        cacheManager.registerCustomCache("typoCorrection",
            Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(Duration.ofHours(1))
                .expireAfterAccess(Duration.ofMinutes(30)) // 30분 미접근 시 만료
                .recordStats()
                .build());
        
        // 대화블록 캐시 (10분) - 운영진 응답 메시지 수정 시 빠른 반영
        cacheManager.registerCustomCache("conversationBlocks",
            Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(10))
                .expireAfterAccess(Duration.ofMinutes(5))  // 5분 미접근 시 만료
                .recordStats()
                .build());
        
        // TF-IDF 벡터 캐시 (30분)
        cacheManager.registerCustomCache("tfidfVectors",
            Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats()
                .build());
        
        // 지식베이스 검색 결과 캐시 (10분)
        cacheManager.registerCustomCache("knowledgeSearch",
            Caffeine.newBuilder()
                .maximumSize(1500)
                .expireAfterWrite(Duration.ofMinutes(10))
                .expireAfterAccess(Duration.ofMinutes(3))
                .recordStats()
                .build());
        
        return cacheManager;
    }
}
