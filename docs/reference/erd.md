# 도메인 관계도 (ERD)

> CLAUDE.md에서 분리된 상세 엔티티 관계 문서

## 엔티티 관계 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                            User                                  │
├─────────────────────────────────────────────────────────────────┤
│ id: Long (PK)                                                    │
│ email: String (unique)                                           │
│ name: String                                                     │
│ phone: String                                                    │
│ provider: String (kakao, google, naver)                          │
│ providerId: String                                               │
│ role: UserRole (USER, ADMIN)                                     │
│ status: UserStatus (ACTIVE, SUSPENDED, DELETED)                  │
│ profileImageUrl: String                                          │
│ preferences: Map<String, Object> (JSONB)                         │
│ stylePreference: UserStylePreference (Embedded)                  │
│ lastLoginAt: LocalDateTime                                       │
│ deletedAt: LocalDateTime                                         │
│ createdAt, modifiedAt: LocalDateTime (BaseEntity)                │
└────────┬──────────────────────────────────────────────────┬──────┘
         │ 1:N                                              │ 1:N
         ▼                                                  ▼
┌────────────────────────┐                    ┌─────────────────────────┐
│      QuizSession       │                    │      Subscription       │
├────────────────────────┤                    ├─────────────────────────┤
│ id: Long (PK)          │                    │ id: Long (PK)           │
│ user_id: Long (FK)     │                    │ user_id: Long (FK)      │
│ quiz_id: Long (FK)     │                    │ plan_id: Long (FK)      │
│ completed: Boolean     │                    │ status: SubscriptionStatus│
│ createdAt, modifiedAt  │                    │ startDate, endDate      │
└────────┬───────────────┘                    │ autoRenew: Boolean      │
         │ 1:N                                │ cancelledAt             │
         ▼                                    └───────────┬─────────────┘
┌────────────────────────┐                                │ N:1
│      QuizAnswer        │                                ▼
├────────────────────────┤                    ┌─────────────────────────┐
│ id: Long (PK)          │                    │    SubscriptionPlan     │
│ session_id: Long (FK)  │                    ├─────────────────────────┤
│ question_id: Long (FK) │                    │ id: Long (PK)           │
│ selectedOptions: List  │                    │ name: String            │
└────────────────────────┘                    │ price: Integer          │
                                              │ durationMonths: Integer │
         ▲                                    │ dailyRecommendLimit     │
         │ N:1                                └─────────────────────────┘
┌────────────────────────┐
│     QuizQuestion       │                    ┌─────────────────────────┐
├────────────────────────┤                    │        Payment          │
│ id: Long (PK)          │                    ├─────────────────────────┤
│ quiz_id: Long (FK)     │                    │ id: Long (PK)           │
│ text: String           │                    │ user_id: Long (FK)      │
│ questionType           │                    │ subscription_id: Long   │
│ displayOrder: Integer  │                    │ amount: Integer         │
│ required: Boolean      │                    │ method: PaymentMethod   │
└────────┬───────────────┘                    │ status: PaymentStatus   │
         │ N:1                                │ transactionId: String   │
         ▼                                    └─────────────────────────┘
┌────────────────────────┐
│         Quiz           │
├────────────────────────┤
│ id: Long (PK)          │
│ title: String          │
│ description: String    │
└────────────────────────┘


┌────────────────────────┐                    ┌─────────────────────────┐
│    Recommendation      │──────1:N──────────▶│   RecommendationItem    │
├────────────────────────┤                    ├─────────────────────────┤
│ id: Long (PK)          │                    │ id: Long (PK)           │
│ session_id: Long (FK)  │                    │ recommendation_id (FK)  │
│ user_id: Long (FK)     │                    │ product_id: Long (FK)   │
│ occasion: String       │                    │ category: ClothingCategory│
│ season: String         │                    │ productName: String     │
│ style: String          │                    │ brandName: String       │
│ reason: String         │                    │ price: Integer          │
│ totalPrice: Integer    │                    │ imageUrl: String        │
│ displayOrder: Integer  │                    │ linkUrl: String         │
│ styleBoardUrl: String  │                    └───────────┬─────────────┘
└────────────────────────┘                                │ N:1
                                                          ▼
                                              ┌─────────────────────────┐
                                              │        Product          │
                                              ├─────────────────────────┤
                                              │ id: Long (PK)           │
                                              │ brand_id: Long (FK)     │
                                              │ name: String            │
                                              │ category: ClothingCategory│
                                              │ price: Integer          │
                                              │ originalPrice: Integer  │
                                              │ colors: String[]        │
                                              │ seasons: String[]       │
                                              │ imageUrl: String        │
                                              │ linkUrl: String         │
                                              │ crawledAt: LocalDateTime│
                                              └───────────┬─────────────┘
                                                          │ N:1
                                                          ▼
                                              ┌─────────────────────────┐
                                              │         Brand           │
                                              ├─────────────────────────┤
                                              │ id: Long (PK)           │
                                              │ name: String (unique)   │
                                              │ englishName: String     │
                                              │ logoUrl: String         │
                                              │ styles: String[]        │
                                              │ priceRange: PriceRange  │
                                              └─────────────────────────┘
```

---

## 주요 비즈니스 플로우

### 1. 추천 생성 플로우
```
User → QuizSession (생성) → QuizAnswer[] (답변) → QuizSession.complete()
     → Python AI 요청 → Recommendation + RecommendationItem[] (저장)
```

### 2. OAuth2 인증 플로우
```
OAuth2 Provider → CustomOAuth2UserService.loadUser()
               → UserService.findOrCreateByOAuth()
               → CustomUserPrincipal (Spring Security 등록)
```

### 3. 구독/결제 플로우
```
User → SubscriptionPlan (선택) → Subscription (생성)
     → Payment (결제 처리) → Subscription.status = ACTIVE
```

---

## 엔티티 소스 코드 위치

| 엔티티 | 경로 |
|--------|------|
| User | `domain/user/entity/User.java` |
| QuizSession | `domain/quiz/entity/QuizSession.java` |
| Quiz, QuizQuestion, QuizAnswer | `domain/quiz/entity/` |
| Recommendation, RecommendationItem | `domain/recommendation/entity/` |
| Product, Brand | `domain/product/entity/` |
| Subscription, SubscriptionPlan | `domain/subscription/entity/` |
| Payment | `domain/payment/entity/` |
