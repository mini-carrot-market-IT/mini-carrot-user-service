# Mini 당근마켓 User Service API 문서

## 개요
사용자 관리를 위한 마이크로서비스입니다.

## Base URL
```
http://localhost:8080
```

## API 엔드포인트

### 1. 회원가입
- **URL**: `POST /api/users/register`
- **설명**: 새로운 사용자를 등록합니다.
- **요청 본문**:
```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "사용자닉네임"
}
```
- **응답**:
```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "사용자닉네임"
}
```

### 2. 로그인
- **URL**: `POST /api/users/login`
- **설명**: 사용자 로그인을 처리하고 JWT 토큰을 반환합니다.
- **요청 본문**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```
- **응답**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 3. 프로필 조회
- **URL**: `GET /api/users/profile`
- **설명**: 현재 로그인한 사용자의 프로필 정보를 조회합니다.
- **헤더**: `Authorization: Bearer {JWT_TOKEN}`
- **응답**:
```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "사용자닉네임"
}
```

## 에러 응답
모든 API는 오류 발생 시 다음과 같은 형태로 응답합니다:
```json
{
  "error": "에러 메시지"
}
```

## 상태 코드
- `200 OK`: 성공
- `400 Bad Request`: 잘못된 요청
- `401 Unauthorized`: 인증 실패
- `500 Internal Server Error`: 서버 오류
