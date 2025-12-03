# API 명세

## 기본 정보

- Base URL: `/api`
- Content-Type: `application/json`
- 인증: Session 기반 (JSESSIONID 쿠키)

## 인증

| 상태 | 설명 |
|------|------|
| 비회원 | 퀴즈/상품 조회만 가능 |
| 회원 | OAuth2 로그인 후 세션 쿠키 사용 (`JSESSIONID`) |

---

## 1. 인증 API (OAuth2)

### GET /api/auth/kakao

카카오 로그인 시작 (리다이렉트)

**Response** `302 Found`
- 카카오 로그인 페이지로 리다이렉트
- `Location: https://kauth.kakao.com/oauth/authorize?...`

---

### GET /api/auth/kakao/callback

카카오 로그인 콜백 (Spring Security가 자동 처리)

**Query Parameters**
- `code`: 카카오 인가 코드

**Response** `302 Found`
- 로그인 성공 시 프론트엔드로 리다이렉트
- 세션 쿠키(`JSESSIONID`)가 자동 설정됨
- `Location: {FRONTEND_URL}/auth/callback?success=true`

**Error**
- 로그인 실패 시: `Location: {FRONTEND_URL}/auth/callback?error=login_failed`

---

### GET /api/auth/me

현재 로그인 사용자 정보 조회

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@kakao.com",
    "name": "홍길동",
    "profileImageUrl": "https://k.kakaocdn.net/...",
    "provider": "KAKAO",
    "role": "USER"
  }
}
```

**Error**
- `401` - 로그인 필요

---

### POST /api/auth/logout

로그아웃

**Response** `200 OK`
```json
{
  "success": true,
  "data": null,
  "message": "로그아웃되었습니다"
}
```

**Note**: 세션이 무효화되며, 카카오 연결은 유지됨

---

## 2. 퀴즈 API

### GET /api/quizzes/{quizId}

퀴즈 상세 조회 (질문, 옵션 포함)

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "나의 스타일 찾기",
    "description": "10가지 질문으로 당신의 스타일을 분석합니다",
    "questions": [
      {
        "id": 1,
        "text": "선호하는 스타일은?",
        "type": "SINGLE",
        "required": true,
        "options": [
          {
            "id": 1,
            "text": "캐주얼",
            "value": "casual",
            "imageUrl": "https://..."
          },
          {
            "id": 2,
            "text": "포멀",
            "value": "formal",
            "imageUrl": "https://..."
          }
        ]
      }
    ]
  }
}
```

---

### POST /api/quizzes/{quizId}/sessions

퀴즈 세션 시작 (로그인 필요)

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "sessionId": 123,
    "quizId": 1,
    "startedAt": "2024-01-15T10:30:00"
  }
}
```

---

### POST /api/quizzes/sessions/{sessionId}/submit

퀴즈 답변 제출 및 완료

**Request**
```json
{
  "answers": [
    {
      "questionId": 1,
      "selectedOptions": ["casual"]
    },
    {
      "questionId": 2,
      "selectedOptions": ["spring", "fall"]
    }
  ]
}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "sessionId": 123,
    "completed": true,
    "completedAt": "2024-01-15T10:35:00"
  }
}
```

---

### POST /api/quizzes/sessions/{sessionId}/recommend

AI 추천 생성 요청

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "recommendations": [
      {
        "id": 1,
        "displayOrder": 1,
        "totalPrice": 250000,
        "reason": "캐주얼하면서도 세련된 스타일을 추천합니다",
        "style": "캐주얼 시크",
        "occasion": "데일리",
        "season": "봄/가을"
      },
      {
        "id": 2,
        "displayOrder": 2,
        "totalPrice": 180000,
        "reason": "편안하면서도 트렌디한 스타일입니다",
        "style": "스트릿 캐주얼",
        "occasion": "데일리",
        "season": "봄/가을"
      }
    ]
  }
}
```

---

## 3. 코디 API

### GET /api/outfits/{recommendationId}

추천 상세 조회

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "totalPrice": 250000,
    "reason": "캐주얼하면서도 세련된 스타일을 추천합니다",
    "style": "캐주얼 시크",
    "occasion": "데일리",
    "season": "봄/가을",
    "items": [
      {
        "id": 1,
        "category": "TOP",
        "displayOrder": 1,
        "product": {
          "id": 101,
          "name": "오버핏 옥스포드 셔츠",
          "brandName": "무신사 스탠다드",
          "price": 45000,
          "imageUrl": "https://...",
          "purchaseUrl": "https://..."
        },
        "priceSnapshot": 45000
      },
      {
        "id": 2,
        "category": "BOTTOM",
        "displayOrder": 2,
        "product": {
          "id": 102,
          "name": "와이드 데님 팬츠",
          "brandName": "리바이스",
          "price": 89000,
          "imageUrl": "https://...",
          "purchaseUrl": "https://..."
        },
        "priceSnapshot": 89000
      }
    ],
    "createdAt": "2024-01-15T10:35:00"
  }
}
```

---

### POST /api/outfits/{recommendationId}/save

추천 코디 저장 (회원 전용)

**Request**
```json
{
  "note": "출근룩으로 좋을 듯"
}
```

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "recommendationId": 1,
    "note": "출근룩으로 좋을 듯",
    "savedAt": "2024-01-15T11:00:00"
  }
}
```

**Error**
- `401` - 로그인 필요
- `409` - 이미 저장됨

---

## 4. 상품 API

### GET /api/products/{productId}

상품 상세 조회

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 101,
    "name": "오버핏 옥스포드 셔츠",
    "brand": {
      "id": 1,
      "name": "무신사 스탠다드",
      "logoUrl": "https://..."
    },
    "category": "TOP",
    "subCategory": "셔츠",
    "price": 45000,
    "originalPrice": 59000,
    "discountPercent": 24,
    "imageUrl": "https://...",
    "images": ["https://...", "https://..."],
    "description": "편안한 핏의 옥스포드 셔츠입니다",
    "sizes": ["S", "M", "L", "XL"],
    "colors": ["화이트", "블루", "핑크"],
    "purchaseUrl": "https://...",
    "isActive": true
  }
}
```

---

### GET /api/brands

브랜드 목록 조회

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "무신사 스탠다드",
      "logoUrl": "https://..."
    },
    {
      "id": 2,
      "name": "리바이스",
      "logoUrl": "https://..."
    }
  ]
}
```

---

## 5. 사용자 API

### GET /api/users/me

내 정보 조회 (회원 전용)

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "phone": "010-1234-5678",
    "profileImageUrl": null,
    "role": "USER",
    "createdAt": "2024-01-01T00:00:00"
  }
}
```

---

### GET /api/outfits/saved

저장된 코디 목록 조회 (회원 전용)

**Query Parameters**
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 10)

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "recommendation": {
          "id": 1,
          "totalPrice": 250000,
          "style": "캐주얼 시크",
          "itemCount": 4,
          "thumbnailUrl": "https://..."
        },
        "note": "출근룩으로 좋을 듯",
        "savedAt": "2024-01-15T11:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

### DELETE /api/outfits/saved/{outfitId}

저장된 코디 삭제 (회원 전용)

**Response** `200 OK`
```json
{
  "success": true,
  "data": null,
  "message": "삭제되었습니다"
}
```

---

## 에러 응답 형식

```json
{
  "success": false,
  "code": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다",
  "timestamp": "2024-01-15T10:30:00"
}
```

## 공통 에러 코드

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `INVALID_INPUT` | 400 | 입력값 유효성 검사 실패 |
| `UNAUTHORIZED` | 401 | 인증 필요 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `NOT_FOUND` | 404 | 리소스 없음 |
| `CONFLICT` | 409 | 충돌 (중복 등) |
| `INTERNAL_ERROR` | 500 | 서버 에러 |
