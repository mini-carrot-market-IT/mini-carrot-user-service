package com.minicarrot.user.service;

import com.minicarrot.user.dto.NotificationDto;
import com.minicarrot.user.dto.UserEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventPublisher {

    public void publishUserRegistrationEvent(UserEventDto userEvent) {
        try {
            log.info("사용자 등록 이벤트 발행: userId={}, email={}", userEvent.getUserId(), userEvent.getEmail());
            
            // RabbitMQ 대신 로깅으로 처리
            log.info("📤 [EVENT] USER_REGISTRATION: userId={}, email={}, nickname={}", 
                userEvent.getUserId(), userEvent.getEmail(), userEvent.getNickname());
            
            // 환영 알림 발송 (로깅)
            publishNotification(NotificationDto.createWelcomeNotification(
                userEvent.getUserId(), 
                userEvent.getEmail(), 
                userEvent.getNickname()
            ));
            
            log.info("사용자 등록 이벤트 발행 완료: userId={}", userEvent.getUserId());
            
        } catch (Exception e) {
            log.error("사용자 등록 이벤트 발행 실패: userId={}, error={}", userEvent.getUserId(), e.getMessage());
        }
    }

    public void publishUserProfileUpdateEvent(UserEventDto userEvent) {
        try {
            log.info("사용자 프로필 업데이트 이벤트 발행: userId={}, nickname={}", 
                userEvent.getUserId(), userEvent.getNickname());
            
            // RabbitMQ 대신 로깅으로 처리
            log.info("📤 [EVENT] USER_PROFILE_UPDATE: userId={}, email={}, nickname={}", 
                userEvent.getUserId(), userEvent.getEmail(), userEvent.getNickname());
            
            // 프로필 업데이트 알림 발송 (로깅)
            publishNotification(NotificationDto.createProfileUpdateNotification(
                userEvent.getUserId(), 
                userEvent.getEmail(), 
                userEvent.getNickname()
            ));
            
            log.info("사용자 프로필 업데이트 이벤트 발행 완료: userId={}", userEvent.getUserId());
            
        } catch (Exception e) {
            log.error("사용자 프로필 업데이트 이벤트 발행 실패: userId={}, error={}", 
                userEvent.getUserId(), e.getMessage());
        }
    }

    private void publishNotification(NotificationDto notification) {
        try {
            log.info("알림 발송: type={}, recipient={}, title={}", 
                notification.getType(), notification.getRecipient(), notification.getTitle());
            
            // RabbitMQ 대신 로깅으로 처리
            log.info("📤 [NOTIFICATION] type={}, recipient={}, title={}, message={}", 
                notification.getType(), notification.getRecipient(), 
                notification.getTitle(), notification.getMessage());
            
        } catch (Exception e) {
            log.error("알림 발송 실패: recipient={}, error={}", notification.getRecipient(), e.getMessage());
        }
    }
} 