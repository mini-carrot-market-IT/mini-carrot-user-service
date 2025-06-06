# 🥕 Mini 당근마켓 User Service

Spring Boot 기반의 사용자 관리 마이크로서비스입니다.

## 📋 목차

- [기능 소개](#-기능-소개)
- [기술 스택](#-기술-스택)
- [시작하기](#-시작하기)
- [환경 설정](#-환경-설정)
- [API 문서](#-api-문서)
- [Docker 배포](#-docker-배포)
- [개발 가이드](#-개발-가이드)
- [보안 설정](#-보안-설정)

## 🚀 기능 소개

- **회원가입**: 이메일, 비밀번호, 닉네임으로 회원가입
- **로그인**: JWT 토큰 기반 인증
- **프로필 조회**: 사용자 정보 조회
- **헬스체크**: 서비스 상태 확인
- **비밀번호 암호화**: BCrypt 사용
- **데이터 검증**: Spring Validation 적용

## 🛠 기술 스택

- **Framework**: Spring Boot 3.5.0
- **Language**: Java 17
- **Database**: MySQL 8.0
- **Security**: Spring Security + JWT
- **Build Tool**: Gradle
- **Container**: Docker
- **Architecture**: Microservice

## 🏃‍♂️ 시작하기

### 1. 저장소 클론

```bash
git clone https://github.com/YOUR_USERNAME/mini-carrot-user-service.git
cd mini-carrot-user-service
```

### 2. 환경 설정

```bash
# 환경변수 파일 복사
cp env.example .env

# .env 파일을 열어서 실제 값으로 수정
vi .env
```

### 3. 애플리케이션 실행

```bash
# 권한 부여 (macOS/Linux)
chmod +x gradlew

# 애플리케이션 실행
./gradlew bootRun
```

애플리케이션이 `http://localhost:8080`에서 실행됩니다.

## ⚙️ 환경 설정

### 데이터베이스 설정

MySQL 데이터베이스가 필요합니다:

```sql
-- 데이터베이스 생성
CREATE DATABASE minicarrot;

-- 사용자 테이블은 자동으로 생성됩니다 (JPA DDL Auto)
```

### 환경변수 설정

`.env` 파일에서 다음 값들을 설정해야 합니다:

```env
# 필수 설정
DB_URL=jdbc:mysql://localhost:3306/minicarrot?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
DB_USERNAME=your_username
DB_PASSWORD=your_password
JWT_SECRET=your-super-secret-key-at-least-32-characters-long

# 선택 설정
JPA_DDL_AUTO=update
LOG_LEVEL=INFO
```

## 📚 API 문서

### 헬스체크

```http
GET /api/users/health
```

**응답:**
```json
{
  "success": true,
  "message": "서비스가 정상 작동중입니다.",
  "data": {
    "status": "UP",
    "service": "mini-carrot-user-service",
    "database": "MySQL"
  }
}
```

### 회원가입

```http
POST /api/users/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "사용자"
}
```

**응답:**
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "사용자"
  }
}
```

### 로그인

```http
POST /api/users/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**응답:**
```json
{
  "success": true,
  "message": "로그인이 완료되었습니다.",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "user": {
      "userId": 1,
      "email": "user@example.com",
      "nickname": "사용자"
    }
  }
}
```

### 프로필 조회

```http
GET /api/users/profile
Authorization: Bearer YOUR_JWT_TOKEN
```

**응답:**
```json
{
  "success": true,
  "message": "프로필 조회가 완료되었습니다.",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "사용자"
  }
}
```

## 🐳 Docker 배포

### Docker 이미지 빌드

```bash
# JAR 파일 빌드
./gradlew bootJar

# Docker 이미지 빌드
docker build -t mini-carrot-user-service .
```

### Docker 실행

```bash
docker run -d \
  --name user-service \
  -p 8080:8080 \
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/minicarrot?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul" \
  -e DB_USERNAME="root" \
  -e DB_PASSWORD="password" \
  -e JWT_SECRET="your-super-secret-key-at-least-32-characters-long" \
  mini-carrot-user-service
```

## 👥 개발 가이드

### 개발 환경 설정

1. **Java 17** 설치
2. **MySQL 8.0** 설치 및 실행
3. **환경변수** 설정 (`.env` 파일)

### 빌드 및 테스트

```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 코드 품질 검사
./gradlew check
```

### 개발 서버 실행

```bash
# 개발 모드로 실행 (hot reload)
./gradlew bootRun --args='--spring.profiles.active=local'
```

## 🔒 보안 설정

### JWT Secret

- **최소 32자 이상**의 강력한 비밀키 사용
- 운영환경에서는 환경변수로 관리
- 주기적으로 교체 권장

### 데이터베이스

- 운영환경에서는 **별도 DB 사용자** 생성
- **최소 권한 원칙** 적용
- 연결 정보는 **환경변수**로 관리

### 파일 보안

다음 파일들은 **절대 Git에 커밋하지 마세요**:

- `.env`
- `application-local.yml`
- `application-prod.yml`
- 실제 배포 설정 파일들

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 있습니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 📞 문의

프로젝트 관련 문의사항이 있으시면 이슈를 등록해 주세요.

---

**Mini 당근마켓 User Service** - 안전하고 확장 가능한 사용자 관리 서비스 🥕

