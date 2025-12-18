-- ==========================================
-- 브랜드 초기 데이터 등록
-- docs/product/brand-by-style.md 문서 기준
-- ==========================================

-- 중복 방지: 이미 브랜드가 있으면 스킵
-- Advisory Lock: 여러 인스턴스 동시 실행 방지 (Lock ID: 123456)
DO $$
BEGIN
    -- Advisory Lock 획득 (동시성 제어)
    PERFORM pg_advisory_lock(123456);

    IF (SELECT COUNT(*) FROM brands) = 0 THEN
        -- 의류 브랜드 (CLOTHING) 12개
        INSERT INTO brands (name, logo_url, primary_style, brand_type, created_at, modified_at) VALUES
        ('Outstanding', NULL, 'hip_hop', 'CLOTHING', NOW(), NOW()),
        ('Draw Fit', NULL, 'hip_hop', 'CLOTHING', NOW(), NOW()),
        ('Musinsa Standard', NULL, 'minimalist', 'CLOTHING', NOW(), NOW()),
        ('Dimitri Black', NULL, 'minimalist', 'CLOTHING', NOW(), NOW()),
        ('Trillion', NULL, 'street', 'CLOTHING', NOW(), NOW()),
        ('Espionage', NULL, 'street', 'CLOTHING', NOW(), NOW()),
        ('The North Face', NULL, 'gorpcore', 'CLOTHING', NOW(), NOW()),
        ('National Geographic', NULL, 'gorpcore', 'CLOTHING', NOW(), NOW()),
        ('Frizmworks', NULL, 'amekaji', 'CLOTHING', NOW(), NOW()),
        ('Toffee', NULL, 'amekaji', 'CLOTHING', NOW(), NOW()),
        ('Thisisneverthat', NULL, 'cityboy', 'CLOTHING', NOW(), NOW()),
        ('Suare', NULL, 'cityboy', 'CLOTHING', NOW(), NOW()),

        -- 신발 브랜드 (SHOES) 10개
        ('Nike', NULL, 'sneakers', 'SHOES', NOW(), NOW()),
        ('Adidas', NULL, 'sneakers', 'SHOES', NOW(), NOW()),
        ('Gucci', NULL, 'loafers', 'SHOES', NOW(), NOW()),
        ('Tod''s', NULL, 'loafers', 'SHOES', NOW(), NOW()),
        ('Dr. Martens', NULL, 'boots', 'SHOES', NOW(), NOW()),
        ('Timberland', NULL, 'boots', 'SHOES', NOW(), NOW()),
        ('Clarks', NULL, 'dress_shoes', 'SHOES', NOW(), NOW()),
        ('Cole Haan', NULL, 'dress_shoes', 'SHOES', NOW(), NOW()),
        ('Birkenstock', NULL, 'sandals', 'SHOES', NOW(), NOW()),
        ('Teva', NULL, 'sandals', 'SHOES', NOW(), NOW());

        RAISE NOTICE '브랜드 초기화 완료: 총 22개 (CLOTHING: 12개, SHOES: 10개)';
    ELSE
        RAISE NOTICE '브랜드 데이터가 이미 존재합니다. 초기화를 건너뜁니다.';
    END IF;

    -- Advisory Lock 해제
    PERFORM pg_advisory_unlock(123456);
END $$;
