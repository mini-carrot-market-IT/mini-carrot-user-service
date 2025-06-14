package com.minicarrot.user.service;

import com.minicarrot.user.dto.ProductStatsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceClient {

    private final RestTemplate restTemplate;
    
    @Value("${app.product-service.url:http://product-service:8082}")
    private String productServiceUrl;

    /**
     * 사용자별 상품 통계 조회 (타임아웃 적용)
     */
    public ProductStatsDto getUserProductStatsWithTimeout(Long userId, int timeoutMs) {
        try {
            log.info("Product Service에서 사용자 통계 조회 (타임아웃 {}ms): userId={}", timeoutMs, userId);
            
            // 타임아웃 설정된 RestTemplate 생성
            RestTemplate timeoutRestTemplate = createTimeoutRestTemplate(timeoutMs);
            
            // Product Service API 호출
            String url = productServiceUrl + "/api/products/stats/" + userId;
            Map<String, Object> response = timeoutRestTemplate.getForObject(url, Map.class);
            
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                
                return ProductStatsDto.builder()
                        .userId(userId)
                        .registeredProducts((Integer) data.getOrDefault("registeredCount", 0))
                        .purchasedProducts((Integer) data.getOrDefault("purchasedCount", 0))
                        .soldProducts((Integer) data.getOrDefault("soldCount", 0))
                        .totalTransactions((Integer) data.getOrDefault("totalTransactions", 0))
                        .totalSales(((Number) data.getOrDefault("totalSalesAmount", 0.0)).doubleValue())
                        .totalPurchases(((Number) data.getOrDefault("totalPurchaseAmount", 0.0)).doubleValue())
                        .build();
            }
            
        } catch (Exception e) {
            log.warn("Product Service 연동 실패 (타임아웃): userId={}, error={}", userId, e.getMessage());
        }
        
        // 실패 시 기본값 반환
        return ProductStatsDto.createDefault(userId);
    }

    /**
     * 사용자가 등록한 상품 목록 조회 (타임아웃 적용)
     */
    public List<Map<String, Object>> getUserProductsWithTimeout(Long userId, int timeoutMs) {
        try {
            log.info("사용자 등록 상품 조회 (타임아웃 {}ms): userId={}", timeoutMs, userId);
            
            // 타임아웃 설정된 RestTemplate 생성
            RestTemplate timeoutRestTemplate = createTimeoutRestTemplate(timeoutMs);
            
            // 전체 상품 목록을 가져와서 userId로 필터링
            String url = productServiceUrl + "/api/products";
            Map<String, Object> response = timeoutRestTemplate.getForObject(url, Map.class);
            
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                List<Map<String, Object>> allProducts = (List<Map<String, Object>>) response.get("data");
                
                // userId로 필터링 (sellerId 또는 userId 필드 확인)
                List<Map<String, Object>> userProducts = allProducts.stream()
                    .filter(product -> {
                        Object sellerId = product.get("sellerId");
                        Object productUserId = product.get("userId");
                        return (sellerId != null && sellerId.equals(userId.intValue())) ||
                               (productUserId != null && productUserId.equals(userId.intValue()));
                    })
                    .collect(Collectors.toList());
                
                log.info("사용자 등록 상품 필터링 완료: userId={}, 전체={}개, 필터링={}개", 
                    userId, allProducts.size(), userProducts.size());
                
                return userProducts;
            }
            
        } catch (Exception e) {
            log.warn("사용자 상품 조회 실패 (타임아웃): userId={}, error={}", userId, e.getMessage());
        }
        
        return List.of();
    }

    /**
     * 최근 활동 내역 조회 (타임아웃 적용)
     */
    public List<Map<String, Object>> getRecentActivityWithTimeout(Long userId, int timeoutMs) {
        try {
            log.info("최근 활동 내역 조회 (타임아웃 {}ms): userId={}", timeoutMs, userId);
            
            // 최근 활동은 등록한 상품 기반으로 생성
            List<Map<String, Object>> userProducts = getUserProductsWithTimeout(userId, timeoutMs);
            
            // 최근 5개 상품을 활동으로 변환
            List<Map<String, Object>> activities = userProducts.stream()
                .limit(5)
                .map(product -> Map.of(
                    "type", "product_registered",
                    "message", String.format("'%s' 상품을 등록했습니다.", product.get("title")),
                    "productId", product.get("productId"),
                    "timestamp", System.currentTimeMillis() - (long)(Math.random() * 86400000) // 임시 타임스탬프
                ))
                .collect(Collectors.toList());
            
            return activities;
            
        } catch (Exception e) {
            log.warn("최근 활동 조회 실패 (타임아웃): userId={}, error={}", userId, e.getMessage());
        }
        
        return List.of();
    }

    /**
     * 타임아웃이 설정된 RestTemplate 생성
     */
    private RestTemplate createTimeoutRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }

    /**
     * 사용자별 상품 통계 조회
     */
    public ProductStatsDto getUserProductStats(Long userId) {
        return getUserProductStatsWithTimeout(userId, 5000); // 기본 5초 타임아웃
    }

    /**
     * 사용자가 등록한 상품 목록 조회
     */
    public List<Map<String, Object>> getUserProducts(Long userId) {
        return getUserProductsWithTimeout(userId, 5000); // 기본 5초 타임아웃
    }

    /**
     * 사용자가 구매한 상품 목록 조회
     */
    public List<Map<String, Object>> getUserPurchases(Long userId) {
        try {
            log.info("사용자 구매 상품 조회: userId={}", userId);
            
            // 구매 내역은 별도 API가 필요하므로 임시로 빈 목록 반환
            // TODO: Product Service에 구매 내역 API 추가 필요
            log.info("구매 내역 API는 아직 구현되지 않음: userId={}", userId);
            
            return List.of();
            
        } catch (Exception e) {
            log.warn("사용자 구매 내역 조회 실패: userId={}, error={}", userId, e.getMessage());
        }
        
        return List.of();
    }

    /**
     * 최근 활동 내역 조회
     */
    public List<Map<String, Object>> getRecentActivity(Long userId) {
        return getRecentActivityWithTimeout(userId, 5000); // 기본 5초 타임아웃
    }
} 