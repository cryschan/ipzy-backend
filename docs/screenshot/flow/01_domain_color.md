# 01. 도메인별 색상 구분

## 테이블 분류

```mermaid
mindmap
  root((IPZY ERD))
    User
      ::icon(fa fa-user)
      users[users - Yellow]
    Quiz
      ::icon(fa fa-question-circle)
      quizzes[quizzes - Purple]
      quiz_sessions[quiz_sessions - Purple]
      quiz_questions[quiz_questions - Orange]
      quiz_options[quiz_options - Orange]
      quiz_answers[quiz_answers - Orange]
    Product
      ::icon(fa fa-shopping-bag)
      brands[brands - Blue]
      products[products - Green]
    Subscription
      ::icon(fa fa-credit-card)
      subscription_plans[subscription_plans - Teal]
      subscriptions[subscriptions - Teal]
      payments[payments - Teal]
    Recommendation
      ::icon(fa fa-star)
      recommendations[recommendations - Pink]
      recommendation_items[recommendation_items - Pink]
    Outfit
      ::icon(fa fa-heart)
      saved_outfits[saved_outfits - Red]
    Logging
      ::icon(fa fa-chart-line)
      activity_logs[activity_logs - Gray]
      admin_audit_logs[admin_audit_logs - Gray]
```

## 색상 범례

| 색상 | 도메인 | 테이블 수 |
|------|--------|----------|
| Yellow | User | 1 |
| Purple | Quiz Core | 2 |
| Orange | Quiz Detail | 3 |
| Blue | Brand | 1 |
| Green | Product | 1 |
| Teal | Subscription | 3 |
| Pink | Recommendation | 2 |
| Red | Outfit | 1 |
| Gray | Logging | 2 |
| **Total** | | **16** |
