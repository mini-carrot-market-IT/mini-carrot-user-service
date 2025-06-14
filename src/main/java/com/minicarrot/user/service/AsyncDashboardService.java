package com.minicarrot.user.service;

import com.minicarrot.user.dto.ProductStatsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncDashboardService {

    private final ProductServiceClient productServiceClient;
    
    // 대시보드 데이터 캐시 (5분 TTL)
    private final Map<Long, DashboardCache> dashboardCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 5 * 60 * 1000; // 5분

    /**
     * 대시보드 데이터를 비동기로 조회하고 캐시
     */
    public Map<String, Object> getDashboardData(Long userId) {
        // 캐시 확인
        DashboardCache cached = dashboardCache.get(userId);
        if (cached != null && !cached.isExpired()) {
            log.info("대시보드 캐시 히트: userId={}", userId);
            return cached.getData();
        }

        // 캐시 미스 또는 만료 - 빠른 응답을 위해 기본값 먼저 반환
        Map<String, Object> defaultData = createDefaultDashboard(userId);
        
        // 백그라운드에서 실제 데이터 로드
        loadDashboardDataAsync(userId);
        
        return defaultData;
    }

    /**
     * 백그라운드에서 실제 대시보드 데이터 로드
     */
    @Async
    public void loadDashboardDataAsync(Long userId) {
        try {
            log.info("대시보드 데이터 비동기 로드 시작: userId={}", userId);
            
            // 병렬로 데이터 조회
            CompletableFuture<ProductStatsDto> statsFuture = CompletableFuture
                .supplyAsync(() -> productServiceClient.getUserProductStatsWithTimeout(userId, 2000));
            
            CompletableFuture<List<Map<String, Object>>> productsFuture = CompletableFuture
                .supplyAsync(() -> productServiceClient.getUserProductsWithTimeout(userId, 2000));
            
            CompletableFuture<List<Map<String, Object>>> activityFuture = CompletableFuture
                .supplyAsync(() -> productServiceClient.getRecentActivityWithTimeout(userId, 2000));

            // 모든 비동기 작업 완료 대기 (최대 3초)
            CompletableFuture.allOf(statsFuture, productsFuture, activityFuture)
                .orTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                .thenRun(() -> {
                    try {
                        ProductStatsDto stats = statsFuture.get();
                        List<Map<String, Object>> products = productsFuture.get();
                        List<Map<String, Object>> activity = activityFuture.get();
                        
                        Map<String, Object> dashboardData = Map.of(
                            "stats", Map.of(
                                "registeredProducts", stats.getRegisteredProducts(),
                                "purchasedProducts", stats.getPurchasedProducts(),
                                "soldProducts", stats.getSoldProducts(),
                                "totalTransactions", stats.getTotalTransactions(),
                                "totalSales", stats.getTotalSales(),
                                "totalPurchases", stats.getTotalPurchases()
                            ),
                            "recentProducts", products.stream().limit(5).toList(),
                            "recentActivity", activity,
                            "lastUpdated", System.currentTimeMillis(),
                            "cached", true
                        );
                        
                        // 캐시에 저장
                        dashboardCache.put(userId, new DashboardCache(dashboardData));
                        log.info("대시보드 데이터 캐시 업데이트 완료: userId={}", userId);
                        
                    } catch (Exception e) {
                        log.warn("대시보드 데이터 로드 실패: userId={}, error={}", userId, e.getMessage());
                    }
                })
                .exceptionally(throwable -> {
                    log.warn("대시보드 데이터 로드 타임아웃: userId={}", userId);
                    return null;
                });
                
        } catch (Exception e) {
            log.error("대시보드 비동기 로드 오류: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 기본 대시보드 데이터 생성 (즉시 응답용)
     */
    private Map<String, Object> createDefaultDashboard(Long userId) {
        return Map.of(
            "stats", Map.of(
                "registeredProducts", 0,
                "purchasedProducts", 0,
                "soldProducts", 0,
                "totalTransactions", 0,
                "totalSales", 0.0,
                "totalPurchases", 0.0
            ),
            "recentProducts", List.of(),
            "recentActivity", List.of(),
            "lastUpdated", System.currentTimeMillis(),
            "loading", true,
            "message", "데이터를 불러오는 중입니다..."
        );
    }

    /**
     * 캐시된 대시보드 데이터 조회 (실시간 업데이트용)
     */
    public Map<String, Object> getCachedDashboardData(Long userId) {
        DashboardCache cached = dashboardCache.get(userId);
        if (cached != null && !cached.isExpired()) {
            return cached.getData();
        }
        return null;
    }

    /**
     * 대시보드 캐시 무효화
     */
    public void invalidateDashboardCache(Long userId) {
        dashboardCache.remove(userId);
        log.info("대시보드 캐시 무효화: userId={}", userId);
    }

    /**
     * 만료된 캐시 정리
     */
    public void cleanupExpiredCache() {
        dashboardCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 대시보드 캐시 클래스
     */
    private static class DashboardCache {
        private final Map<String, Object> data;
        private final long timestamp;

        public DashboardCache(Map<String, Object> data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public Map<String, Object> getData() {
            return data;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL;
        }
    }
} 