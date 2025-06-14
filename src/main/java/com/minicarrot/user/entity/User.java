package com.minicarrot.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Entity
@Table(name = "carrot_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = "password")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false, length = 50)
    private String nickname;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // 정적 팩토리 메서드 - 생성 로직 캡슐화
    public static User createUser(String email, String rawPassword, String nickname, PasswordEncoder passwordEncoder) {
        validateEmail(email);
        validatePassword(rawPassword);
        validateNickname(nickname);
        
        User user = new User();
        user.email = email;
        user.password = passwordEncoder.encode(rawPassword);
        user.nickname = nickname;
        return user;
    }

    // 비밀번호 변경 - 비즈니스 로직 포함
    public void changePassword(String currentPassword, String newPassword, PasswordEncoder passwordEncoder) {
        if (!passwordEncoder.matches(currentPassword, this.password)) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        
        validatePassword(newPassword);
        this.password = passwordEncoder.encode(newPassword);
    }

    // 닉네임 변경 - 유효성 검사 포함
    public void changeNickname(String newNickname) {
        validateNickname(newNickname);
        this.nickname = newNickname;
    }

    // 비밀번호 확인
    public boolean isPasswordMatched(String rawPassword, PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(rawPassword, this.password);
    }

    // 유효성 검사 메서드들
    private static void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 20) {
            throw new IllegalArgumentException("비밀번호는 6자 이상 20자 이하여야 합니다.");
        }
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }
        if (nickname.length() < 2 || nickname.length() > 10) {
            throw new IllegalArgumentException("닉네임은 2자 이상 10자 이하여야 합니다.");
        }
    }
} 