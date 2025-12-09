package com.ipzy.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 무신사 PLP API 응답 DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MusinsaPlpResponse {

    private DataWrapper data;
    private MetaWrapper meta;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataWrapper {
        private List<ProductItem> list;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetaWrapper {
        private String result;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductItem {
        @JsonProperty("goodsNo")
        private Long goodsNo;

        @JsonProperty("goodsName")
        private String goodsName;

        @JsonProperty("goodsLinkUrl")
        private String goodsLinkUrl;

        @JsonProperty("thumbnail")
        private String thumbnail;

        @JsonProperty("brand")
        private String brand;

        @JsonProperty("brandName")
        private String brandName;

        @JsonProperty("displayGenderText")
        private String displayGenderText;

        @JsonProperty("normalPrice")
        private Integer normalPrice;

        @JsonProperty("price")
        private Integer price;

        @JsonProperty("saleRate")
        private Integer saleRate;

        @JsonProperty("couponPrice")
        private Integer couponPrice;

        @JsonProperty("couponSaleRate")
        private Integer couponSaleRate;

        @JsonProperty("reviewCount")
        private Integer reviewCount;

        @JsonProperty("reviewScore")
        private Integer reviewScore;

        @JsonProperty("isSoldOut")
        private Boolean isSoldOut;

        @JsonProperty("isPlusDelivery")
        private Boolean isPlusDelivery;
    }
}
