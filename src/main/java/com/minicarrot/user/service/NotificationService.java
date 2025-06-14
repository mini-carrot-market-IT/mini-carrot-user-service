package com.minicarrot.user.service;

import com.minicarrot.user.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    /**
     * 실시간 알림 생성
     */
    public NotificationDto createRealTimeNotification(Long userId, String eventType, String title, String message) {
        return NotificationDto.builder()
                .type("REALTIME")
                .userId(userId)
                .title(title)
                .message(message)
                .priority("MEDIUM")
                .createdAt(LocalDateTime.now())
                .channel("SSE")
                .build();
    }

    /**
     * 회원가입 실시간 알림 생성
     */
    public Map<String, Object> createRegistrationNotification(Long userId, String nickname) {
        return Map.of(
                "type", "user_registration",
                "title", "회원가입 완료! 🎉",
                "message", String.format("%s님, 미니 당근에 오신 것을 환영합니다!", nickname),
                "userId", userId,
                "timestamp", System.currentTimeMillis(),
                "icon", "🥕"
        );
    }

    /**
     * 프로필 업데이트 실시간 알림 생성
     */
    public Map<String, Object> createProfileUpdateNotification(Long userId, String nickname) {
        return Map.of(
                "type", "profile_update",
                "title", "프로필 업데이트 완료! ✨",
                "message", String.format("%s님의 프로필이 성공적으로 업데이트되었습니다.", nickname),
                "userId", userId,
                "timestamp", System.currentTimeMillis(),
                "icon", "👤"
        );
    }

    /**
     * 시스템 알림 생성
     */
    public Map<String, Object> createSystemNotification(String title, String message) {
        return Map.of(
                "type", "system",
                "title", title,
                "message", message,
                "timestamp", System.currentTimeMillis(),
                "icon", "🔔"
        );
    }

    /**
     * 에러 알림 생성
     */
    public Map<String, Object> createErrorNotification(String title, String message) {
        return Map.of(
                "type", "error",
                "title", title,
                "message", message,
                "timestamp", System.currentTimeMillis(),
                "icon", "⚠️"
        );
    }
} 