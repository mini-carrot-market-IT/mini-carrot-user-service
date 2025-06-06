# 🔧 프로젝트 설정 가이드

친구들이 이 프로젝트를 쉽게 설정하고 실행할 수 있도록 단계별로 안내합니다.

## 📋 필수 준비사항

### 1. 개발 환경
- **Java 17** 설치 (OpenJDK 권장)
- **MySQL 8.0** 설치 및 실행
- **Git** 설치
- **IDE** (IntelliJ IDEA, VSCode 등)

### 2. MySQL 설치 방법

#### macOS (Homebrew 사용)
```bash
# MySQL 설치
brew install mysql

# MySQL 서비스 시작
brew services start mysql

# MySQL 접속 (기본 root 계정)
mysql -u root -p
```

#### Windows
1. [MySQL 공식 웹사이트](https://dev.mysql.com/downloads/mysql/)에서 다운로드
2. MySQL Installer 실행
3. MySQL Server 8.0 설치
4. MySQL Workbench 설치 (선택사항)

#### Ubuntu/Debian
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
sudo mysql_secure_installation
```

## 🚀 프로젝트 설정

### 1. 저장소 클론
```bash
git clone https://github.com/YOUR_USERNAME/mini-carrot-user-service.git
cd mini-carrot-user-service
```

### 2. 데이터베이스 설정
```sql
-- MySQL에 접속하여 데이터베이스 생성
mysql -u root -p

-- 데이터베이스 생성
CREATE DATABASE minicarrot;

-- 사용자 생성 (선택사항)
CREATE USER 'minicarrot'@'localhost' IDENTIFIED BY 'password123';
GRANT ALL PRIVILEGES ON minicarrot.* TO 'minicarrot'@'localhost';
FLUSH PRIVILEGES;

-- 확인
SHOW DATABASES;
USE minicarrot;
```

### 3. 환경변수 설정
```bash
# 환경변수 템플릿 복사
cp env.example .env

# .env 파일 편집
# macOS/Linux
nano .env
# 또는
vi .env

# Windows
notepad .env
```

### 4. .env 파일 설정 예시
```env
# 데이터베이스 설정
DB_URL=jdbc:mysql://localhost:3306/minicarrot?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# JWT 설정 (32자 이상의 강력한 키로 변경)
JWT_SECRET=mini-carrot-super-secret-key-change-this-in-production
JWT_EXPIRATION=86400000

# JPA 설정
JPA_DDL_AUTO=update
JPA_SHOW_SQL=true

# 로깅 레벨
LOG_LEVEL=INFO
LOG_LEVEL_SECURITY=WARN

# 애플리케이션 포트
SERVER_PORT=8080
```

### 5. 실행 권한 부여 (macOS/Linux)
```bash
chmod +x gradlew
```

### 6. 애플리케이션 실행
```bash
# 빌드 및 실행
./gradlew bootRun

# 또는 빌드 후 실행
./gradlew build
java -jar build/libs/mini-carrot-user-service-0.0.1-SNAPSHOT.jar
```

## 🧪 동작 확인

### 1. 헬스체크
```bash
curl http://localhost:8080/api/users/health
```

**성공 응답:**
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

### 2. 회원가입 테스트
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "nickname": "테스트유저"
  }'
```

### 3. 로그인 테스트
```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

## 🔧 IDE 설정

### IntelliJ IDEA
1. **File > Open** → 프로젝트 폴더 선택
2. **Gradle** 프로젝트로 인식되면 자동으로 의존성 다운로드
3. **Run Configuration** 설정:
   - Main Class: `com.minicarrot.user.UserServiceApplication`
   - Environment Variables: `.env` 파일의 내용 추가

### VS Code
1. **Extensions** 설치:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - MySQL Extension (선택사항)
2. **Terminal**에서 `./gradlew bootRun` 실행

## 🐳 Docker로 실행 (선택사항)

### 1. Docker 이미지 빌드
```bash
# JAR 파일 빌드
./gradlew bootJar

# Docker 이미지 빌드
docker build -t mini-carrot-user-service .
```

### 2. Docker Compose 사용
```yaml
# docker-compose.yml 파일 생성
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: minicarrot
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
  
  user-service:
    build: .
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:mysql://mysql:3306/minicarrot?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
      DB_USERNAME: root
      DB_PASSWORD: rootpassword
      JWT_SECRET: mini-carrot-super-secret-key-change-this-in-production
    depends_on:
      - mysql

volumes:
  mysql_data:
```

```bash
# Docker Compose 실행
docker-compose up -d
```

## 🚨 문제 해결

### MySQL 연결 오류
```
Access denied for user 'root'@'localhost'
```
**해결방법:**
1. MySQL 비밀번호 확인
2. `.env` 파일의 `DB_PASSWORD` 수정

### Port 이미 사용 중 오류
```
Port 8080 was already in use
```
**해결방법:**
1. 다른 애플리케이션 종료
2. 또는 `.env`에서 `SERVER_PORT=8081`로 변경

### Java 버전 오류
```
Unsupported class file major version
```
**해결방법:**
1. Java 17 설치 확인: `java -version`
2. JAVA_HOME 환경변수 설정

### 데이터베이스 테이블 생성 오류
**해결방법:**
1. MySQL이 실행 중인지 확인
2. 데이터베이스 권한 확인
3. `.env`의 `JPA_DDL_AUTO=create` 로 변경 (최초 1회)

## 📞 도움이 필요할 때

1. **GitHub Issues** 사용
2. **로그 확인**: `logs/application.log`
3. **MySQL 로그 확인**: MySQL 에러 로그
4. **커뮤니티**: Stack Overflow, Spring Boot 공식 문서

## 📚 추가 학습 자료

- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [MySQL 8.0 문서](https://dev.mysql.com/doc/refman/8.0/en/)
- [JWT.io](https://jwt.io/) - JWT 토큰 디버깅
- [Spring Security 가이드](https://spring.io/guides/topicals/spring-security-architecture)

---

**행운을 빕니다! 🍀** 