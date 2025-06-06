# 🔒 보안 설정 가이드

## 중요 사항
이 프로젝트는 민감한 정보를 하드코딩하지 않고 환경변수와 Kubernetes Secret을 사용합니다.

## 🔑 Kubernetes Secret 설정

### 1. Secret 생성
```bash
kubectl create secret generic user-service-secrets \
  --from-literal=database-url="jdbc:mysql://YOUR_DB_HOST:PORT/DB_NAME?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8" \
  --from-literal=database-username="YOUR_USERNAME" \
  --from-literal=database-password="YOUR_PASSWORD" \
  --from-literal=jwt-secret="YOUR_JWT_SECRET_KEY" \
  --namespace=tuk-trainee12
```

### 2. Secret 확인
```bash
kubectl get secrets user-service-secrets -n tuk-trainee12
kubectl describe secret user-service-secrets -n tuk-trainee12
```

## 🛡️ 로컬 개발 환경 설정

### 환경변수 설정 방법

#### Option 1: IDE 환경변수 설정
```bash
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/minicarrot
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password
JWT_SECRET=your_jwt_secret_key
```

#### Option 2: application-local.yml 파일 생성 (.gitignore에 포함됨)
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/minicarrot
    username: your_username
    password: your_password

jwt:
  secret: your_jwt_secret_key
  expiration: 86400000
```

## ⚠️ 절대 하지 말아야 할 것들

1. **데이터베이스 정보 하드코딩 금지**
2. **JWT Secret 키 하드코딩 금지**
3. **실제 Secret 파일 Git 커밋 금지**
4. **프로덕션 환경 정보 공개 금지**

## 🔍 민감한 정보 체크리스트

배포 전 반드시 확인:
- [ ] 데이터베이스 URL/사용자명/비밀번호가 하드코딩되지 않았는가?
- [ ] JWT Secret이 하드코딩되지 않았는가?
- [ ] API 키나 토큰이 노출되지 않았는가?
- [ ] 로그 파일에 민감한 정보가 기록되지 않았는가?

## 📞 문제 발생 시

민감한 정보가 실수로 커밋된 경우:
1. 즉시 Secret 키 변경
2. Git 히스토리에서 민감한 정보 제거
3. 보안팀에 즉시 보고 