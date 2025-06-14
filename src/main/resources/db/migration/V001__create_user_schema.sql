-- =====================================================
-- Mini Carrot User Service - Database Schema
-- =====================================================

-- 데이터베이스 생성 (필요한 경우)
CREATE DATABASE IF NOT EXISTS mini_carrot_user
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE mini_carrot_user;

-- =====================================================
-- 사용자 테이블 생성
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 인덱스 생성
    INDEX idx_email (email),
    INDEX idx_nickname (nickname),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 사용자 이벤트 로그 테이블 (선택적)
-- =====================================================
CREATE TABLE IF NOT EXISTS user_events (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    event_type VARCHAR(50) NOT NULL,
    event_data JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 외래키 설정
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- 인덱스 생성
    INDEX idx_user_id (user_id),
    INDEX idx_event_type (event_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 초기 테스트 데이터 (개발용)
-- =====================================================
INSERT IGNORE INTO users (email, password, nickname) VALUES
-- 비밀번호: password123 (BCrypt 해시)
('admin@minicarrot.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1VxYfCLZ8fK8GfYP3k8iQ6YYO5t4S4O', '관리자'),
('user1@minicarrot.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1VxYfCLZ8fK8GfYP3k8iQ6YYO5t4S4O', '사용자1'),
('user2@minicarrot.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1VxYfCLZ8fK8GfYP3k8iQ6YYO5t4S4O', '사용자2');

-- =====================================================
-- 성능 최적화를 위한 추가 설정
-- =====================================================

-- 사용자 테이블 통계 업데이트
ANALYZE TABLE users;
ANALYZE TABLE user_events; 