package io.moyam.chatbot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * 캐시 설정
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 기본 캐시 설정
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)           // 최대 1000개 엔트리
            .expireAfterWrite(Duration.ofMinutes(30))  // 30분 후 만료
            .expireAfterAccess(Duration.ofMinutes(10)) // 10분 미접근 시 만료
            .recordStats());             // 통계 수집
            
        // 캐시 이름 미리 등록
        cacheManager.setCacheNames(
            List.of(
                "scenarios",        // 시나리오 정보
                "scenarioSteps",    // 시나리오 단계
                "bots"              // 봇 정보
            )
        );
        
        return cacheManager;
    }
}
