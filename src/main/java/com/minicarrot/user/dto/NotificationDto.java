package com.minicarrot.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    
    private String type; // PUSH, EMAIL, SMS
    private String recipient; // 수신자 (이메일 또는 전화번호)
    private Long userId;
    private String title;
    private String message;
    private String priority; // HIGH, MEDIUM, LOW
    private LocalDateTime createdAt;
    private String channel; // FCM, SENDGRID, AWS_SES
    
    public static NotificationDto createWelcomeNotification(Long userId, String email, String nickname) {
        return NotificationDto.builder()
                .type("EMAIL")
                .recipient(email)
                .userId(userId)
                .title("미니 당근에 오신 것을 환영합니다!")
                .message(String.format("%s님, 미니 당근 회원가입을 축하드립니다! 이제 다양한 중고거래를 시작해보세요.", nickname))
                .priority("MEDIUM")
                .createdAt(LocalDateTime.now())
                .channel("SENDGRID")
                .build();
    }
    
    public static NotificationDto createProfileUpdateNotification(Long userId, String email, String nickname) {
        return NotificationDto.builder()
                .type("PUSH")
                .recipient(email)
                .userId(userId)
                .title("프로필이 업데이트되었습니다")
                .message(String.format("%s님의 프로필 정보가 성공적으로 업데이트되었습니다.", nickname))
                .priority("LOW")
                .createdAt(LocalDateTime.now())
                .channel("FCM")
                .build();
    }
} 