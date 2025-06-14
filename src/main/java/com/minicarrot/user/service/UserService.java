package com.minicarrot.user.service;

import com.minicarrot.user.common.Constants.ErrorMessage;
import com.minicarrot.user.dto.TokenResponse;
import com.minicarrot.user.dto.UserEventDto;
import com.minicarrot.user.dto.UserLoginRequest;
import com.minicarrot.user.dto.UserRegisterRequest;
import com.minicarrot.user.dto.UserResponse;
import com.minicarrot.user.entity.User;
import com.minicarrot.user.repository.UserRepository;
import com.minicarrot.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserEventPublisher userEventPublisher;

    /**
     * 사용자 등록
     */
    @Transactional
    public UserResponse register(UserRegisterRequest request) {
        log.info("회원가입 시도: email={}, nickname={}", request.getEmail(), request.getNickname());
        
        // 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("이메일 중복: {}", request.getEmail());
            throw new IllegalArgumentException(ErrorMessage.EMAIL_ALREADY_EXISTS);
        }

        // 닉네임 중복 검사
        if (userRepository.existsByNickname(request.getNickname())) {
            log.warn("닉네임 중복: {}", request.getNickname());
            throw new IllegalArgumentException(ErrorMessage.NICKNAME_ALREADY_EXISTS);
        }

        // 정적 팩토리 메서드 사용으로 안전한 사용자 생성
        User user = User.createUser(
                request.getEmail(),
                request.getPassword(),
                request.getNickname(),
                passwordEncoder
        );

        User savedUser = userRepository.save(user);
        log.info("회원가입 성공: userId={}, email={}", savedUser.getUserId(), savedUser.getEmail());

        // 사용자 등록 이벤트 발행 (트랜잭션 커밋 후 처리)
        try {
            UserEventDto registrationEvent = UserEventDto.createRegistrationEvent(
                    savedUser.getUserId(),
                    savedUser.getEmail(),
                    savedUser.getNickname()
            );
            userEventPublisher.publishUserRegistrationEvent(registrationEvent);
        } catch (Exception e) {
            log.warn("사용자 등록 이벤트 발행 실패: userId={}, error={}", savedUser.getUserId(), e.getMessage());
            // 이벤트 발행 실패는 회원가입 자체를 실패시키지 않음
        }

        return UserResponse.builder()
                .userId(savedUser.getUserId())
                .email(savedUser.getEmail())
                .nickname(savedUser.getNickname())
                .build();
    }



    @Transactional(readOnly = true)
    public TokenResponse login(UserLoginRequest request) {
        log.info("로그인 시도: email={}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자: {}", request.getEmail());
                    return new IllegalArgumentException(ErrorMessage.USER_NOT_FOUND);
                });

        // 엔티티의 도메인 메서드 사용
        if (!user.isPasswordMatched(request.getPassword(), passwordEncoder)) {
            log.warn("비밀번호 불일치: email={}", request.getEmail());
            throw new IllegalArgumentException(ErrorMessage.INVALID_PASSWORD);
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getUserId(), user.getNickname());
        log.info("로그인 성공: userId={}, email={}", user.getUserId(), user.getEmail());
        
        // TokenResponse에 토큰과 사용자 정보 모두 포함
        UserResponse userResponse = UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
        
        return TokenResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }



    @Transactional(readOnly = true)
    public UserResponse getProfile(String token) {
        // Bearer 토큰에서 실제 토큰 추출
        String actualToken = token.replace("Bearer ", "");
        
        if (!jwtUtil.isTokenValid(actualToken)) {
            log.warn("유효하지 않은 토큰");
            throw new IllegalArgumentException(ErrorMessage.INVALID_TOKEN);
        }

        String email = jwtUtil.getEmailFromToken(actualToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("토큰의 사용자를 찾을 수 없음: email={}", email);
                    return new IllegalArgumentException(ErrorMessage.USER_NOT_FOUND);
                });

        log.info("프로필 조회: userId={}, email={}", user.getUserId(), user.getEmail());

        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    // 닉네임 변경 메서드 추가
    @Transactional
    public UserResponse changeNickname(String token, String newNickname) {
        String actualToken = token.replace("Bearer ", "");
        
        if (!jwtUtil.isTokenValid(actualToken)) {
            throw new IllegalArgumentException(ErrorMessage.INVALID_TOKEN);
        }

        String email = jwtUtil.getEmailFromToken(actualToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessage.USER_NOT_FOUND));

        // 닉네임 중복 검사
        if (!user.getNickname().equals(newNickname) && userRepository.existsByNickname(newNickname)) {
            throw new IllegalArgumentException(ErrorMessage.NICKNAME_ALREADY_EXISTS);
        }

        // 이전 닉네임 저장
        String previousNickname = user.getNickname();
        
        // 엔티티의 도메인 메서드 사용
        user.changeNickname(newNickname);
        
        // 프로필 업데이트 이벤트 발행
        try {
            UserEventDto updateEvent = UserEventDto.createProfileUpdateEvent(
                    user.getUserId(),
                    user.getEmail(),
                    newNickname,
                    previousNickname
            );
            userEventPublisher.publishUserProfileUpdateEvent(updateEvent);
        } catch (Exception e) {
            log.warn("프로필 업데이트 이벤트 발행 실패: userId={}, error={}", user.getUserId(), e.getMessage());
        }
        
        log.info("닉네임 변경 성공: userId={}, newNickname={}", user.getUserId(), newNickname);

        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }



    // 비밀번호 변경 메서드 추가
    @Transactional
    public void changePassword(String token, String currentPassword, String newPassword) {
        String actualToken = token.replace("Bearer ", "");
        
        if (!jwtUtil.isTokenValid(actualToken)) {
            throw new IllegalArgumentException(ErrorMessage.INVALID_TOKEN);
        }

        String email = jwtUtil.getEmailFromToken(actualToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessage.USER_NOT_FOUND));

        // 엔티티의 도메인 메서드 사용
        user.changePassword(currentPassword, newPassword, passwordEncoder);
        
        log.info("비밀번호 변경 성공: userId={}", user.getUserId());
    }

    /**
     * 모든 사용자 조회
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("모든 사용자 조회 요청");
        
        List<User> users = userRepository.findAll();
        log.info("사용자 조회 완료: 총 {}명", users.size());
        
        return users.stream()
                .map(user -> UserResponse.builder()
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .build())
                .collect(Collectors.toList());
    }



    // 사용자 삭제 메서드 추가
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessage.USER_NOT_FOUND));
        
        userRepository.delete(user);
        log.info("사용자 삭제 성공: userId={}", userId);
    }

    // 사용자 검색 메서드 추가
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String keyword) {
        log.info("사용자 검색: keyword={}", keyword);
        
        List<User> users = userRepository.findByEmailContainingOrNicknameContaining(keyword, keyword);
        return users.stream()
                .map(user -> UserResponse.builder()
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .build())
                .collect(Collectors.toList());
    }
} 