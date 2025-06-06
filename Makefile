# Mini 당근마켓 User Service Makefile
# 사용법: make <command>

.PHONY: help setup build run test clean docker-build docker-run docker-stop docker-clean logs

# 기본 명령어 (help)
help:
	@echo "🥕 Mini 당근마켓 User Service 명령어"
	@echo ""
	@echo "📋 설정 & 빌드:"
	@echo "  setup          - 초기 설정 (환경변수 파일 복사)"
	@echo "  build          - 애플리케이션 빌드"
	@echo ""
	@echo "🚀 실행:"
	@echo "  run            - 애플리케이션 실행"
	@echo "  test           - 테스트 실행"
	@echo ""
	@echo "🐳 Docker:"
	@echo "  docker-build   - Docker 이미지 빌드"
	@echo "  docker-run     - Docker Compose로 전체 스택 실행"
	@echo "  docker-stop    - Docker Compose 중지"
	@echo "  docker-clean   - Docker 리소스 정리"
	@echo ""
	@echo "📊 모니터링:"
	@echo "  logs           - 애플리케이션 로그 확인"
	@echo "  health         - 헬스체크"
	@echo ""
	@echo "🧹 정리:"
	@echo "  clean          - 빌드 파일 정리"

# 초기 설정
setup:
	@echo "🔧 초기 설정을 시작합니다..."
	@if [ ! -f .env ]; then \
		cp env.example .env; \
		echo "✅ .env 파일이 생성되었습니다. 필요한 값들을 수정해주세요."; \
	else \
		echo "ℹ️  .env 파일이 이미 존재합니다."; \
	fi
	@chmod +x gradlew
	@echo "✅ 실행 권한이 부여되었습니다."

# 빌드
build:
	@echo "🔨 애플리케이션을 빌드합니다..."
	./gradlew clean build -x test

# 실행
run:
	@echo "🚀 애플리케이션을 실행합니다..."
	./gradlew bootRun

# 테스트
test:
	@echo "🧪 테스트를 실행합니다..."
	./gradlew test

# JAR 빌드
jar:
	@echo "📦 JAR 파일을 빌드합니다..."
	./gradlew bootJar

# Docker 이미지 빌드
docker-build:
	@echo "🐳 Docker 이미지를 빌드합니다..."
	docker build -t mini-carrot-user-service:latest .

# Docker Compose 실행
docker-run:
	@echo "🐳 Docker Compose로 전체 스택을 실행합니다..."
	docker-compose up -d
	@echo "✅ 서비스가 시작되었습니다."
	@echo "📍 애플리케이션: http://localhost:8080"
	@echo "📍 MySQL: localhost:3306"

# Docker Compose 중지
docker-stop:
	@echo "🛑 Docker Compose를 중지합니다..."
	docker-compose down

# Docker 리소스 정리
docker-clean:
	@echo "🧹 Docker 리소스를 정리합니다..."
	docker-compose down -v
	docker system prune -f
	@echo "✅ 정리가 완료되었습니다."

# 로그 확인
logs:
	@echo "📊 애플리케이션 로그를 확인합니다..."
	docker-compose logs -f user-service

# 헬스체크
health:
	@echo "💊 헬스체크를 실행합니다..."
	@curl -s http://localhost:8080/api/users/health | jq . || echo "❌ 서비스가 실행중이지 않거나 jq가 설치되지 않았습니다."

# 빌드 정리
clean:
	@echo "🧹 빌드 파일을 정리합니다..."
	./gradlew clean
	@echo "✅ 정리가 완료되었습니다."

# 개발 환경 설정
dev-setup: setup
	@echo "👨‍💻 개발 환경을 설정합니다..."
	@echo "📝 다음 단계를 진행하세요:"
	@echo "1. MySQL 설치 및 실행"
	@echo "2. .env 파일에서 데이터베이스 정보 수정"
	@echo "3. make run 으로 애플리케이션 실행"

# 전체 테스트 (빌드 + 테스트 + 헬스체크)
test-all: build test
	@echo "🔍 통합 테스트를 실행합니다..."
	@make run &
	@sleep 10
	@make health
	@pkill -f "gradle.*bootRun" || true

# API 테스트
api-test:
	@echo "🧪 API 테스트를 실행합니다..."
	@echo "1. 헬스체크..."
	@curl -s http://localhost:8080/api/users/health
	@echo -e "\n2. 회원가입 테스트..."
	@curl -X POST http://localhost:8080/api/users/register \
		-H "Content-Type: application/json" \
		-d '{"email":"test@example.com","password":"password123","nickname":"테스트"}'
	@echo -e "\n3. 로그인 테스트..."
	@curl -X POST http://localhost:8080/api/users/login \
		-H "Content-Type: application/json" \
		-d '{"email":"test@example.com","password":"password123"}' 