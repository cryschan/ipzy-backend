package com.ipzy.domain.product.repository;

import com.ipzy.domain.product.entity.Product;
import com.ipzy._global.common.enums.ClothingCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findByBrandId(Long brandId);

    List<Product> findByCategory(ClothingCategory category);

    List<Product> findByPrimaryStyle(String primaryStyle);

    List<Product> findByIsActiveTrue();

    boolean existsByNameAndBrandId(String name, Long brandId);

    /**
     * 특정 브랜드의 상품 중 주어진 상품명 목록에 해당하는 상품명들을 조회 (중복 체크용)
     *
     * 삭제되지 않은 상품만 중복으로 체크합니다.
     * - deletedAt IS NULL: 삭제된 상품은 재크롤링 허용
     *
     * @param brandId 브랜드 ID
     * @param names 상품명 목록
     * @return 이미 존재하는 상품명 Set (삭제되지 않은 상품만)
     */
    @Query("SELECT p.name FROM Product p WHERE p.brand.id = :brandId AND p.name IN :names AND p.deletedAt IS NULL")
    Set<String> findExistingNamesByBrandIdAndNameIn(@Param("brandId") Long brandId,
                                                      @Param("names") List<String> names);
}
