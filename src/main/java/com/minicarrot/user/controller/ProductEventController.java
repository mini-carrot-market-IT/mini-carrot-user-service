package com.minicarrot.user.controller;

import com.minicarrot.user.common.ApiResponse;
import com.minicarrot.user.controller.NotificationController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductEventController {

    private final NotificationController notificationController;

    /**
     * 상품 등록 이벤트 수신 (Product Service에서 호출)
     */
    @PostMapping("/product/registered")
    public ResponseEntity<ApiResponse<Void>> onProductRegistered(@RequestBody Map<String, Object> eventData) {
        try {
            Long userId = ((Number) eventData.get("userId")).longValue();
            String productName = (String) eventData.get("productName");
            Long productId = ((Number) eventData.get("productId")).longValue();
            
            log.info("상품 등록 이벤트 수신: userId={}, productId={}, productName={}", userId, productId, productName);
            
            // 실시간 알림 전송
            Map<String, Object> notification = Map.of(
                "type", "product_registered",
                "title", "상품 등록 완료! 🎉",
                "message", String.format("'%s' 상품이 성공적으로 등록되었습니다.", productName),
                "productId", productId,
                "timestamp", System.currentTimeMillis()
            );
            
            notificationController.sendNotificationToUser(userId, "product_registered", notification);
            
            // 대시보드 자동 새로고침 이벤트 발송
            sendDashboardRefreshEvent(userId, "product_registered");
            
            return ResponseEntity.ok(ApiResponse.success("상품 등록 알림이 전송되었습니다.", null));
            
        } catch (Exception e) {
            log.error("상품 등록 이벤트 처리 실패: error={}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success("이벤트 처리 완료", null));
        }
    }

    /**
     * 상품 구매 이벤트 수신 (Product Service에서 호출)
     */
    @PostMapping("/product/purchased")
    public ResponseEntity<ApiResponse<Void>> onProductPurchased(@RequestBody Map<String, Object> eventData) {
        try {
            Long sellerId = ((Number) eventData.get("sellerId")).longValue();
            Long buyerId = ((Number) eventData.get("buyerId")).longValue();
            String productName = (String) eventData.get("productName");
            Long productId = ((Number) eventData.get("productId")).longValue();
            Double price = ((Number) eventData.get("price")).doubleValue();
            
            log.info("상품 구매 이벤트 수신: sellerId={}, buyerId={}, productId={}, productName={}", 
                sellerId, buyerId, productId, productName);
            
            // 판매자 알림
            Map<String, Object> sellerNotification = Map.of(
                "type", "product_sold",
                "title", "상품 판매 완료! 💰",
                "message", String.format("'%s' 상품이 판매되었습니다. (₩%,.0f)", productName, price),
                "productId", productId,
                "buyerId", buyerId,
                "timestamp", System.currentTimeMillis()
            );
            
            // 구매자 알림
            Map<String, Object> buyerNotification = Map.of(
                "type", "product_purchased",
                "title", "상품 구매 완료! 🛒",
                "message", String.format("'%s' 상품을 구매했습니다. (₩%,.0f)", productName, price),
                "productId", productId,
                "sellerId", sellerId,
                "timestamp", System.currentTimeMillis()
            );
            
            notificationController.sendNotificationToUser(sellerId, "product_sold", sellerNotification);
            notificationController.sendNotificationToUser(buyerId, "product_purchased", buyerNotification);
            
            // 양쪽 사용자 대시보드 자동 새로고침
            sendDashboardRefreshEvent(sellerId, "product_sold");
            sendDashboardRefreshEvent(buyerId, "product_purchased");
            
            return ResponseEntity.ok(ApiResponse.success("구매 알림이 전송되었습니다.", null));
            
        } catch (Exception e) {
            log.error("상품 구매 이벤트 처리 실패: error={}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success("이벤트 처리 완료", null));
        }
    }

    /**
     * 대시보드 새로고침 이벤트 (Product Service에서 호출)
     */
    @PostMapping("/dashboard/refresh")
    public ResponseEntity<ApiResponse<Void>> onDashboardRefresh(@RequestBody Map<String, Object> eventData) {
        try {
            Long userId = ((Number) eventData.get("userId")).longValue();
            String eventType = (String) eventData.getOrDefault("eventType", "dashboard_update");
            
            log.info("대시보드 새로고침 이벤트 수신: userId={}, eventType={}", userId, eventType);
            
            sendDashboardRefreshEvent(userId, eventType);
            
            return ResponseEntity.ok(ApiResponse.success("대시보드 새로고침 알림이 전송되었습니다.", null));
            
        } catch (Exception e) {
            log.error("대시보드 새로고침 이벤트 처리 실패: error={}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success("이벤트 처리 완료", null));
        }
    }

    /**
     * 대시보드 자동 새로고침 이벤트 발송 (내부 메서드)
     */
    private void sendDashboardRefreshEvent(Long userId, String eventType) {
        try {
            Map<String, Object> refreshNotification = Map.of(
                "type", "dashboard_refresh",
                "eventType", eventType,
                "message", "대시보드 정보가 업데이트되었습니다.",
                "autoRefresh", true,
                "timestamp", System.currentTimeMillis()
            );
            
            notificationController.sendNotificationToUser(userId, "dashboard_refresh", refreshNotification);
            log.debug("대시보드 자동 새로고침 이벤트 발송: userId={}, eventType={}", userId, eventType);
            
        } catch (Exception e) {
            log.warn("대시보드 새로고침 이벤트 발송 실패: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 상품 상태 변경 이벤트 (판매중 -> 판매완료 등)
     */
    @PostMapping("/product/status-changed")
    public ResponseEntity<ApiResponse<Void>> onProductStatusChanged(@RequestBody Map<String, Object> eventData) {
        try {
            Long userId = ((Number) eventData.get("userId")).longValue();
            String productName = (String) eventData.get("productName");
            String oldStatus = (String) eventData.get("oldStatus");
            String newStatus = (String) eventData.get("newStatus");
            
            log.info("상품 상태 변경 이벤트 수신: userId={}, productName={}, {}→{}", 
                userId, productName, oldStatus, newStatus);
            
            String statusMessage = getStatusChangeMessage(oldStatus, newStatus);
            
            Map<String, Object> notification = Map.of(
                "type", "product_status_changed",
                "title", "상품 상태 변경 📋",
                "message", String.format("'%s' %s", productName, statusMessage),
                "oldStatus", oldStatus,
                "newStatus", newStatus,
                "timestamp", System.currentTimeMillis()
            );
            
            notificationController.sendNotificationToUser(userId, "product_status_changed", notification);
            sendDashboardRefreshEvent(userId, "product_status_changed");
            
            return ResponseEntity.ok(ApiResponse.success("상품 상태 변경 알림이 전송되었습니다.", null));
            
        } catch (Exception e) {
            log.error("상품 상태 변경 이벤트 처리 실패: error={}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success("이벤트 처리 완료", null));
        }
    }

    /**
     * 상태 변경 메시지 생성
     */
    private String getStatusChangeMessage(String oldStatus, String newStatus) {
        return switch (newStatus.toUpperCase()) {
            case "SOLD" -> "상품이 판매완료되었습니다.";
            case "RESERVED" -> "상품이 예약중으로 변경되었습니다.";
            case "AVAILABLE" -> "상품이 판매중으로 변경되었습니다.";
            case "HIDDEN" -> "상품이 숨김처리되었습니다.";
            default -> String.format("상품 상태가 %s에서 %s로 변경되었습니다.", oldStatus, newStatus);
        };
    }
} 