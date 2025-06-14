# 🔄 마이페이지 실시간 업데이트 가이드

## 🎯 문제 해결

### 1. **새로고침해야 대시보드 수량이 업데이트되는 문제**
- **해결**: SSE(Server-Sent Events) 실시간 알림 연동
- **구현**: 자동 대시보드 새로고침 이벤트 수신

### 2. **내가 등록한 상품, 구매한 상품 탭에 아무것도 안 뜨는 문제**
- **해결**: Product Service API 엔드포인트 수정 완료
- **구현**: 올바른 API 호출 및 데이터 매핑

## 🚀 프론트엔드 구현 가이드

### 1. SSE 연결 설정

```javascript
// SSE 연결 관리 클래스
class MyPageSSEManager {
    constructor(userId, authToken) {
        this.userId = userId;
        this.authToken = authToken;
        this.eventSource = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
    }

    // SSE 연결 시작
    connect() {
        const url = `/api/notifications/stream/${this.userId}`;
        
        this.eventSource = new EventSource(url);
        
        this.eventSource.onopen = () => {
            console.log('🔌 마이페이지 실시간 연결 성공');
            this.reconnectAttempts = 0;
        };

        this.eventSource.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                this.handleNotification(data);
            } catch (error) {
                console.error('알림 데이터 파싱 오류:', error);
            }
        };

        this.eventSource.onerror = (error) => {
            console.error('SSE 연결 오류:', error);
            this.handleReconnect();
        };
    }

    // 알림 처리
    handleNotification(notification) {
        console.log('📢 실시간 알림 수신:', notification);

        switch (notification.type) {
            case 'dashboard_refresh':
                this.refreshDashboard(notification);
                break;
            case 'product_registered':
                this.handleProductRegistered(notification);
                break;
            case 'product_sold':
                this.handleProductSold(notification);
                break;
            case 'product_purchased':
                this.handleProductPurchased(notification);
                break;
            case 'product_status_changed':
                this.handleProductStatusChanged(notification);
                break;
        }
    }

    // 대시보드 자동 새로고침
    async refreshDashboard(notification) {
        console.log('🔄 대시보드 자동 새로고침:', notification.eventType);
        
        try {
            // 대시보드 데이터 다시 로드
            await this.loadDashboardData();
            
            // 관련 탭도 새로고침
            if (notification.eventType === 'product_registered') {
                await this.loadMyProducts();
            } else if (notification.eventType === 'product_purchased') {
                await this.loadPurchasedProducts();
            }
            
            // 사용자에게 알림 표시 (선택사항)
            this.showToast('📊 대시보드가 업데이트되었습니다.', 'success');
            
        } catch (error) {
            console.error('대시보드 새로고침 실패:', error);
        }
    }

    // 상품 등록 완료 처리
    async handleProductRegistered(notification) {
        console.log('✅ 상품 등록 완료:', notification);
        
        // 내가 등록한 상품 목록 새로고침
        await this.loadMyProducts();
        
        // 성공 알림 표시
        this.showToast(notification.message, 'success');
    }

    // 상품 판매 완료 처리
    async handleProductSold(notification) {
        console.log('💰 상품 판매 완료:', notification);
        
        // 대시보드와 상품 목록 새로고침
        await Promise.all([
            this.loadDashboardData(),
            this.loadMyProducts()
        ]);
        
        this.showToast(notification.message, 'success');
    }

    // 상품 구매 완료 처리
    async handleProductPurchased(notification) {
        console.log('🛒 상품 구매 완료:', notification);
        
        // 구매 목록 새로고침
        await this.loadPurchasedProducts();
        
        this.showToast(notification.message, 'success');
    }

    // 상품 상태 변경 처리
    async handleProductStatusChanged(notification) {
        console.log('📋 상품 상태 변경:', notification);
        
        await this.loadMyProducts();
        this.showToast(notification.message, 'info');
    }

    // 재연결 처리
    handleReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`🔄 SSE 재연결 시도 ${this.reconnectAttempts}/${this.maxReconnectAttempts}`);
            
            setTimeout(() => {
                this.connect();
            }, 2000 * this.reconnectAttempts); // 지수 백오프
        } else {
            console.error('❌ SSE 재연결 포기');
            this.showToast('실시간 업데이트 연결이 끊어졌습니다.', 'warning');
        }
    }

    // 연결 종료
    disconnect() {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
            console.log('🔌 SSE 연결 종료');
        }
    }

    // API 호출 헬퍼
    async apiCall(url, options = {}) {
        const response = await fetch(url, {
            ...options,
            headers: {
                'Authorization': `Bearer ${this.authToken}`,
                'Content-Type': 'application/json',
                ...options.headers
            }
        });
        
        if (!response.ok) {
            throw new Error(`API 호출 실패: ${response.status}`);
        }
        
        return response.json();
    }

    // 대시보드 데이터 로드
    async loadDashboardData() {
        try {
            const result = await this.apiCall('/api/users/dashboard');
            
            if (result.success) {
                this.updateDashboardUI(result.data);
            }
        } catch (error) {
            console.error('대시보드 로드 실패:', error);
        }
    }

    // 내가 등록한 상품 로드
    async loadMyProducts() {
        try {
            const result = await this.apiCall('/api/users/products/mine');
            
            if (result.success) {
                this.updateMyProductsUI(result.data);
            }
        } catch (error) {
            console.error('내 상품 로드 실패:', error);
        }
    }

    // 구매한 상품 로드
    async loadPurchasedProducts() {
        try {
            const result = await this.apiCall('/api/users/products/purchased');
            
            if (result.success) {
                this.updatePurchasedProductsUI(result.data);
            }
        } catch (error) {
            console.error('구매 상품 로드 실패:', error);
        }
    }

    // UI 업데이트 메서드들 (구현 필요)
    updateDashboardUI(data) {
        // 대시보드 통계 업데이트
        const stats = data.stats;
        
        document.getElementById('registered-count').textContent = stats.registeredProducts;
        document.getElementById('purchased-count').textContent = stats.purchasedProducts;
        document.getElementById('sold-count').textContent = stats.soldProducts;
        document.getElementById('total-sales').textContent = `₩${stats.totalSales.toLocaleString()}`;
        document.getElementById('total-purchases').textContent = `₩${stats.totalPurchases.toLocaleString()}`;
        
        // 최근 활동 업데이트
        this.updateRecentActivity(data.recentActivity);
    }

    updateMyProductsUI(data) {
        const container = document.getElementById('my-products-list');
        
        if (data.products && data.products.length > 0) {
            container.innerHTML = data.products.map(product => `
                <div class="product-card" data-product-id="${product.id}">
                    <img src="${product.imageUrl || '/images/no-image.png'}" alt="${product.name}">
                    <div class="product-info">
                        <h3>${product.name}</h3>
                        <p class="price">₩${product.price.toLocaleString()}</p>
                        <p class="status status-${product.status.toLowerCase()}">${product.statusText}</p>
                        <p class="date">${new Date(product.createdAt).toLocaleDateString()}</p>
                    </div>
                </div>
            `).join('');
        } else {
            container.innerHTML = '<div class="empty-state">등록한 상품이 없습니다.</div>';
        }
        
        // 카운트 업데이트
        document.getElementById('my-products-count').textContent = data.count;
    }

    updatePurchasedProductsUI(data) {
        const container = document.getElementById('purchased-products-list');
        
        if (data.products && data.products.length > 0) {
            container.innerHTML = data.products.map(product => `
                <div class="product-card" data-product-id="${product.id}">
                    <img src="${product.imageUrl || '/images/no-image.png'}" alt="${product.name}">
                    <div class="product-info">
                        <h3>${product.name}</h3>
                        <p class="price">₩${product.price.toLocaleString()}</p>
                        <p class="seller">판매자: ${product.sellerNickname}</p>
                        <p class="date">${new Date(product.purchasedAt).toLocaleDateString()}</p>
                    </div>
                </div>
            `).join('');
        } else {
            container.innerHTML = '<div class="empty-state">구매한 상품이 없습니다.</div>';
        }
        
        // 카운트 업데이트
        document.getElementById('purchased-products-count').textContent = data.count;
    }

    updateRecentActivity(activities) {
        const container = document.getElementById('recent-activity-list');
        
        if (activities && activities.length > 0) {
            container.innerHTML = activities.map(activity => `
                <div class="activity-item">
                    <div class="activity-icon">${this.getActivityIcon(activity.type)}</div>
                    <div class="activity-content">
                        <p class="activity-message">${activity.message}</p>
                        <p class="activity-time">${this.formatTime(activity.timestamp)}</p>
                    </div>
                </div>
            `).join('');
        } else {
            container.innerHTML = '<div class="empty-state">최근 활동이 없습니다.</div>';
        }
    }

    // 토스트 알림 표시
    showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.textContent = message;
        
        document.body.appendChild(toast);
        
        // 애니메이션
        setTimeout(() => toast.classList.add('show'), 100);
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => document.body.removeChild(toast), 300);
        }, 3000);
    }

    // 활동 아이콘 반환
    getActivityIcon(type) {
        const icons = {
            'product_registered': '📝',
            'product_sold': '💰',
            'product_purchased': '🛒',
            'product_status_changed': '📋'
        };
        return icons[type] || '📌';
    }

    // 시간 포맷팅
    formatTime(timestamp) {
        const now = new Date();
        const time = new Date(timestamp);
        const diff = now - time;
        
        if (diff < 60000) return '방금 전';
        if (diff < 3600000) return `${Math.floor(diff / 60000)}분 전`;
        if (diff < 86400000) return `${Math.floor(diff / 3600000)}시간 전`;
        return time.toLocaleDateString();
    }
}
```

### 2. React Hook 구현 예제

```jsx
// useMyPageSSE.js
import { useEffect, useRef, useState } from 'react';

export function useMyPageSSE(userId, authToken) {
    const [dashboardData, setDashboardData] = useState(null);
    const [myProducts, setMyProducts] = useState([]);
    const [purchasedProducts, setPurchasedProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const sseManagerRef = useRef(null);

    useEffect(() => {
        if (!userId || !authToken) return;

        // SSE 매니저 초기화
        const sseManager = new MyPageSSEManager(userId, authToken);
        
        // UI 업데이트 콜백 설정
        sseManager.updateDashboardUI = (data) => setDashboardData(data);
        sseManager.updateMyProductsUI = (data) => setMyProducts(data.products || []);
        sseManager.updatePurchasedProductsUI = (data) => setPurchasedProducts(data.products || []);
        
        // 초기 데이터 로드
        const loadInitialData = async () => {
            setLoading(true);
            try {
                await Promise.all([
                    sseManager.loadDashboardData(),
                    sseManager.loadMyProducts(),
                    sseManager.loadPurchasedProducts()
                ]);
            } catch (error) {
                console.error('초기 데이터 로드 실패:', error);
            } finally {
                setLoading(false);
            }
        };

        // SSE 연결 시작
        sseManager.connect();
        loadInitialData();
        
        sseManagerRef.current = sseManager;

        // 정리
        return () => {
            sseManager.disconnect();
        };
    }, [userId, authToken]);

    return {
        dashboardData,
        myProducts,
        purchasedProducts,
        loading,
        refresh: () => {
            if (sseManagerRef.current) {
                sseManagerRef.current.loadDashboardData();
                sseManagerRef.current.loadMyProducts();
                sseManagerRef.current.loadPurchasedProducts();
            }
        }
    };
}

// MyPage.jsx
import React from 'react';
import { useMyPageSSE } from './useMyPageSSE';

export function MyPage() {
    const userId = 1; // 실제로는 인증에서 가져옴
    const authToken = localStorage.getItem('authToken');
    
    const { dashboardData, myProducts, purchasedProducts, loading, refresh } = 
        useMyPageSSE(userId, authToken);

    if (loading) {
        return <div className="loading">로딩 중...</div>;
    }

    return (
        <div className="my-page">
            <h1>마이페이지</h1>
            
            {/* 대시보드 */}
            <section className="dashboard">
                <h2>대시보드</h2>
                {dashboardData && (
                    <div className="stats-grid">
                        <div className="stat-card">
                            <h3>등록한 상품</h3>
                            <p className="stat-number">{dashboardData.stats.registeredProducts}</p>
                        </div>
                        <div className="stat-card">
                            <h3>구매한 상품</h3>
                            <p className="stat-number">{dashboardData.stats.purchasedProducts}</p>
                        </div>
                        <div className="stat-card">
                            <h3>판매 완료</h3>
                            <p className="stat-number">{dashboardData.stats.soldProducts}</p>
                        </div>
                        <div className="stat-card">
                            <h3>총 판매금액</h3>
                            <p className="stat-number">₩{dashboardData.stats.totalSales.toLocaleString()}</p>
                        </div>
                    </div>
                )}
            </section>

            {/* 내가 등록한 상품 */}
            <section className="my-products">
                <h2>내가 등록한 상품 ({myProducts.length})</h2>
                <div className="products-grid">
                    {myProducts.map(product => (
                        <div key={product.id} className="product-card">
                            <img src={product.imageUrl || '/images/no-image.png'} alt={product.name} />
                            <h3>{product.name}</h3>
                            <p className="price">₩{product.price.toLocaleString()}</p>
                            <p className={`status status-${product.status.toLowerCase()}`}>
                                {product.statusText}
                            </p>
                        </div>
                    ))}
                </div>
                {myProducts.length === 0 && (
                    <div className="empty-state">등록한 상품이 없습니다.</div>
                )}
            </section>

            {/* 구매한 상품 */}
            <section className="purchased-products">
                <h2>구매한 상품 ({purchasedProducts.length})</h2>
                <div className="products-grid">
                    {purchasedProducts.map(product => (
                        <div key={product.id} className="product-card">
                            <img src={product.imageUrl || '/images/no-image.png'} alt={product.name} />
                            <h3>{product.name}</h3>
                            <p className="price">₩{product.price.toLocaleString()}</p>
                            <p className="seller">판매자: {product.sellerNickname}</p>
                        </div>
                    ))}
                </div>
                {purchasedProducts.length === 0 && (
                    <div className="empty-state">구매한 상품이 없습니다.</div>
                )}
            </section>

            {/* 수동 새로고침 버튼 */}
            <button onClick={refresh} className="refresh-button">
                🔄 새로고침
            </button>
        </div>
    );
}
```

### 3. CSS 스타일

```css
/* 토스트 알림 스타일 */
.toast {
    position: fixed;
    top: 20px;
    right: 20px;
    padding: 12px 20px;
    border-radius: 8px;
    color: white;
    font-weight: 500;
    z-index: 1000;
    transform: translateX(100%);
    transition: transform 0.3s ease;
}

.toast.show {
    transform: translateX(0);
}

.toast-success { background-color: #10b981; }
.toast-info { background-color: #3b82f6; }
.toast-warning { background-color: #f59e0b; }
.toast-error { background-color: #ef4444; }

/* 상품 상태 스타일 */
.status {
    padding: 4px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;
}

.status-available { background-color: #dcfce7; color: #166534; }
.status-sold { background-color: #fee2e2; color: #991b1b; }
.status-reserved { background-color: #fef3c7; color: #92400e; }
.status-hidden { background-color: #f3f4f6; color: #6b7280; }

/* 빈 상태 스타일 */
.empty-state {
    text-align: center;
    padding: 40px;
    color: #6b7280;
    font-size: 16px;
}

/* 로딩 스타일 */
.loading {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 200px;
    font-size: 18px;
    color: #6b7280;
}
```

## 🔧 백엔드 수정 사항

1. **ProductServiceClient API 엔드포인트 수정** ✅
   - `/api/products/mine?userId=1` → `/api/products/user/1/registered`
   - `/api/products/purchased?userId=1` → `/api/products/user/1/purchased`

2. **실시간 이벤트 시스템 강화** ✅
   - 상품 등록/구매/상태변경 시 자동 대시보드 새로고침
   - SSE를 통한 실시간 알림 발송

3. **데이터 매핑 개선** ✅
   - Product Service 응답 형식에 맞춘 필드명 수정
   - 안전한 타입 캐스팅 추가

## 🚀 배포 및 테스트

```bash
# 1. 애플리케이션 빌드
./gradlew bootJar

# 2. Docker 이미지 빌드
docker build --platform linux/amd64 -t hwangsk0419/user-service:mypage-fix .

# 3. 이미지 푸시
docker push hwangsk0419/user-service:mypage-fix

# 4. Kubernetes 배포 업데이트
kubectl set image deployment/user-service user-service=hwangsk0419/user-service:mypage-fix
```

이제 마이페이지에서 **실시간 업데이트**가 작동하고, **내가 등록한 상품**과 **구매한 상품** 탭에도 데이터가 정상적으로 표시될 것입니다! 🎉 