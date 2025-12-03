-- =============================================
-- IPZY MVP Database Schema
-- PostgreSQL 15+
-- =============================================

-- =============================================
-- 1. 사용자 (User)
-- =============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(500),
    provider VARCHAR(20) NOT NULL,           -- KAKAO, NAVER, GOOGLE
    provider_id VARCHAR(255) NOT NULL,       -- OAuth2 provider 사용자 ID
    role VARCHAR(20) NOT NULL DEFAULT 'USER', -- USER, ADMIN
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_users_provider_provider_id UNIQUE (provider, provider_id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_provider ON users(provider);

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

-- =============================================
-- 3. 퀴즈 질문 (QuizQuestion)
-- =============================================
CREATE TABLE quiz_questions (
    id BIGSERIAL PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    text VARCHAR(500) NOT NULL,
    type VARCHAR(20) NOT NULL,               -- SINGLE, MULTIPLE
    display_order INTEGER NOT NULL DEFAULT 0,
    required BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_quiz_questions_quiz FOREIGN KEY (quiz_id)
        REFERENCES quizzes(id) ON DELETE CASCADE
);

CREATE INDEX idx_quiz_questions_quiz_id ON quiz_questions(quiz_id);

-- =============================================
-- 4. 퀴즈 옵션 (QuizOption)
-- =============================================
CREATE TABLE quiz_options (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    text VARCHAR(200) NOT NULL,
    value VARCHAR(100) NOT NULL,             -- 옵션 값 (casual, formal 등)
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
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, COMPLETED, ABANDONED
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_quiz_sessions_quiz FOREIGN KEY (quiz_id)
        REFERENCES quizzes(id) ON DELETE CASCADE,
    CONSTRAINT fk_quiz_sessions_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
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
    selected_options VARCHAR(500)[] NOT NULL, -- PostgreSQL Array (선택된 옵션 values)
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
    category VARCHAR(50) NOT NULL,           -- TOP, BOTTOM, OUTER, SHOES, ACCESSORY
    sub_category VARCHAR(50),                -- 셔츠, 니트, 팬츠 등
    price INTEGER NOT NULL,
    original_price INTEGER,
    discount_percent INTEGER,
    image_url VARCHAR(500) NOT NULL,
    images VARCHAR(500)[],                   -- PostgreSQL Array
    description TEXT,
    sizes VARCHAR(20)[],                     -- PostgreSQL Array (S, M, L, XL)
    colors VARCHAR(50)[],                    -- PostgreSQL Array (화이트, 블루 등)
    tags VARCHAR(50)[],                      -- PostgreSQL Array (캐주얼, 오버핏 등)
    purchase_url VARCHAR(500) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id)
        REFERENCES brands(id) ON DELETE RESTRICT
);

CREATE INDEX idx_products_brand_id ON products(brand_id);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_is_active ON products(is_active);
CREATE INDEX idx_products_tags ON products USING GIN(tags);

-- =============================================
-- 9. AI 추천 (Recommendation)
-- =============================================
CREATE TABLE recommendations (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 1,
    total_price INTEGER NOT NULL,
    style VARCHAR(100),                      -- 캐주얼 시크, 스트릿 캐주얼 등
    occasion VARCHAR(100),                   -- 데일리, 출근 등
    season VARCHAR(50),                      -- 봄/가을, 여름 등
    reason TEXT,                             -- AI 추천 이유
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_recommendations_session FOREIGN KEY (session_id)
        REFERENCES quiz_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_recommendations_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_recommendations_session_id ON recommendations(session_id);
CREATE INDEX idx_recommendations_user_id ON recommendations(user_id);

-- =============================================
-- 10. 추천 아이템 (RecommendationItem)
-- =============================================
CREATE TABLE recommendation_items (
    id BIGSERIAL PRIMARY KEY,
    recommendation_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,           -- TOP, BOTTOM, OUTER, SHOES, ACCESSORY
    display_order INTEGER NOT NULL DEFAULT 0,
    price_snapshot INTEGER NOT NULL,         -- 추천 시점의 가격 스냅샷
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_recommendation_items_recommendation FOREIGN KEY (recommendation_id)
        REFERENCES recommendations(id) ON DELETE CASCADE,
    CONSTRAINT fk_recommendation_items_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE RESTRICT
);

CREATE INDEX idx_recommendation_items_recommendation_id ON recommendation_items(recommendation_id);
CREATE INDEX idx_recommendation_items_product_id ON recommendation_items(product_id);

-- =============================================
-- 11. 저장된 코디 (SavedOutfit)
-- =============================================
CREATE TABLE saved_outfits (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recommendation_id BIGINT NOT NULL,
    note VARCHAR(500),                       -- 사용자 메모
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
-- 트리거: modified_at 자동 업데이트
-- =============================================
CREATE OR REPLACE FUNCTION update_modified_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.modified_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 각 테이블에 트리거 적용
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

CREATE TRIGGER update_recommendations_modified_at BEFORE UPDATE ON recommendations
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();

CREATE TRIGGER update_recommendation_items_modified_at BEFORE UPDATE ON recommendation_items
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();

CREATE TRIGGER update_saved_outfits_modified_at BEFORE UPDATE ON saved_outfits
    FOR EACH ROW EXECUTE FUNCTION update_modified_at_column();
