package com.minicarrot.user.service;

import com.minicarrot.user.dto.UserResponse;
import com.minicarrot.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * JWT 토큰 캐싱 서비스
 * - 토큰 검증 결과를 메모리에 캐싱하여 성능 향상
 * - 만료된 캐시 자동 정리
 * - 로컬 토큰 검증으로 DB 조회 최소화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtCacheService {

    private final JwtUtil jwtUtil;
    
    // 토큰 캐시 (토큰 해시 -> 사용자 정보)
    private final ConcurrentHashMap<String, CachedUserInfo> tokenCache = new ConcurrentHashMap<>();
    
    // 캐시 정리를 위한 스케줄러
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // 캐시 TTL (5분)
    private static final long CACHE_TTL_MINUTES = 5;
    
    /**
     * 캐시된 사용자 정보 클래스
     */
    private static class CachedUserInfo {
        private final UserResponse userResponse;
        private final long cachedAt;
        private final long expiresAt;
        
        public CachedUserInfo(UserResponse userResponse) {
            this.userResponse = userResponse;
            this.cachedAt = System.currentTimeMillis();
            this.expiresAt = cachedAt + TimeUnit.MINUTES.toMillis(CACHE_TTL_MINUTES);
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
        
        public UserResponse getUserResponse() {
            return userResponse;
        }
    }
    
    /**
     * 초기화 - 캐시 정리 스케줄러 시작
     */
    public void init() {
        // 1분마다 만료된 캐시 정리
        scheduler.scheduleAtFixedRate(this::cleanExpiredCache, 1, 1, TimeUnit.MINUTES);
        log.info("JWT 캐시 서비스 초기화 완료 - TTL: {}분", CACHE_TTL_MINUTES);
    }
    
    /**
     * 토큰에서 사용자 정보 조회 (캐시 우선)
     */
    public UserResponse getUserFromToken(String token) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Bearer 토큰에서 실제 토큰 추출
            String actualToken = extractActualToken(token);
            
            // 1. 로컬 토큰 검증 (빠른 실패)
            if (!jwtUtil.isTokenValid(actualToken)) {
                log.warn("유효하지 않은 토큰");
                throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
            }
            
            // 2. 토큰 해시 생성 (캐시 키)
            String tokenHash = generateTokenHash(actualToken);
            
            // 3. 캐시에서 조회
            CachedUserInfo cachedInfo = tokenCache.get(tokenHash);
            if (cachedInfo != null && !cachedInfo.isExpired()) {
                long duration = System.currentTimeMillis() - startTime;
                log.debug("캐시에서 사용자 정보 조회 성공: userId={}, 소요시간={}ms", 
                    cachedInfo.getUserResponse().getUserId(), duration);
                return cachedInfo.getUserResponse();
            }
            
            // 4. 캐시 미스 - 토큰에서 직접 사용자 정보 추출
            UserResponse userResponse = extractUserFromToken(actualToken);
            
            // 5. 캐시에 저장
            tokenCache.put(tokenHash, new CachedUserInfo(userResponse));
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("토큰에서 사용자 정보 추출 및 캐싱 완료: userId={}, 소요시간={}ms", 
                userResponse.getUserId(), duration);
            
            return userResponse;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("토큰에서 사용자 정보 조회 실패: error={}, 소요시간={}ms", e.getMessage(), duration);
            throw e;
        }
    }
    
    /**
     * 토큰에서 직접 사용자 정보 추출 (DB 조회 없이)
     */
    private UserResponse extractUserFromToken(String token) {
        try {
            String email = jwtUtil.getEmailFromToken(token);
            Long userId = jwtUtil.getUserIdFromToken(token);
            String nickname = jwtUtil.getNicknameFromToken(token);
            
            return UserResponse.builder()
                    .userId(userId)
                    .email(email)
                    .nickname(nickname)
                    .build();
                    
        } catch (Exception e) {
            log.error("토큰에서 사용자 정보 추출 실패: error={}", e.getMessage());
            throw new IllegalArgumentException("토큰에서 사용자 정보를 추출할 수 없습니다.", e);
        }
    }
    
    /**
     * Bearer 토큰에서 실제 토큰 추출
     */
    private String extractActualToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("토큰이 제공되지 않았습니다.");
        }
        
        if (token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        
        return token;
    }
    
    /**
     * 토큰 해시 생성 (캐시 키용)
     */
    private String generateTokenHash(String token) {
        // 토큰의 마지막 20자리를 해시로 사용 (보안상 전체 토큰 저장 방지)
        if (token.length() > 20) {
            return token.substring(token.length() - 20);
        }
        return token;
    }
    
    /**
     * 특정 사용자의 캐시 무효화
     */
    public void invalidateUserCache(Long userId) {
        tokenCache.entrySet().removeIf(entry -> {
            CachedUserInfo info = entry.getValue();
            return info.getUserResponse().getUserId().equals(userId);
        });
        log.debug("사용자 캐시 무효화 완료: userId={}", userId);
    }
    
    /**
     * 만료된 캐시 정리
     */
    private void cleanExpiredCache() {
        int beforeSize = tokenCache.size();
        tokenCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int afterSize = tokenCache.size();
        
        if (beforeSize != afterSize) {
            log.debug("만료된 캐시 정리 완료: {}개 -> {}개 ({}개 제거)", 
                beforeSize, afterSize, beforeSize - afterSize);
        }
    }
    
    /**
     * 캐시 통계 조회
     */
    public CacheStats getCacheStats() {
        int totalEntries = tokenCache.size();
        long expiredEntries = tokenCache.values().stream()
                .mapToLong(info -> info.isExpired() ? 1 : 0)
                .sum();
        
        return new CacheStats(totalEntries, (int) expiredEntries);
    }
    
    /**
     * 캐시 통계 클래스
     */
    public static class CacheStats {
        private final int totalEntries;
        private final int expiredEntries;
        
        public CacheStats(int totalEntries, int expiredEntries) {
            this.totalEntries = totalEntries;
            this.expiredEntries = expiredEntries;
        }
        
        public int getTotalEntries() { return totalEntries; }
        public int getExpiredEntries() { return expiredEntries; }
        public int getActiveEntries() { return totalEntries - expiredEntries; }
    }
    
    /**
     * 서비스 종료 시 스케줄러 정리
     */
    public void destroy() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("JWT 캐시 서비스 종료 완료");
    }
} 