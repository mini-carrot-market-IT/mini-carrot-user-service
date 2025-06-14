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
public class UserEventDto {
    
    private String eventType; // REGISTRATION, PROFILE_UPDATE, PASSWORD_CHANGE
    private Long userId;
    private String email;
    private String nickname;
    private String previousNickname;
    private LocalDateTime eventTime;
    private String source; // USER_SERVICE
    
    public static UserEventDto createRegistrationEvent(Long userId, String email, String nickname) {
        return UserEventDto.builder()
                .eventType("REGISTRATION")
                .userId(userId)
                .email(email)
                .nickname(nickname)
                .eventTime(LocalDateTime.now())
                .source("USER_SERVICE")
                .build();
    }
    
    public static UserEventDto createProfileUpdateEvent(Long userId, String email, String nickname, String previousNickname) {
        return UserEventDto.builder()
                .eventType("PROFILE_UPDATE")
                .userId(userId)
                .email(email)
                .nickname(nickname)
                .previousNickname(previousNickname)
                .eventTime(LocalDateTime.now())
                .source("USER_SERVICE")
                .build();
    }
} 