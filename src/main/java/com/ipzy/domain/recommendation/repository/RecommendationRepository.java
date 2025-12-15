package com.ipzy.domain.recommendation.repository;

import com.ipzy.domain.recommendation.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    /**
     * 세션 ID로 추천 목록 조회 (아이템 포함)
     */
    @Query("""
                SELECT DISTINCT r FROM Recommendation r
                LEFT JOIN FETCH r.items
                WHERE r.session.id = :sessionId
                ORDER BY r.displayOrder ASC
            """)
    List<Recommendation> findBySessionIdWithItems(Long sessionId);

    /**
     * 사용자 ID로 추천 목록 조회
     */
    @Query("""
                SELECT DISTINCT r FROM Recommendation r
                LEFT JOIN FETCH r.items
                WHERE r.user.id = :userId
                ORDER BY r.createdAt DESC
            """)
    List<Recommendation> findByUserIdWithItems(Long userId);

    /**
     * 추천 ID로 조회 (아이템 포함)
     */
    @Query("""
                SELECT r FROM Recommendation r
                LEFT JOIN FETCH r.items
                WHERE r.id = :recommendationId
            """)
    Optional<Recommendation> findByIdWithItems(Long recommendationId);

    /**
     * 세션에 이미 추천이 있는지 확인
     */
    boolean existsBySessionId(Long sessionId);
}
