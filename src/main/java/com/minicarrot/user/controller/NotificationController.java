package com.minicarrot.user.controller;

import com.minicarrot.user.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;
    
    // 사용자별 SSE 연결 관리
    private final ConcurrentHashMap<Long, SseEmitter> userConnections = new ConcurrentHashMap<>();

    /**
     * SSE 연결 생성 - 실시간 알림 구독
     */
    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@PathVariable Long userId) {
        log.info("🔔 실시간 알림 구독 시작: userId={}", userId);
        
        // 30분 타임아웃 설정
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        // 연결 저장
        userConnections.put(userId, emitter);
        
        // 연결 완료 시 정리
        emitter.onCompletion(() -> {
            log.info("🔌 SSE 연결 완료: userId={}", userId);
            userConnections.remove(userId);
        });
        
        // 타임아웃 시 정리
        emitter.onTimeout(() -> {
            log.info("⏰ SSE 연결 타임아웃: userId={}", userId);
            userConnections.remove(userId);
        });
        
        // 에러 시 정리
        emitter.onError((ex) -> {
            log.error("❌ SSE 연결 에러: userId={}, error={}", userId, ex.getMessage());
            userConnections.remove(userId);
        });
        
        try {
            // 연결 확인 메시지 전송 (JSON 형식으로 통일)
            Map<String, Object> connectionMessage = Map.of(
                "type", "connection",
                "message", "실시간 알림이 연결되었습니다!",
                "timestamp", System.currentTimeMillis(),
                "userId", userId
            );
            
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(connectionMessage)
                .id(String.valueOf(System.currentTimeMillis())));
                
            log.info("✅ SSE 연결 성공: userId={}", userId);
        } catch (Exception e) {
            log.error("❌ 초기 메시지 전송 실패: userId={}, error={}", userId, e.getMessage());
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    /**
     * 특정 사용자에게 실시간 알림 전송
     */
    public void sendNotificationToUser(Long userId, String eventType, Object data) {
        SseEmitter emitter = userConnections.get(userId);
        
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(data)
                    .id(String.valueOf(System.currentTimeMillis())));
                    
                log.info("📤 실시간 알림 전송 성공: userId={}, eventType={}", userId, eventType);
            } catch (Exception e) {
                log.error("❌ 실시간 알림 전송 실패: userId={}, error={}", userId, e.getMessage());
                // 연결이 끊어진 경우 제거
                userConnections.remove(userId);
            }
        } else {
            log.debug("🔍 SSE 연결 없음: userId={}", userId);
        }
    }

    /**
     * 모든 연결된 사용자에게 브로드캐스트
     */
    public void broadcastNotification(String eventType, Object data) {
        log.info("📢 브로드캐스트 알림: eventType={}, 연결된 사용자 수={}", eventType, userConnections.size());
        
        userConnections.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(data)
                    .id(String.valueOf(System.currentTimeMillis())));
            } catch (Exception e) {
                log.error("❌ 브로드캐스트 전송 실패: userId={}, error={}", userId, e.getMessage());
                userConnections.remove(userId);
            }
        });
    }

    /**
     * 연결 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<?> getConnectionStatus() {
        return ResponseEntity.ok()
            .body(Map.of(
                "connectedUsers", userConnections.size(),
                "userIds", userConnections.keySet()
            ));
    }

    /**
     * 테스트용 알림 전송
     */
    @PostMapping("/test/{userId}")
    public ResponseEntity<?> sendTestNotification(@PathVariable Long userId) {
        sendNotificationToUser(userId, "test", 
            Map.of(
                "title", "테스트 알림",
                "message", "실시간 알림이 정상적으로 작동합니다!",
                "timestamp", System.currentTimeMillis()
            ));
        
        return ResponseEntity.ok()
            .body(Map.of("message", "테스트 알림이 전송되었습니다."));
    }
} 