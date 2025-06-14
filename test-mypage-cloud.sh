#!/bin/bash

# 포트 포워딩을 통한 클라우드 서비스 접근
BASE_URL="http://localhost:8080"  # kubectl port-forward를 통한 접근

echo "=== 클라우드 환경 마이페이지 성능 테스트 ==="
echo "Base URL: $BASE_URL"
echo ""

# 1. 먼저 회원가입으로 테스트 사용자 생성
echo "1. 테스트 사용자 생성 중..."
SIGNUP_RESPONSE=$(curl -s -w "\n응답시간: %{time_total}초" \
  -X POST "$BASE_URL/api/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser_'$(date +%s)'",
    "password": "testpass123",
    "nickname": "테스트유저_'$(date +%s)'",
    "email": "test'$(date +%s)'@test.com"
  }')

echo "$SIGNUP_RESPONSE"
echo ""

# 2. 로그인해서 JWT 토큰 획득
echo "2. 로그인 중..."
LOGIN_RESPONSE=$(curl -s -w "\n응답시간: %{time_total}초" \
  -X POST "$BASE_URL/api/users/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123",
    "email": "admin@test.com"
  }')

echo "$LOGIN_RESPONSE"

# JWT 토큰 추출 (jq가 없을 경우를 대비한 간단한 방법)
JWT_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$JWT_TOKEN" ]; then
    echo "❌ JWT 토큰을 획득할 수 없습니다. 로그인을 확인해주세요."
    exit 1
fi

echo "✅ JWT 토큰 획득 성공"
echo ""

# 3. 마이페이지 API들 성능 테스트
echo "3. 마이페이지 API 성능 테스트 시작..."
echo "JWT Token: ${JWT_TOKEN:0:20}..."
echo ""

# 내 프로필 조회
echo "📋 내 프로필 조회 테스트"
for i in {1..3}; do
    echo "  시도 $i:"
    PROFILE_RESPONSE=$(curl -s -w "    응답시간: %{time_total}초\n" \
      -X GET "$BASE_URL/api/users/profile" \
      -H "Authorization: Bearer $JWT_TOKEN")
    
    echo "    응답: $(echo "$PROFILE_RESPONSE" | head -c 100)..."
done
echo ""

# 내 상품 목록 조회
echo "🛍️ 내 상품 목록 조회 테스트"
for i in {1..3}; do
    echo "  시도 $i:"
    PRODUCTS_RESPONSE=$(curl -s -w "    응답시간: %{time_total}초\n" \
      -X GET "$BASE_URL/api/users/my-products" \
      -H "Authorization: Bearer $JWT_TOKEN")
    
    echo "    응답: $(echo "$PRODUCTS_RESPONSE" | head -c 100)..."
done
echo ""

# 구매한 상품 목록 조회
echo "🛒 구매한 상품 목록 조회 테스트"
for i in {1..3}; do
    echo "  시도 $i:"
    PURCHASES_RESPONSE=$(curl -s -w "    응답시간: %{time_total}초\n" \
      -X GET "$BASE_URL/api/users/purchased-products" \
      -H "Authorization: Bearer $JWT_TOKEN")
    
    echo "    응답: $(echo "$PURCHASES_RESPONSE" | head -c 100)..."
done
echo ""

# 대시보드 조회
echo "📊 대시보드 조회 테스트"
for i in {1..3}; do
    echo "  시도 $i:"
    DASHBOARD_RESPONSE=$(curl -s -w "    응답시간: %{time_total}초\n" \
      -X GET "$BASE_URL/api/users/dashboard" \
      -H "Authorization: Bearer $JWT_TOKEN")
    
    echo "    응답: $(echo "$DASHBOARD_RESPONSE" | head -c 100)..."
done
echo ""

echo "=== 테스트 완료 ==="
echo "💡 참고: 첫 번째 요청은 캐시 미스로 인해 느릴 수 있습니다."
echo "💡 두 번째, 세 번째 요청에서 캐시 효과를 확인할 수 있습니다." 