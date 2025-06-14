package com.minicarrot.user.controller;

import com.minicarrot.user.common.ApiResponse;
import com.minicarrot.user.common.Constants.ErrorMessage;
import com.minicarrot.user.common.Constants.SuccessMessage;
import com.minicarrot.user.dto.TokenResponse;
import com.minicarrot.user.dto.UserLoginRequest;
import com.minicarrot.user.dto.UserRegisterRequest;
import com.minicarrot.user.dto.UserResponse;
import com.minicarrot.user.dto.NicknameChangeRequest;
import com.minicarrot.user.dto.PasswordChangeRequest;
import com.minicarrot.user.dto.ProductStatsDto;
import com.minicarrot.user.service.AsyncDashboardService;
import com.minicarrot.user.service.UserService;
import com.minicarrot.user.service.ProductServiceClient;
import com.minicarrot.user.service.JwtCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ProductServiceClient productServiceClient;
    private final JwtCacheService jwtCacheService;
    private final AsyncDashboardService asyncDashboardService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        Map<String, String> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "mini-carrot-user-service");
        healthInfo.put("database", "MySQL on Naver Cloud");
        
        // 캐시 통계 추가
        JwtCacheService.CacheStats cacheStats = jwtCacheService.getCacheStats();
        healthInfo.put("cache_total", String.valueOf(cacheStats.getTotalEntries()));
        healthInfo.put("cache_active", String.valueOf(cacheStats.getActiveEntries()));
        
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
        
        // 캐시된 토큰 검증 사용
        UserResponse response = jwtCacheService.getUserFromToken(token);
        return ResponseEntity.ok(ApiResponse.success(SuccessMessage.PROFILE_RETRIEVED, response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        log.info("모든 사용자 조회 요청");
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("사용자 목록 조회가 완료되었습니다.", users));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(@RequestParam String keyword) {
        log.info("사용자 검색 요청: keyword={}", keyword);
        List<UserResponse> users = userService.searchUsers(keyword);
        return ResponseEntity.ok(ApiResponse.success("사용자 검색이 완료되었습니다.", users));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        log.info("사용자 삭제 요청: userId={}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자가 삭제되었습니다.", null));
    }

    @PutMapping("/nickname")
    public ResponseEntity<ApiResponse<UserResponse>> changeNickname(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody NicknameChangeRequest request) {
        log.info("닉네임 변경 요청: newNickname={}", request.getNewNickname());
        
        // 캐시된 토큰 검증으로 사용자 정보 조회
        UserResponse currentUser = jwtCacheService.getUserFromToken(token);
        
        // 실제 닉네임 변경은 UserService에서 처리 (DB 업데이트 필요)
        UserResponse response = userService.changeNickname(token, request.getNewNickname());
        
        // 캐시 무효화 (닉네임이 변경되었으므로)
        jwtCacheService.invalidateUserCache(currentUser.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success("닉네임이 변경되었습니다.", response));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody PasswordChangeRequest request) {
        log.info("비밀번호 변경 요청");
        
        // 캐시된 토큰 검증으로 사용자 정보 조회
        UserResponse currentUser = jwtCacheService.getUserFromToken(token);
        
        // 실제 비밀번호 변경은 UserService에서 처리 (DB 업데이트 및 현재 비밀번호 검증 필요)
        userService.changePassword(token, request.getCurrentPassword(), request.getNewPassword());
        
        // 캐시 무효화 (보안상 비밀번호 변경 시 캐시 제거)
        jwtCacheService.invalidateUserCache(currentUser.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다.", null));
    }

    // 마이페이지 관련 API들 - 성능 최적화 적용
    @GetMapping("/products/mine")
    public ResponseEntity<ApiResponse<Object>> getMyProducts(@RequestHeader("Authorization") String token) {
        long startTime = System.currentTimeMillis();
        log.info("내가 등록한 상품 조회 요청");
        
        try {
            // 🚀 캐시된 토큰 검증 사용 (DB 조회 없이 토큰에서 직접 추출)
            UserResponse profile = jwtCacheService.getUserFromToken(token);
            
            List<Map<String, Object>> products = productServiceClient.getUserProducts(profile.getUserId());
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("내 상품 조회 성공: userId={}, 상품수={}개, 소요시간={}ms", 
                profile.getUserId(), products.size(), duration);
            
            return ResponseEntity.ok(ApiResponse.success("내가 등록한 상품 목록입니다.", 
                Map.of("products", products, "count", products.size(), "responseTime", duration + "ms")));
                
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("내 상품 조회 실패: error={}, 소요시간={}ms", e.getMessage(), duration);
            return ResponseEntity.ok(ApiResponse.success("내가 등록한 상품 목록입니다.", 
                Map.of("products", List.of(), "count", 0, "error", "상품 목록을 불러올 수 없습니다.", "responseTime", duration + "ms")));
        }
    }

    @GetMapping("/products/purchased")
    public ResponseEntity<ApiResponse<Object>> getPurchasedProducts(@RequestHeader("Authorization") String token) {
        long startTime = System.currentTimeMillis();
        log.info("구매한 상품 조회 요청");
        
        try {
            // 🚀 캐시된 토큰 검증 사용 (DB 조회 없이 토큰에서 직접 추출)
            UserResponse profile = jwtCacheService.getUserFromToken(token);
            
            List<Map<String, Object>> products = productServiceClient.getUserPurchases(profile.getUserId());
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("구매 상품 조회 성공: userId={}, 상품수={}개, 소요시간={}ms", 
                profile.getUserId(), products.size(), duration);
            
            return ResponseEntity.ok(ApiResponse.success("구매한 상품 목록입니다.", 
                Map.of("products", products, "count", products.size(), "responseTime", duration + "ms")));
                
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("구매 상품 조회 실패: error={}, 소요시간={}ms", e.getMessage(), duration);
            return ResponseEntity.ok(ApiResponse.success("구매한 상품 목록입니다.", 
                Map.of("products", List.of(), "count", 0, "error", "구매 목록을 불러올 수 없습니다.", "responseTime", duration + "ms")));
        }
    }

    @GetMapping("/products/liked")
    public ResponseEntity<ApiResponse<Object>> getLikedProducts(@RequestHeader("Authorization") String token) {
        long startTime = System.currentTimeMillis();
        log.info("찜한 상품 조회 요청");
        
        try {
            // 🚀 캐시된 토큰 검증 사용
            UserResponse profile = jwtCacheService.getUserFromToken(token);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("찜한 상품 조회 완료: userId={}, 소요시간={}ms", profile.getUserId(), duration);
            
            // 찜 기능은 현재 구현되지 않음 (사용자 요청에 따라 제거)
            return ResponseEntity.ok(ApiResponse.success("찜 기능은 현재 지원하지 않습니다.", 
                Map.of("products", List.of(), "count", 0, "message", "찜 기능은 추후 업데이트 예정입니다.", "responseTime", duration + "ms")));
                
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("찜한 상품 조회 실패: error={}, 소요시간={}ms", e.getMessage(), duration);
            return ResponseEntity.ok(ApiResponse.success("찜 기능은 현재 지원하지 않습니다.", 
                Map.of("products", List.of(), "count", 0, "error", "토큰 검증 실패", "responseTime", duration + "ms")));
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Object>> getUserDashboard(@RequestHeader("Authorization") String token) {
        long startTime = System.currentTimeMillis();
        log.info("🚀 사용자 대시보드 조회 요청 (빠른 응답 모드)");
        
        try {
            // 🚀 캐시된 토큰 검증 사용 (DB 조회 없이 토큰에서 직접 추출)
            UserResponse profile = jwtCacheService.getUserFromToken(token);
            
            // 🚀 비동기 대시보드 서비스 사용 (즉시 응답 + 백그라운드 로딩)
            Map<String, Object> dashboardData = asyncDashboardService.getDashboardData(profile.getUserId());
            
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> dashboard = Map.of(
                "profile", profile,
                "data", dashboardData,
                "lastUpdated", System.currentTimeMillis(),
                "responseTime", duration + "ms"
            );
            
            log.info("✅ 대시보드 조회 성공 (즉시 응답): userId={}, 소요시간={}ms", 
                profile.getUserId(), duration);
            
            return ResponseEntity.ok(ApiResponse.success("사용자 대시보드 정보입니다.", dashboard));
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ 대시보드 조회 실패: error={}, 소요시간={}ms", e.getMessage(), duration);
            
            try {
                // 에러 발생 시에도 캐시된 토큰 검증 사용
                UserResponse profile = jwtCacheService.getUserFromToken(token);
                Map<String, Object> dashboard = Map.of(
                    "profile", profile,
                    "data", Map.of(
                        "stats", Map.of(
                            "registeredProducts", 0,
                            "purchasedProducts", 0,
                            "soldProducts", 0,
                            "totalTransactions", 0,
                            "totalSales", 0.0,
                            "totalPurchases", 0.0
                        ),
                        "recentProducts", List.of(),
                        "recentActivity", List.of(),
                        "loading", false,
                        "error", "일시적으로 통계를 불러올 수 없습니다. 잠시 후 다시 시도해주세요."
                    ),
                    "responseTime", duration + "ms"
                );
                
                return ResponseEntity.ok(ApiResponse.success("사용자 대시보드 정보입니다.", dashboard));
                
            } catch (Exception tokenError) {
                log.error("토큰 검증도 실패: error={}", tokenError.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body((ApiResponse<Object>) ApiResponse.error("토큰이 유효하지 않습니다."));
            }
        }
    }

    @GetMapping("/dashboard/refresh")
    public ResponseEntity<ApiResponse<Object>> refreshDashboard(@RequestHeader("Authorization") String token) {
        log.info("🔄 대시보드 새로고침 요청");
        
        try {
            UserResponse profile = jwtCacheService.getUserFromToken(token);
            
            // 캐시된 데이터 조회
            Map<String, Object> cachedData = asyncDashboardService.getCachedDashboardData(profile.getUserId());
            
            if (cachedData != null) {
                Map<String, Object> dashboard = Map.of(
                    "profile", profile,
                    "data", cachedData,
                    "lastUpdated", System.currentTimeMillis(),
                    "cached", true
                );
                
                log.info("✅ 대시보드 캐시 데이터 반환: userId={}", profile.getUserId());
                return ResponseEntity.ok(ApiResponse.success("대시보드 데이터가 업데이트되었습니다.", dashboard));
            } else {
                // 캐시 무효화 후 새로 로드
                asyncDashboardService.invalidateDashboardCache(profile.getUserId());
                return getUserDashboard(token);
            }
            
        } catch (Exception e) {
            log.error("대시보드 새로고침 실패: error={}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body((ApiResponse<Object>) ApiResponse.error("대시보드 새로고침에 실패했습니다."));
        }
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