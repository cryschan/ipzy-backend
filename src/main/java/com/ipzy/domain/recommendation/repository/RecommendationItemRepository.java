package com.ipzy.domain.recommendation.repository;

import com.ipzy.domain.recommendation.entity.RecommendationItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationItemRepository extends JpaRepository<RecommendationItem, Long> {
    // Cascade로 대부분 처리되므로 기본 CRUD만 제공
}
