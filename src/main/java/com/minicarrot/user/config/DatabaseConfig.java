package com.minicarrot.user.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * 데이터베이스 연결 안정성을 위한 설정
 * - 재시도 로직 구성
 * - 커스텀 헬스체크 구현
 * - 연결 모니터링 설정
 */
@Configuration
@EnableRetry
@Slf4j
public class DatabaseConfig {

    /**
     * 데이터베이스 연결을 위한 재시도 템플릿
     * 지수 백오프 정책으로 재시도 간격을 점진적으로 증가
     */
    @Bean
    public RetryTemplate databaseRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // 재시도 정책: 최대 5회 시도
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(5);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // 백오프 정책: 지수적 증가 (1초 -> 2초 -> 4초 -> 8초 -> 16초)
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);  // 1초
        backOffPolicy.setMultiplier(2.0);        // 2배씩 증가
        backOffPolicy.setMaxInterval(30000);     // 최대 30초
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        log.info("데이터베이스 재시도 템플릿 구성 완료 - 최대 5회 시도, 지수 백오프");
        return retryTemplate;
    }

    /**
     * 커스텀 데이터베이스 헬스 인디케이터
     * 기본 헬스체크보다 더 상세한 정보 제공
     */
    @Component
    @ConditionalOnProperty(name = "management.health.db.enabled", havingValue = "true", matchIfMissing = true)
    public static class CustomDatabaseHealthIndicator implements HealthIndicator {

        @Autowired
        private DataSource dataSource;

        @Override
        public Health health() {
            try {
                return checkDatabaseHealth();
            } catch (Exception e) {
                log.error("데이터베이스 헬스체크 실패", e);
                return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", System.currentTimeMillis())
                    .build();
            }
        }

        private Health checkDatabaseHealth() throws SQLException {
            long startTime = System.currentTimeMillis();
            
            try (Connection connection = dataSource.getConnection()) {
                // 연결 테스트 쿼리 실행
                boolean isValid = connection.isValid(5); // 5초 타임아웃
                long responseTime = System.currentTimeMillis() - startTime;
                
                Map<String, Object> details = new HashMap<>();
                details.put("database", connection.getMetaData().getDatabaseProductName());
                details.put("version", connection.getMetaData().getDatabaseProductVersion());
                details.put("url", connection.getMetaData().getURL());
                details.put("responseTime", responseTime + "ms");
                details.put("timestamp", System.currentTimeMillis());
                
                if (isValid && responseTime < 5000) { // 5초 이내 응답
                    details.put("status", "healthy");
                    log.debug("데이터베이스 헬스체크 성공 - 응답시간: {}ms", responseTime);
                    return Health.up().withDetails(details).build();
                } else {
                    details.put("status", "slow_response");
                    log.warn("데이터베이스 응답 지연 - 응답시간: {}ms", responseTime);
                    return Health.down().withDetails(details).build();
                }
            }
        }
    }

    /**
     * 데이터베이스 연결 모니터링을 위한 빈
     */
    @Component
    @Slf4j
    public static class DatabaseConnectionMonitor {

        @Autowired
        private DataSource dataSource;

        /**
         * 애플리케이션 시작 시 데이터베이스 연결 상태 확인
         */
        @org.springframework.context.event.EventListener
        public void onApplicationReady(org.springframework.boot.context.event.ApplicationReadyEvent event) {
            log.info("애플리케이션 시작 완료 - 데이터베이스 연결 상태 확인 시작");
            checkInitialConnection();
        }

        private void checkInitialConnection() {
            try (Connection connection = dataSource.getConnection()) {
                log.info("데이터베이스 연결 성공 - URL: {}, 제품: {} {}", 
                    connection.getMetaData().getURL(),
                    connection.getMetaData().getDatabaseProductName(),
                    connection.getMetaData().getDatabaseProductVersion());
            } catch (SQLException e) {
                log.error("데이터베이스 연결 실패", e);
            }
        }
    }
} 