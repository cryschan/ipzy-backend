-- =============================================
-- IPZY Full Database Schema
-- PostgreSQL 15+
-- 총 16개 테이블
-- =============================================

-- =============================================
-- 1. 사용자 (User)
-- =============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(500),
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    preferences JSONB DEFAULT '{}',
    last_login_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_users_provider_provider_id UNIQUE (provider, provider_id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_provider CHECK (provider IN ('KAKAO', 'NAVER', 'GOOGLE')),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_provider ON users(provider);
CREATE INDEX idx_users_status ON users(status);

-- =============================================
-- 2. 퀴즈 (Quiz)
-- =============================================
CREATE TABLE quizzes (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_quizzes_is_active ON quizzes(is_active);
CREATE INDEX idx_quizzes_display_order ON quizzes(display_order);

-- =============================================
-- 3. 퀴즈 질문 (QuizQuestion)
-- =============================================
CREATE TABLE quiz_questions (
    id BIGSERIAL PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    text VARCHAR(500) NOT NULL,
    type VARCHAR(20) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    required BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_quiz_questions_quiz FOREIGN KEY (quiz_id)
        REFERENCES quizzes(id) ON DELETE CASCADE,
    CONSTRAINT chk_quiz_questions_type CHECK (type IN ('SINGLE', 'MULTIPLE'))
);

CREATE INDEX idx_quiz_questions_quiz_id ON quiz_questions(quiz_id);

-- =============================================
-- 4. 퀴즈 옵션 (QuizOption)
-- =============================================
CREATE TABLE quiz_options (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    text VARCHAR(200) NOT NULL,
    value VARCHAR(100) NOT NULL,
    image_url VARCHAR(500),
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_quiz_options_question FOREIGN KEY (question_id)
        REFERENCES quiz_questions(id) ON DELETE CASCADE
);

CREATE INDEX idx_quiz_options_question_id ON quiz_options(question_id);

-- =============================================
-- 5. 퀴즈 세션 (QuizSession)
-- =============================================
CREATE TABLE quiz_sessions (
    id BIGSERIAL PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_quiz_sessions_quiz FOREIGN KEY (quiz_id)
        REFERENCES quizzes(id) ON DELETE CASCADE,
    CONSTRAINT fk_quiz_sessions_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_quiz_sessions_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED'))
);

CREATE INDEX idx_quiz_sessions_user_id ON quiz_sessions(user_id);
CREATE INDEX idx_quiz_sessions_quiz_id ON quiz_sessions(quiz_id);
CREATE INDEX idx_quiz_sessions_status ON quiz_sessions(status);

-- =============================================
-- 6. 퀴즈 답변 (QuizAnswer)
-- =============================================
CREATE TABLE quiz_answers (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_options VARCHAR(100)[] NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_quiz_answers_session FOREIGN KEY (session_id)
        REFERENCES quiz_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_quiz_answers_question FOREIGN KEY (question_id)
        REFERENCES quiz_questions(id) ON DELETE CASCADE,
    CONSTRAINT uk_quiz_answers_session_question UNIQUE (session_id, question_id)
);

CREATE INDEX idx_quiz_answers_session_id ON quiz_answers(session_id);

-- =============================================
-- 7. 브랜드 (Brand)
-- =============================================
CREATE TABLE brands (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    logo_url VARCHAR(500),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_brands_name UNIQUE (name)
);

CREATE INDEX idx_brands_is_active ON brands(is_active);

-- =============================================
-- 8. 상품 (Product)
-- =============================================
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    brand_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    sub_category VARCHAR(50),
    price INTEGER NOT NULL,
    original_price INTEGER,
    discount_percent INTEGER DEFAULT 0,
    image_url VARCHAR(500) NOT NULL,
    images VARCHAR(500)[],
    description TEXT,
    sizes VARCHAR(20)[],
    colors VARCHAR(50)[],
    tags VARCHAR(50)[],
    stock INTEGER NOT NULL DEFAULT 0,
    purchase_url VARCHAR(500) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id)
        REFERENCES brands(id) ON DELETE RESTRICT,
    CONSTRAINT chk_products_category CHECK (category IN ('TOP', 'BOTTOM', 'OUTER', 'SHOES', 'ACCESSORY')),
    CONSTRAINT chk_products_price CHECK (price >= 0),
    CONSTRAINT chk_products_stock CHECK (stock >= 0),
    CONSTRAINT chk_products_discount CHECK (discount_percent IS NULL OR (discount_percent >= 0 AND discount_percent <= 100))
);

CREATE INDEX idx_products_brand_id ON products(brand_id);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_is_active ON products(is_active);
CREATE INDEX idx_products_tags ON products USING GIN(tags);

-- =============================================
-- 9. 구독 플랜 (SubscriptionPlan)
-- =============================================
CREATE TABLE subscription_plans (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    price INTEGER NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'KRW',
    billing_period VARCHAR(20),
    features JSONB DEFAULT '{}',
    description VARCHAR(500),
    badge VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_subscription_plans_name UNIQUE (name),
    CONSTRAINT chk_subscription_plans_billing_period CHECK (billing_period IN ('MONTHLY', 'YEARLY')),
    CONSTRAINT chk_subscription_plans_price CHECK (price >= 0)
);

CREATE INDEX idx_subscription_plans_is_active ON subscription_plans(is_active);

-- =============================================
-- 10. 구독 (Subscription)
-- =============================================
CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    auto_renew BOOLEAN NOT NULL DEFAULT true,
    cancelled_at TIMESTAMP,
    cancel_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_subscriptions_plan FOREIGN KEY (plan_id)
        REFERENCES subscription_plans(id) ON DELETE RESTRICT,
    CONSTRAINT chk_subscriptions_status CHECK (status IN ('PENDING', 'ACTIVE', 'CANCELLED', 'EXPIRED'))
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_plan_id ON subscriptions(plan_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);

-- =============================================
-- 11. 결제 (Payment)
-- =============================================
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT,
    amount INTEGER NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'KRW',
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description VARCHAR(500),
    transaction_id VARCHAR(100),
    receipt_url VARCHAR(500),
    failure_reason VARCHAR(500),
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payments_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_payments_subscription FOREIGN KEY (subscription_id)
        REFERENCES subscriptions(id) ON DELETE SET NULL,
    CONSTRAINT chk_payments_method CHECK (method IN ('CARD', 'BANK_TRANSFER', 'KAKAO_PAY', 'NAVER_PAY', 'TOSS_PAY')),
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED', 'CANCELLED')),
    CONSTRAINT chk_payments_amount CHECK (amount >= 0)
);

CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_subscription_id ON payments(subscription_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);

-- =============================================
-- 12. AI 추천 (Recommendation)
-- =============================================
CREATE TABLE recommendations (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 1,
    total_price INTEGER NOT NULL,
    style VARCHAR(100),
    occasion VARCHAR(100),
    season VARCHAR(50),
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_recommendations_session FOREIGN KEY (session_id)
        REFERENCES quiz_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_recommendations_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_recommendations_total_price CHECK (total_price >= 0)
);

CREATE INDEX idx_recommendations_session_id ON recommendations(session_id);
CREATE INDEX idx_recommendations_user_id ON recommendations(user_id);

-- =============================================
-- 13. 추천 아이템 (RecommendationItem)
-- =============================================
CREATE TABLE recommendation_items (
    id BIGSERIAL PRIMARY KEY,
    recommendation_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    price_snapshot INTEGER NOT NULL,
    product_name_snapshot VARCHAR(200) NOT NULL,
    image_url_snapshot VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_recommendation_items_recommendation FOREIGN KEY (recommendation_id)
        REFERENCES recommendations(id) ON DELETE CASCADE,
    CONSTRAINT fk_recommendation_items_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT chk_recommendation_items_category CHECK (category IN ('TOP', 'BOTTOM', 'OUTER', 'SHOES', 'ACCESSORY'))
);

CREATE INDEX idx_recommendation_items_recommendation_id ON recommendation_items(recommendation_id);
CREATE INDEX idx_recommendation_items_product_id ON recommendation_items(product_id);

-- =============================================
-- 14. 저장된 코디 (SavedOutfit)
-- =============================================
CREATE TABLE saved_outfits (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recommendation_id BIGINT NOT NULL,
    note VARCHAR(500),
    saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_saved_outfits_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_outfits_recommendation FOREIGN KEY (recommendation_id)
        REFERENCES recommendations(id) ON DELETE CASCADE,
    CONSTRAINT uk_saved_outfits_user_recommendation UNIQUE (user_id, recommendation_id)
);

CREATE INDEX idx_saved_outfits_user_id ON saved_outfits(user_id);
CREATE INDEX idx_saved_outfits_recommendation_id ON saved_outfits(recommendation_id);

-- =============================================
-- 15. 활동 로그 (ActivityLog)
-- =============================================
CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    activity_type VARCHAR(30) NOT NULL,
    metadata JSONB DEFAULT '{}',
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_activity_logs_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_activity_logs_type CHECK (activity_type IN (
        'LOGIN', 'LOGOUT', 'SIGNUP',
        'QUIZ_START', 'QUIZ_COMPLETE', 'QUIZ_ABANDON',
        'RECOMMENDATION_VIEW', 'RECOMMENDATION_REFRESH',
        'OUTFIT_SAVE', 'OUTFIT_UNSAVE',
        'PRODUCT_VIEW', 'PRODUCT_CLICK',
        'PROFILE_UPDATE', 'PASSWORD_CHANGE'
    ))
);

CREATE INDEX idx_activity_logs_user_id ON activity_logs(user_id);
CREATE INDEX idx_activity_logs_activity_type ON activity_logs(activity_type);
CREATE INDEX idx_activity_logs_created_at ON activity_logs(created_at);

-- =============================================
-- 16. 관리자 감사 로그 (AdminAuditLog)
-- =============================================
CREATE TABLE admin_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT,
    before_data JSONB DEFAULT '{}',
    after_data JSONB DEFAULT '{}',
    ip_address VARCHAR(45),
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_admin_audit_logs_admin FOREIGN KEY (admin_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_admin_audit_logs_action CHECK (action IN (
        'CREATE', 'UPDATE', 'DELETE', 'SOFT_DELETE', 'RESTORE',
        'ACTIVATE', 'DEACTIVATE',
        'USER_SUSPEND', 'USER_UNSUSPEND', 'USER_DELETE',
        'GRANT_ROLE', 'REVOKE_ROLE'
    )),
    CONSTRAINT chk_admin_audit_logs_target_type CHECK (target_type IN (
        'USER', 'QUIZ', 'QUIZ_QUESTION', 'QUIZ_OPTION',
        'BRAND', 'PRODUCT',
        'SUBSCRIPTION_PLAN', 'SUBSCRIPTION', 'PAYMENT',
        'RECOMMENDATION'
    ))
);

CREATE INDEX idx_admin_audit_logs_admin_id ON admin_audit_logs(admin_id);
CREATE INDEX idx_admin_audit_logs_action ON admin_audit_logs(action);
CREATE INDEX idx_admin_audit_logs_target_type ON admin_audit_logs(target_type);
CREATE INDEX idx_admin_audit_logs_created_at ON admin_audit_logs(created_at);

-- =============================================
-- 트리거: modified_at 자동 업데이트
-- =============================================
CREATE OR REPLACE FUNCTION update_modified_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.modified_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 각 테이블에 트리거 적용 (activity_logs, admin_audit_logs 제외 - modified_at 없음)
CREATE TRIGGER update_users_modified_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();

CREATE TRIGGER update_quizzes_modified_at BEFORE UPDATE ON quizzes
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();

CREATE TRIGGER update_quiz_questions_modified_at BEFORE UPDATE ON quiz_questions
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();

CREATE TRIGGER update_quiz_options_modified_at BEFORE UPDATE ON quiz_options
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();

CREATE TRIGGER update_quiz_sessions_modified_at BEFORE UPDATE ON quiz_sessions
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();

CREATE TRIGGER update_quiz_answers_modified_at BEFORE UPDATE ON quiz_answers
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();

CREATE TRIGGER update_brands_modified_at BEFORE UPDATE ON brands
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();

CREATE TRIGGER update_products_modified_at BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();

CREATE TRIGGER update_subscription_plans_modified_at BEFORE UPDATE ON subscription_plans
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();

CREATE TRIGGER update_subscriptions_modified_at BEFORE UPDATE ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();

CREATE TRIGGER update_payments_modified_at BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();

CREATE TRIGGER update_recommendations_modified_at BEFORE UPDATE ON recommendations
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();

CREATE TRIGGER update_recommendation_items_modified_at BEFORE UPDATE ON recommendation_items
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();

CREATE TRIGGER update_saved_outfits_modified_at BEFORE UPDATE ON saved_outfits
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();
