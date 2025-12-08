package com.ipzy.domain.product.entity;

import com.ipzy.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "brands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "primary_style", length = 50)
    private String primaryStyle;

    @Builder
    public Brand(String name, String logoUrl, String primaryStyle) {
        this.name = name;
        this.logoUrl = logoUrl;
        this.primaryStyle = primaryStyle;
    }

    public void update(String name, String logoUrl, String primaryStyle) {
        this.name = name;
        this.logoUrl = logoUrl;
        this.primaryStyle = primaryStyle;
    }
}
