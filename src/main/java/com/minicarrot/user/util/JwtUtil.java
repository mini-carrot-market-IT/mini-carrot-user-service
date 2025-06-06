package com.minicarrot.user.util;

import com.minicarrot.user.common.Constants.ErrorMessage;
import com.minicarrot.user.common.Constants.JwtConstants;
import com.minicarrot.user.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    public String generateToken(String email, Long userId, String nickname) {
        try {
            String token = Jwts.builder()
                    .subject(email)
                    .claim(JwtConstants.USER_ID_CLAIM, userId)
                    .claim(JwtConstants.NICKNAME_CLAIM, nickname)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                    .signWith(getSigningKey())
                    .compact();
            
            log.debug("JWT 토큰 생성 성공: email={}", email);
            return token;
        } catch (Exception e) {
            log.error("JWT 토큰 생성 실패: email={}, error={}", email, e.getMessage());
            throw new RuntimeException(ErrorMessage.TOKEN_GENERATION_FAILED, e);
        }
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        return getClaims(token).get(JwtConstants.USER_ID_CLAIM, Long.class);
    }

    public String getNicknameFromToken(String token) {
        return getClaims(token).get(JwtConstants.NICKNAME_CLAIM, String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("유효하지 않은 JWT 토큰: {}", e.getMessage());
            return false;
        }
    }

    private Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.error("JWT 토큰 파싱 실패: {}", e.getMessage());
            throw e;
        }
    }
} 