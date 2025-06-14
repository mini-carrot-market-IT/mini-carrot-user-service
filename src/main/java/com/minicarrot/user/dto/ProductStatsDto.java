package com.minicarrot.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStatsDto {
    
    private Long userId;
    private int registeredProducts;
    private int purchasedProducts;
    private int soldProducts;
    private int totalTransactions;
    private double totalSales;
    private double totalPurchases;
    
    // 기본값 생성 메서드
    public static ProductStatsDto createDefault(Long userId) {
        return ProductStatsDto.builder()
                .userId(userId)
                .registeredProducts(0)
                .purchasedProducts(0)
                .soldProducts(0)
                .totalTransactions(0)
                .totalSales(0.0)
                .totalPurchases(0.0)
                .build();
    }
} 