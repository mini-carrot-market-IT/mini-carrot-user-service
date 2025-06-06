package com.minicarrot.user.controller;

import com.minicarrot.user.common.ApiResponse;
import com.minicarrot.user.common.Constants.ErrorMessage;
import com.minicarrot.user.common.Constants.SuccessMessage;
import com.minicarrot.user.dto.TokenResponse;
import com.minicarrot.user.dto.UserLoginRequest;
import com.minicarrot.user.dto.UserRegisterRequest;
import com.minicarrot.user.dto.UserResponse;
import com.minicarrot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        Map<String, String> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "mini-carrot-user-service");
        healthInfo.put("database", "MySQL on Naver Cloud");
        return ResponseEntity.ok(ApiResponse.success("서비스가 정상 작동중입니다.", healthInfo));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody UserRegisterRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());
        UserResponse response = userService.register(request);
        return ResponseEntity.ok(ApiResponse.success(SuccessMessage.REGISTER_SUCCESS, response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody UserLoginRequest request) {
        log.info("로그인 요청: email={}", request.getEmail());
        TokenResponse tokenResponse = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success(SuccessMessage.LOGIN_SUCCESS, tokenResponse));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@RequestHeader("Authorization") String token) {
        log.info("프로필 조회 요청");
        UserResponse response = userService.getProfile(token);
        return ResponseEntity.ok(ApiResponse.success(SuccessMessage.PROFILE_RETRIEVED, response));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationExceptions(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new HashMap<>();
        
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        log.warn("유효성 검증 실패: {}", fieldErrors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorMessage.VALIDATION_FAILED, fieldErrors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("요청 처리 중 오류 발생: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericException(Exception e) {
        log.error("서버 내부 오류: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorMessage.INTERNAL_SERVER_ERROR));
    }
} 