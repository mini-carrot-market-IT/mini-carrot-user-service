package com.minicarrot.user.service;

import com.minicarrot.user.controller.NotificationController;
import com.minicarrot.user.dto.NotificationDto;
import com.minicarrot.user.dto.UserEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 사용자 이벤트 처리 서비스
 * - 로깅 기반 이벤트 처리
 * - SSE 실시간 알림 연동
 * - RabbitMQ 없이 동작
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserEventListener {

    private final NotificationController notificationController;
    private final NotificationService notificationService;

    /**
     * 사용자 등록 이벤트 처리 (로깅 기반)
     */
    public void handleUserRegistrationEvent(UserEventDto userEvent) {
        log.info("🎉 사용자 등록 이벤트 수신: userId={}, email={}, nickname={}", 
            userEvent.getUserId(), userEvent.getEmail(), userEvent.getNickname());
        
        // 실시간 알림 전송
        var notification = notificationService.createRegistrationNotification(
            userEvent.getUserId(), userEvent.getNickname());
        notificationController.sendNotificationToUser(
            userEvent.getUserId(), "user_registration", notification);
        
        // 여기서 실제 비즈니스 로직 처리
        processUserRegistration(userEvent);
    }

    /**
     * 사용자 프로필 업데이트 이벤트 처리 (로깅 기반)
     */
    public void handleUserProfileUpdateEvent(UserEventDto userEvent) {
        log.info("📝 사용자 프로필 업데이트 이벤트 수신: userId={}, nickname={}", 
            userEvent.getUserId(), userEvent.getNickname());
        
        // 실시간 알림 전송
        var notification = notificationService.createProfileUpdateNotification(
            userEvent.getUserId(), userEvent.getNickname());
        notificationController.sendNotificationToUser(
            userEvent.getUserId(), "profile_update", notification);
        
        // 여기서 실제 비즈니스 로직 처리
        processUserProfileUpdate(userEvent);
    }

    /**
     * 알림 이벤트 처리 (로깅 기반)
     */
    public void handleNotificationEvent(NotificationDto notification) {
        log.info("📧 알림 이벤트 수신: type={}, recipient={}, title={}", 
            notification.getType(), notification.getRecipient(), notification.getTitle());
        
        // 여기서 실제 알림 발송 로직 처리
        processNotification(notification);
    }

    /**
     * 사용자 등록 처리 로직
     */
    private void processUserRegistration(UserEventDto userEvent) {
        try {
            // 실제 처리 로직 (예시)
            log.info("✅ 사용자 등록 처리 완료: {}", userEvent.getEmail());
            
            // 예: 통계 업데이트, 외부 API 호출, 캐시 갱신 등
            
        } catch (Exception e) {
            log.error("❌ 사용자 등록 처리 실패: {}", e.getMessage());
        }
    }

    /**
     * 사용자 프로필 업데이트 처리 로직
     */
    private void processUserProfileUpdate(UserEventDto userEvent) {
        try {
            // 실제 처리 로직 (예시)
            log.info("✅ 프로필 업데이트 처리 완료: {}", userEvent.getNickname());
            
            // 예: 검색 인덱스 업데이트, 캐시 무효화 등
            
        } catch (Exception e) {
            log.error("❌ 프로필 업데이트 처리 실패: {}", e.getMessage());
        }
    }

    /**
     * 알림 처리 로직
     */
    private void processNotification(NotificationDto notification) {
        try {
            // 실제 알림 발송 로직 (예시)
            log.info("✅ 알림 발송 완료: {} -> {}", notification.getTitle(), notification.getRecipient());
            
            // 예: 이메일 발송, SMS 발송, 푸시 알림 등
            
        } catch (Exception e) {
            log.error("❌ 알림 발송 실패: {}", e.getMessage());
        }
    }
} 