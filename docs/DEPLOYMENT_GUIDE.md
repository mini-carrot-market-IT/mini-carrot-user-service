# 🚀 배포 가이드

## 📋 목차
- [환경별 배포 방법](#환경별-배포-방법)
- [데이터베이스 연결 변경사항](#데이터베이스-연결-변경사항)
- [NCP 클러스터 배포](#ncp-클러스터-배포)
- [로컬 개발 환경](#로컬-개발-환경)
- [트러블슈팅](#트러블슈팅)

## 🔄 환경별 배포 방법

### 🏗️ 중요한 변경사항
> ⚠️ **공용 DB에서 독립적인 MySQL 컨테이너로 변경**
> - 기존: 공유 MySQL 서버 (223.130.162.28:30100/bookdb)
> - 신규: 전용 MySQL 컨테이너 (tuk-trainee12 네임스페이스)

## 📊 데이터베이스 연결 변경사항

### 🔧 연결 정보 비교

| 항목 | 기존 (공용 DB) | 신규 (전용 컨테이너) |
|------|---------------|------------------|
| **호스트** | `223.130.162.28` | `mysql-service.tuk-trainee12.svc.cluster.local` |
| **포트** | `30100` | `3306` (내부) / `31206` (외부) |
| **데이터베이스** | `bookdb` | `mini_carrot_user` |
| **사용자** | `root` | `carrot_user` |
| **비밀번호** | `rootpassword` | `CarrotPass#2024` |
| **네임스페이스** | - | `tuk-trainee12` |

### 🔗 연결 URL 변경

```yaml
# 기존 (공용 DB)
spring:
  datasource:
    url: jdbc:mysql://223.130.162.28:30100/bookdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul

# 신규 (전용 컨테이너)
spring:
  datasource:
    url: jdbc:mysql://mysql-service.tuk-trainee12.svc.cluster.local:3306/mini_carrot_user?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
```

## ☁️ NCP 클러스터 배포

### 1️⃣ **사전 준비**

```bash
# 네임스페이스 확인
kubectl get namespace tuk-trainee12

# MySQL 서비스 확인
kubectl get svc -n tuk-trainee12 mysql-service
```

### 2️⃣ **Secret 생성**

```bash
# Secret 파일 적용
kubectl apply -f k8s/create-secrets.yaml

# Secret 생성 확인
kubectl get secrets -n tuk-trainee12
```

### 3️⃣ **User Service 배포**

```bash
# 애플리케이션 배포
kubectl apply -f k8s/user-service-deployment.yaml

# 배포 상태 확인
kubectl get deployments -n tuk-trainee12 user-service
kubectl get pods -n tuk-trainee12 -l app=user-service
```

### 4️⃣ **서비스 확인**

```bash
# 서비스 목록 확인
kubectl get svc -n tuk-trainee12

# NodePort 확인 (31207)
kubectl get svc -n tuk-trainee12 user-service-external

# 로그 확인
kubectl logs -n tuk-trainee12 -l app=user-service --tail=50
```

### 5️⃣ **헬스체크**

```bash
# 클러스터 내부에서 테스트
kubectl run test-pod --image=curlimages/curl -it --rm -- sh
curl http://user-service.tuk-trainee12.svc.cluster.local:8080/actuator/health

# 외부에서 테스트 (NodePort)
curl http://[노드IP]:31207/actuator/health
```

## 🐳 로컬 개발 환경

### 1️⃣ **Docker Compose 시작**

```bash
# 전체 인프라 시작
docker-compose -f docker-compose-messagequeue.yml up -d

# 서비스 상태 확인
docker-compose -f docker-compose-messagequeue.yml ps
```

### 2️⃣ **데이터베이스 초기화 확인**

```bash
# MySQL 컨테이너 접속
docker exec -it minicarrot-mysql mysql -u minicarrot -p

# 데이터베이스 확인
mysql> SHOW DATABASES;
mysql> USE minicarrot;
mysql> SHOW TABLES;
mysql> SELECT * FROM users;
```

### 3️⃣ **Spring Boot 애플리케이션 실행**

```bash
# 로컬 프로파일로 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 또는 IDE에서 실행 시
# VM Options: -Dspring.profiles.active=local
```

### 4️⃣ **서비스 테스트**

```bash
# 헬스체크
curl http://localhost:8080/actuator/health

# 회원가입 테스트
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "nickname": "테스터"
  }'

# 로그인 테스트
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@minicarrot.com",
    "password": "password123"
  }'
```

## 🔍 트러블슈팅

### 1️⃣ **데이터베이스 연결 오류**

```bash
# 증상: Can't connect to MySQL server
# 해결: MySQL 서비스 상태 확인
kubectl get pods -n tuk-trainee12 -l app=mysql
kubectl logs -n tuk-trainee12 [mysql-pod-name]

# DNS 해상도 확인
kubectl run test-dns --image=busybox -it --rm -- nslookup mysql-service.tuk-trainee12.svc.cluster.local
```

### 2️⃣ **Secret 접근 오류**

```bash
# 증상: couldn't find key jwt-secret in Secret
# 해결: Secret 재생성
kubectl delete secret user-service-secret -n tuk-trainee12
kubectl apply -f k8s/create-secrets.yaml
```

### 3️⃣ **NodePort 접속 불가**

```bash
# 증상: Connection refused on NodePort
# 해결: 서비스와 파드 상태 확인
kubectl get svc -n tuk-trainee12 user-service-external
kubectl get endpoints -n tuk-trainee12 user-service-external
kubectl describe svc -n tuk-trainee12 user-service-external
```

### 4️⃣ **애플리케이션 시작 실패**

```bash
# 로그 확인
kubectl logs -n tuk-trainee12 -l app=user-service --previous

# 환경변수 확인
kubectl describe pod -n tuk-trainee12 [pod-name]

# 재시작
kubectl rollout restart deployment/user-service -n tuk-trainee12
```

## 📊 모니터링 및 로그

### 🔍 **로그 확인**

```bash
# 실시간 로그 모니터링
kubectl logs -n tuk-trainee12 -l app=user-service -f

# 특정 파드 로그
kubectl logs -n tuk-trainee12 [pod-name] --tail=100

# 이전 컨테이너 로그 (재시작된 경우)
kubectl logs -n tuk-trainee12 [pod-name] --previous
```

### 📈 **메트릭 확인**

```bash
# Prometheus 메트릭
curl http://[노드IP]:31207/actuator/prometheus

# 헬스 상태
curl http://[노드IP]:31207/actuator/health
```

## 🔧 환경별 설정 파일

### **application-local.yml** (로컬 개발)
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/minicarrot
    username: minicarrot
    password: password123
```

### **application-ncp.yml** (NCP 클러스터)
```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql-service.tuk-trainee12.svc.cluster.local:3306/mini_carrot_user
    username: carrot_user
    password: CarrotPass#2024
```

## 🎯 성공적인 배포 확인 체크리스트

- [ ] MySQL 컨테이너가 정상 실행 중
- [ ] Secret이 정상적으로 생성됨
- [ ] User Service 파드가 Running 상태
- [ ] 서비스가 ClusterIP와 NodePort로 노출됨
- [ ] 헬스체크 API가 정상 응답
- [ ] 회원가입/로그인 API가 정상 동작
- [ ] RabbitMQ 이벤트가 정상 발행됨
- [ ] 로그에 에러가 없음

이제 독립적인 MySQL 컨테이너 환경에서 완벽하게 동작합니다! 🎉 