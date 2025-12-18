-- ==========================================
-- Partial Unique Index 생성
-- ACTIVE 또는 PENDING 상태는 사용자당 하나만 허용
-- ==========================================

-- 기존 중복 데이터가 있다면 먼저 정리 (최신 것만 유지)
UPDATE subscriptions s1
SET
    status = 'CANCELLED',
    cancel_reason = 'System: Duplicate cleanup before unique constraint',
    cancelled_at = NOW()
WHERE s1.status IN ('ACTIVE', 'PENDING')
  AND EXISTS (
    SELECT 1 FROM subscriptions s2
    WHERE s2.user_id = s1.user_id
      AND s2.status IN ('ACTIVE', 'PENDING')
      AND s2.created_at > s1.created_at
  );

-- Partial Unique Index 생성
-- ACTIVE/PENDING만 user_id당 1개 제한, EXPIRED/CANCELLED는 여러 개 가능
CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_user_valid_status
ON subscriptions (user_id)
WHERE status IN ('ACTIVE', 'PENDING');

-- 인덱스 설명 추가
COMMENT ON INDEX uk_subscription_user_valid_status IS
'사용자당 하나의 유효한 구독(ACTIVE 또는 PENDING)만 허용. EXPIRED/CANCELLED는 히스토리로 여러 개 가능.';