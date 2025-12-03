# 08. 로깅 플로우

## ERD

```mermaid
erDiagram
    users ||--o{ activity_logs : generates
    users ||--o{ admin_audit_logs : performs

    activity_logs {
        bigint id PK
        bigint user_id FK "nullable"
        varchar activity_type "LOGIN/QUIZ_START/..."
        jsonb metadata
        varchar ip_address
        varchar user_agent
        timestamp created_at
    }

    admin_audit_logs {
        bigint id PK
        bigint admin_id FK
        varchar action "CREATE/UPDATE/DELETE"
        varchar target_type "USER/PRODUCT/..."
        bigint target_id
        jsonb before_data
        jsonb after_data
        varchar ip_address
        varchar reason
        timestamp created_at
    }
```

## 사용자 활동 로그

```mermaid
flowchart TB
    subgraph 사용자_활동["Activity Types"]
        direction TB

        subgraph 인증["인증"]
            LOGIN[LOGIN]
            LOGOUT[LOGOUT]
            SIGNUP[SIGNUP]
        end

        subgraph 퀴즈["퀴즈"]
            QS[QUIZ_START]
            QC[QUIZ_COMPLETE]
            QA[QUIZ_ABANDON]
        end

        subgraph 추천["추천"]
            RV[RECOMMENDATION_VIEW]
            RR[RECOMMENDATION_REFRESH]
        end

        subgraph 저장["저장"]
            OS[OUTFIT_SAVE]
            OU[OUTFIT_UNSAVE]
        end

        subgraph 상품["상품"]
            PV[PRODUCT_VIEW]
            PC[PRODUCT_CLICK]
        end

        subgraph 프로필["프로필"]
            PU[PROFILE_UPDATE]
            PWC[PASSWORD_CHANGE]
        end
    end

    사용자_활동 --> AL[(activity_logs)]

    style AL fill:#D3D3D3
```

## 관리자 감사 로그

```mermaid
flowchart TB
    subgraph 관리자_액션["Admin Actions"]
        direction TB

        subgraph CRUD["CRUD"]
            CREATE[CREATE]
            UPDATE[UPDATE]
            DELETE[DELETE]
            SD[SOFT_DELETE]
            RESTORE[RESTORE]
        end

        subgraph 상태["상태 변경"]
            ACT[ACTIVATE]
            DEACT[DEACTIVATE]
        end

        subgraph 사용자관리["사용자 관리"]
            US[USER_SUSPEND]
            UU[USER_UNSUSPEND]
            UD[USER_DELETE]
        end

        subgraph 권한["권한"]
            GR[GRANT_ROLE]
            RR[REVOKE_ROLE]
        end
    end

    관리자_액션 --> AAL[(admin_audit_logs)]

    style AAL fill:#D3D3D3
```

## 감사 로그 상세 예시

```mermaid
sequenceDiagram
    participant A as Admin
    participant P as products
    participant AAL as admin_audit_logs

    A->>P: 상품 가격 수정 요청

    Note over AAL: before_data 저장<br/>{"price": 59000}

    P->>P: price: 59000 → 49000

    Note over AAL: after_data 저장<br/>{"price": 49000}

    P->>AAL: 감사 로그 생성

    Note over AAL: action: UPDATE<br/>target_type: PRODUCT<br/>target_id: 123<br/>reason: "할인 이벤트"
```

## 로그 데이터 예시

### activity_logs

```json
{
  "id": 1,
  "user_id": 100,
  "activity_type": "QUIZ_COMPLETE",
  "metadata": {
    "quiz_id": 1,
    "session_id": 50,
    "duration_seconds": 180
  },
  "ip_address": "192.168.1.1",
  "user_agent": "Mozilla/5.0...",
  "created_at": "2024-01-15T10:30:00"
}
```

### admin_audit_logs

```json
{
  "id": 1,
  "admin_id": 1,
  "action": "UPDATE",
  "target_type": "PRODUCT",
  "target_id": 123,
  "before_data": {"price": 59000, "is_active": true},
  "after_data": {"price": 49000, "is_active": true},
  "ip_address": "10.0.0.1",
  "reason": "연말 할인 이벤트 적용",
  "created_at": "2024-01-15T09:00:00"
}
```
