# 📚 API 명세서

## 🎯 새롭게 추가된 API 엔드포인트들

### 🆕 추가된 기능들

| 기능 | 이전 | 현재 | 상태 |
|------|------|------|------|
| 사용자 목록 | ❌ | ✅ `GET /api/users` | 🆕 신규 |
| 사용자 검색 | ❌ | ✅ `GET /api/users/search` | 🆕 신규 |
| 사용자 삭제 | ❌ | ✅ `DELETE /api/users/{id}` | 🆕 신규 |
| 닉네임 변경 | ❌ | ✅ `PUT /api/users/nickname` | 🆕 신규 |
| 비밀번호 변경 | ❌ | ✅ `PUT /api/users/password` | 🆕 신규 |
| 이벤트 시스템 | ❌ | ✅ 자동 알림 발송 | 🆕 신규 |

## 📝 상세 API 명세

### 1. 사용자 목록 조회

```http
GET /api/users
Authorization: Bearer {token}
```

**응답 예제:**
```json
{
  "success": true,
  "message": "사용자 목록 조회가 완료되었습니다.",
  "data": [
    {
      "userId": 1,
      "email": "user1@example.com",
      "nickname": "사용자1"
    },
    {
      "userId": 2,
      "email": "user2@example.com", 
      "nickname": "사용자2"
    }
  ]
}
```

### 2. 사용자 검색

```http
GET /api/users/search?keyword={keyword}
Authorization: Bearer {token}
```

**파라미터:**
- `keyword`: 검색할 키워드 (이메일 또는 닉네임)

**응답 예제:**
```json
{
  "success": true,
  "message": "사용자 검색이 완료되었습니다.",
  "data": [
    {
      "userId": 1,
      "email": "john@example.com",
      "nickname": "john"
    }
  ]
}
```

### 3. 사용자 삭제

```http
DELETE /api/users/{userId}
Authorization: Bearer {token}
```

**응답 예제:**
```json
{
  "success": true,
  "message": "사용자가 삭제되었습니다.",
  "data": null
}
```

### 4. 닉네임 변경

```http
PUT /api/users/nickname?newNickname={nickname}
Authorization: Bearer {token}
```

**응답 예제:**
```json
{
  "success": true,
  "message": "닉네임이 변경되었습니다.",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "새로운닉네임"
  }
}
```

**🎊 자동 발생 이벤트:**
- 프로필 업데이트 푸시 알림 자동 발송
- Product 서비스에 사용자 정보 동기화

### 5. 비밀번호 변경

```http
PUT /api/users/password
Authorization: Bearer {token}
Content-Type: application/json

{
  "currentPassword": "현재비밀번호",
  "newPassword": "새로운비밀번호"
}
```

**응답 예제:**
```json
{
  "success": true,
  "message": "비밀번호가 변경되었습니다.",
  "data": null
}
```

## 🎉 자동 이벤트 시스템

### 회원가입 시 자동 발생 이벤트
```javascript
// 프론트엔드에서 회원가입 호출
const response = await fetch('/api/users/register', {
  method: 'POST',
  body: JSON.stringify({ email, password, nickname })
});

// 백엔드에서 자동으로 발생하는 일들:
// 1. 사용자 DB 저장
// 2. 회원가입 이벤트 발행 (RabbitMQ)
// 3. 환영 이메일 자동 발송
// 4. Product 서비스에 신규 사용자 알림
```

### 프로필 업데이트 시 자동 발생 이벤트
```javascript
// 프론트엔드에서 닉네임 변경 호출
const response = await fetch('/api/users/nickname?newNickname=새닉네임', {
  method: 'PUT'
});

// 백엔드에서 자동으로 발생하는 일들:
// 1. 닉네임 DB 업데이트
// 2. 프로필 업데이트 이벤트 발행
// 3. 업데이트 알림 푸시 발송
// 4. Product 서비스 사용자 정보 동기화
```

## 🔄 React/Vue 컴포넌트 예제

### React Hook 예제

```tsx
// useUsers.ts
import { useState, useEffect } from 'react';

interface User {
  userId: number;
  email: string;
  nickname: string;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export function useUsers() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchUsers = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const token = localStorage.getItem('authToken');
      const response = await fetch('/api/users', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      const result: ApiResponse<User[]> = await response.json();
      
      if (result.success) {
        setUsers(result.data);
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError('사용자 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const deleteUser = async (userId: number) => {
    try {
      const token = localStorage.getItem('authToken');
      const response = await fetch(`/api/users/${userId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      const result: ApiResponse<null> = await response.json();
      
      if (result.success) {
        // 목록에서 제거
        setUsers(prev => prev.filter(user => user.userId !== userId));
        return true;
      } else {
        setError(result.message);
        return false;
      }
    } catch (err) {
      setError('사용자 삭제에 실패했습니다.');
      return false;
    }
  };

  const searchUsers = async (keyword: string) => {
    setLoading(true);
    setError(null);
    
    try {
      const token = localStorage.getItem('authToken');
      const response = await fetch(
        `/api/users/search?keyword=${encodeURIComponent(keyword)}`,
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );
      
      const result: ApiResponse<User[]> = await response.json();
      
      if (result.success) {
        setUsers(result.data);
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError('사용자 검색에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  return {
    users,
    loading,
    error,
    fetchUsers,
    deleteUser,
    searchUsers
  };
}
```

### React 컴포넌트 예제

```tsx
// UserManagement.tsx
import React, { useState } from 'react';
import { useUsers } from './hooks/useUsers';

export function UserManagement() {
  const { users, loading, error, deleteUser, searchUsers, fetchUsers } = useUsers();
  const [searchKeyword, setSearchKeyword] = useState('');

  const handleSearch = () => {
    if (searchKeyword.trim()) {
      searchUsers(searchKeyword);
    } else {
      fetchUsers(); // 전체 목록 다시 로드
    }
  };

  const handleDelete = async (userId: number, nickname: string) => {
    if (confirm(`정말로 "${nickname}" 사용자를 삭제하시겠습니까?`)) {
      const success = await deleteUser(userId);
      if (success) {
        alert('사용자가 삭제되었습니다.');
      }
    }
  };

  if (loading) return <div>로딩 중...</div>;
  if (error) return <div>오류: {error}</div>;

  return (
    <div className="user-management">
      <h2>사용자 관리</h2>
      
      {/* 검색 */}
      <div className="search-section">
        <input
          type="text"
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
          placeholder="이메일 또는 닉네임으로 검색..."
        />
        <button onClick={handleSearch}>검색</button>
        <button onClick={fetchUsers}>전체 목록</button>
      </div>

      {/* 사용자 목록 */}
      <div className="user-list">
        {users.length === 0 ? (
          <p>사용자가 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>이메일</th>
                <th>닉네임</th>
                <th>작업</th>
              </tr>
            </thead>
            <tbody>
              {users.map(user => (
                <tr key={user.userId}>
                  <td>{user.userId}</td>
                  <td>{user.email}</td>
                  <td>{user.nickname}</td>
                  <td>
                    <button
                      onClick={() => handleDelete(user.userId, user.nickname)}
                      className="delete-btn"
                    >
                      삭제
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
```

### Vue 3 Composition API 예제

```vue
<!-- UserManagement.vue -->
<template>
  <div class="user-management">
    <h2>사용자 관리</h2>
    
    <!-- 검색 -->
    <div class="search-section">
      <input
        v-model="searchKeyword"
        @keyup.enter="handleSearch"
        placeholder="이메일 또는 닉네임으로 검색..."
      />
      <button @click="handleSearch">검색</button>
      <button @click="fetchUsers">전체 목록</button>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="loading">로딩 중...</div>
    
    <!-- 에러 상태 -->
    <div v-else-if="error" class="error">오류: {{ error }}</div>
    
    <!-- 사용자 목록 -->
    <div v-else class="user-list">
      <table v-if="users.length > 0">
        <thead>
          <tr>
            <th>ID</th>
            <th>이메일</th>
            <th>닉네임</th>
            <th>작업</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.userId">
            <td>{{ user.userId }}</td>
            <td>{{ user.email }}</td>
            <td>{{ user.nickname }}</td>
            <td>
              <button @click="handleDelete(user.userId, user.nickname)">
                삭제
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else>사용자가 없습니다.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';

interface User {
  userId: number;
  email: string;
  nickname: string;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

const users = ref<User[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const searchKeyword = ref('');

const apiCall = async (url: string, options: RequestInit = {}) => {
  const token = localStorage.getItem('authToken');
  const response = await fetch(url, {
    ...options,
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
      ...options.headers
    }
  });
  return response.json();
};

const fetchUsers = async () => {
  loading.value = true;
  error.value = null;
  
  try {
    const result: ApiResponse<User[]> = await apiCall('/api/users');
    
    if (result.success) {
      users.value = result.data;
    } else {
      error.value = result.message;
    }
  } catch (err) {
    error.value = '사용자 목록을 불러오는데 실패했습니다.';
  } finally {
    loading.value = false;
  }
};

const searchUsers = async (keyword: string) => {
  loading.value = true;
  error.value = null;
  
  try {
    const result: ApiResponse<User[]> = await apiCall(
      `/api/users/search?keyword=${encodeURIComponent(keyword)}`
    );
    
    if (result.success) {
      users.value = result.data;
    } else {
      error.value = result.message;
    }
  } catch (err) {
    error.value = '사용자 검색에 실패했습니다.';
  } finally {
    loading.value = false;
  }
};

const deleteUser = async (userId: number, nickname: string) => {
  if (!confirm(`정말로 "${nickname}" 사용자를 삭제하시겠습니까?`)) {
    return;
  }
  
  try {
    const result: ApiResponse<null> = await apiCall(`/api/users/${userId}`, {
      method: 'DELETE'
    });
    
    if (result.success) {
      users.value = users.value.filter(user => user.userId !== userId);
      alert('사용자가 삭제되었습니다.');
    } else {
      error.value = result.message;
    }
  } catch (err) {
    error.value = '사용자 삭제에 실패했습니다.';
  }
};

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    searchUsers(searchKeyword.value);
  } else {
    fetchUsers();
  }
};

const handleDelete = (userId: number, nickname: string) => {
  deleteUser(userId, nickname);
};

onMounted(() => {
  fetchUsers();
});
</script>
```

## 📱 모바일 앱 (React Native) 예제

```tsx
// UserService.ts
export class UserService {
  private baseUrl = 'http://localhost:8080/api';
  
  private async getAuthHeaders() {
    const token = await AsyncStorage.getItem('authToken');
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    };
  }

  async getUsers(): Promise<User[]> {
    const headers = await this.getAuthHeaders();
    const response = await fetch(`${this.baseUrl}/users`, { headers });
    const result = await response.json();
    
    if (result.success) {
      return result.data;
    }
    throw new Error(result.message);
  }

  async searchUsers(keyword: string): Promise<User[]> {
    const headers = await this.getAuthHeaders();
    const response = await fetch(
      `${this.baseUrl}/users/search?keyword=${encodeURIComponent(keyword)}`,
      { headers }
    );
    const result = await response.json();
    
    if (result.success) {
      return result.data;
    }
    throw new Error(result.message);
  }

  async deleteUser(userId: number): Promise<void> {
    const headers = await this.getAuthHeaders();
    const response = await fetch(`${this.baseUrl}/users/${userId}`, {
      method: 'DELETE',
      headers
    });
    const result = await response.json();
    
    if (!result.success) {
      throw new Error(result.message);
    }
  }
}
```

이제 프론트엔드 개발자들이 백엔드의 모든 새로운 기능을 완벽하게 활용할 수 있습니다! 🚀 