package com.ipzy.domain.product.repository;

import com.ipzy.domain.product.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findByNameAndBrandType(String name, String brandType);

    List<Brand> findByPrimaryStyle(String primaryStyle);

    boolean existsByNameAndBrandType(String name, String brandType);
}
