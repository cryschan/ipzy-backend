package com.ipzy.domain.product.repository;

import com.ipzy.domain.product.entity.Product;
import com.ipzy.global.common.enums.ClothingCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByBrandId(Long brandId);

    List<Product> findByCategory(ClothingCategory category);

    List<Product> findByPrimaryStyle(String primaryStyle);

    List<Product> findByIsActiveTrue();

    boolean existsByNameAndBrandId(String name, Long brandId);
}
