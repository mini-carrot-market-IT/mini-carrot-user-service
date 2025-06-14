package com.minicarrot.user.config;

import com.minicarrot.user.service.JwtCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import jakarta.annotation.PreDestroy;

/**
 * 캐시 설정 클래스
 * - JWT 캐시 서비스 초기화 및 종료 관리
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CacheConfig {

    private final JwtCacheService jwtCacheService;

    /**
     * 애플리케이션 시작 시 캐시 서비스 초기화
     */
    @EventListener(ContextRefreshedEvent.class)
    public void initializeCache() {
        log.info("캐시 서비스 초기화 시작");
        jwtCacheService.init();
        log.info("캐시 서비스 초기화 완료");
    }

    /**
     * 애플리케이션 종료 시 캐시 서비스 정리
     */
    @PreDestroy
    public void destroyCache() {
        log.info("캐시 서비스 종료 시작");
        jwtCacheService.destroy();
        log.info("캐시 서비스 종료 완료");
    }
} 