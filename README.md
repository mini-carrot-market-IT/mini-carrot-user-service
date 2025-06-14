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

### 💳 사용자 관리
- **회원가입/로그인**: JWT 토큰 기반 인증
- **프로필 관리**: 닉네임, 비밀번호 변경
- **사용자 검색**: 이메일, 닉네임으로 검색
- **사용자 목록**: 전체 사용자 조회 및 삭제
- **실시간 이벤트**: 회원가입, 프로필 업데이트 이벤트 발행

### 🔐 보안 기능
- **JWT 인증**: 안전한 토큰 기반 인증
- **비밀번호 암호화**: BCrypt 해싱
- **입력 유효성 검사**: Bean Validation
- **CORS 설정**: 크로스 오리진 요청 지원

### 🔄 메시지 큐 시스템
- **RabbitMQ 연동**: 비동기 메시지 처리
- **이벤트 발행**: 사용자 등록/업데이트 이벤트
- **알림 시스템**: 환영 메시지, 프로필 업데이트 알림
- **Dead Letter Queue**: 실패 메시지 처리

### 🌐 외부 API 연동
- **WebClient**: 비동기 HTTP 클라이언트
- **Product 서비스 연동**: 마이크로서비스 통신
- **외부 API 호출**: JSONPlaceholder API 연동
- **푸시 알림**: FCM, APNS 연동 준비

### 📊 모니터링 시스템
- **Prometheus**: 메트릭 수집
- **Grafana**: 실시간 대시보드  
- **Elasticsearch + Kibana**: 로그 분석
- **Actuator**: Spring Boot 모니터링

## 🛠 기술 스택

### 🔧 Core
- **Framework**: Spring Boot 3.5.0
- **Language**: Java 17
- **Build Tool**: Gradle
- **Architecture**: Microservice

### 💾 Database & Messaging
- **Database**: MySQL 8.0
- **Message Queue**: RabbitMQ 3.12
- **Cache**: Redis (연동 준비)

### 🔐 Security & Auth
- **Security**: Spring Security + JWT
- **Password**: BCrypt Hashing
- **Validation**: Bean Validation

### 🌐 Communication
- **HTTP Client**: WebClient (Reactive)
- **REST API**: Spring Web MVC
- **Serialization**: Jackson JSON

### 📊 Monitoring & Observability
- **Metrics**: Prometheus + Micrometer
- **Visualization**: Grafana
- **Logging**: Logback + ELK Stack
- **Health Check**: Spring Actuator

### 🐳 DevOps
- **Container**: Docker & Docker Compose
- **Orchestration**: Kubernetes (NCP)
- **Registry**: Naver Container Registry
- **CI/CD**: GitHub Actions (준비)

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

### 3. 환경별 실행 방법

#### 🐳 로컬 개발 환경 (Docker Compose 권장)

```bash
# 1. 전체 인프라 시작 (MySQL, RabbitMQ, 모니터링)
docker-compose -f docker-compose-messagequeue.yml up -d

# 2. Spring Boot 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 3. 서비스 접속 확인
curl http://localhost:8080/actuator/health
```

#### ☁️ NCP 클러스터 배포

```bash
# 1. Secrets 생성 (한 번만 실행)
kubectl apply -f k8s/create-secrets.yaml

# 2. User Service 배포
kubectl apply -f k8s/user-service-deployment.yaml

# 3. 배포 상태 확인
kubectl get pods -n tuk-trainee12 -l app=user-service
kubectl get svc -n tuk-trainee12 user-service-external
```

## ⚙️ 데이터베이스 연결 정보

### 🔗 연결 방식별 설정

| 환경 | 호스트 | 포트 | 데이터베이스 | 사용자 | 비밀번호 |
|------|--------|------|-------------|--------|----------|
| **로컬 개발** | `localhost` | `3306` | `minicarrot` | `minicarrot` | `password123` |
| **NCP 내부** | `mysql-service.tuk-trainee12.svc.cluster.local` | `3306` | `mini_carrot_user` | `carrot_user` | `CarrotPass#2024` |
| **NCP 외부** | `[노드IP]` | `31206` | `mini_carrot_user` | `carrot_user` | `CarrotPass#2024` |

### 🛠️ 데이터베이스 초기화

```sql
-- 테이블은 자동으로 생성됩니다 (JPA DDL Auto: update)
-- 초기 테스트 데이터도 포함됩니다

-- 테스트 계정:
-- 이메일: admin@minicarrot.com
-- 비밀번호: password123
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

