package com.ipzy.domain.admin.controller;

import com.ipzy._global.common.ApiResponse;
import com.ipzy.domain.admin.dto.AdminProductDetailResponse;
import com.ipzy.domain.admin.dto.AdminProductResponse;
import com.ipzy.domain.admin.dto.AdminProductSearchRequest;
import com.ipzy.domain.admin.exception.AdminException;
import com.ipzy.domain.admin.service.AdminAuthService;
import com.ipzy.domain.admin.service.AdminProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Product", description = "관리자용 상품 관리 API")
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final AdminAuthService adminAuthService;

    @Operation(summary = "상품 목록 조회", description = "검색 조건에 따른 상품 목록 페이징 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "content": [
                          {
                            "id": 1,
                            "brandName": "무신사 스탠다드",
                            "name": "릴렉스드 핏 크루넥 스웨터",
                            "category": "TOP",
                            "price": 39900,
                            "originalPrice": 49900,
                            "discountPercent": 20,
                            "isActive": true,
                            "imageUrl": "https://example.com/image.jpg",
                            "createdAt": "2024-01-15T10:30:00"
                          }
                        ],
                        "totalElements": 500,
                        "totalPages": 25
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "로그인 필요",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_005",
                        "message": "로그인이 필요합니다"
                      }
                    }
                    """)
            )
        )
    })
    @GetMapping
    public ApiResponse<Page<AdminProductResponse>> getProducts(
            @ParameterObject @Valid AdminProductSearchRequest request,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpSession session
    ) {
        validateAdminSession(session);
        Page<AdminProductResponse> products = adminProductService.findProducts(request, pageable);
        return ApiResponse.success(products);
    }

    @Operation(summary = "상품 상세 조회", description = "특정 상품의 상세 정보 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "id": 1,
                        "brand": {
                          "id": 1,
                          "name": "MUSINSA_STANDARD",
                          "displayName": "무신사 스탠다드"
                        },
                        "name": "릴렉스드 핏 크루넥 스웨터",
                        "category": "TOP",
                        "subCategory": "니트/스웨터",
                        "primaryStyle": "캐주얼",
                        "price": 39900,
                        "originalPrice": 49900,
                        "discountPercent": 20,
                        "imageUrl": "https://example.com/image.jpg",
                        "isActive": true,
                        "purchaseUrl": "https://musinsa.com/product/123",
                        "createdAt": "2024-01-15T10:30:00"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "로그인 필요",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_005",
                        "message": "로그인이 필요합니다"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "상품을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_012",
                        "message": "상품을 찾을 수 없습니다"
                      }
                    }
                    """)
            )
        )
    })
    @GetMapping("/{productId}")
    public ApiResponse<AdminProductDetailResponse> getProductDetail(
            @Parameter(description = "상품 ID", required = true, example = "1")
            @PathVariable Long productId,
            HttpSession session
    ) {
        validateAdminSession(session);
        AdminProductDetailResponse product = adminProductService.findProductDetail(productId);
        return ApiResponse.success(product);
    }

    @Operation(summary = "상품 활성화", description = "비활성화된 상품을 활성화합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "활성화 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "id": 1,
                        "brand": {
                          "id": 1,
                          "name": "MUSINSA_STANDARD",
                          "displayName": "무신사 스탠다드"
                        },
                        "name": "릴렉스드 핏 크루넥 스웨터",
                        "category": "TOP",
                        "subCategory": "니트/스웨터",
                        "primaryStyle": "캐주얼",
                        "price": 39900,
                        "originalPrice": 49900,
                        "discountPercent": 20,
                        "imageUrl": "https://example.com/image.jpg",
                        "isActive": true,
                        "purchaseUrl": "https://musinsa.com/product/123",
                        "createdAt": "2024-01-15T10:30:00"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "삭제된 상품",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_013",
                        "message": "삭제된 상품은 수정할 수 없습니다"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "로그인 필요",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_005",
                        "message": "로그인이 필요합니다"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "상품을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_012",
                        "message": "상품을 찾을 수 없습니다"
                      }
                    }
                    """)
            )
        )
    })
    @PatchMapping("/{productId}/activate")
    public ApiResponse<AdminProductDetailResponse> activateProduct(
            @Parameter(description = "상품 ID", required = true, example = "1")
            @PathVariable Long productId,
            HttpSession session
    ) {
        Long adminId = validateAdminSession(session);
        AdminProductDetailResponse product = adminProductService.activateProduct(productId, adminId);
        return ApiResponse.success(product);
    }

    @Operation(summary = "상품 비활성화", description = "활성화된 상품을 비활성화합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "비활성화 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "id": 1,
                        "brand": {
                          "id": 1,
                          "name": "MUSINSA_STANDARD",
                          "displayName": "무신사 스탠다드"
                        },
                        "name": "릴렉스드 핏 크루넥 스웨터",
                        "category": "TOP",
                        "subCategory": "니트/스웨터",
                        "primaryStyle": "캐주얼",
                        "price": 39900,
                        "originalPrice": 49900,
                        "discountPercent": 20,
                        "imageUrl": "https://example.com/image.jpg",
                        "isActive": false,
                        "purchaseUrl": "https://musinsa.com/product/123",
                        "createdAt": "2024-01-15T10:30:00"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "삭제된 상품",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_013",
                        "message": "삭제된 상품은 수정할 수 없습니다"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "로그인 필요",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_005",
                        "message": "로그인이 필요합니다"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "상품을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_012",
                        "message": "상품을 찾을 수 없습니다"
                      }
                    }
                    """)
            )
        )
    })
    @PatchMapping("/{productId}/deactivate")
    public ApiResponse<AdminProductDetailResponse> deactivateProduct(
            @Parameter(description = "상품 ID", required = true, example = "1")
            @PathVariable Long productId,
            HttpSession session
    ) {
        Long adminId = validateAdminSession(session);
        AdminProductDetailResponse product = adminProductService.deactivateProduct(productId, adminId);
        return ApiResponse.success(product);
    }

    @Operation(summary = "상품 삭제", description = "상품을 소프트 삭제합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "삭제 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": null
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "이미 삭제된 상품",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_013",
                        "message": "삭제된 상품은 수정할 수 없습니다"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "로그인 필요",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_005",
                        "message": "로그인이 필요합니다"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "상품을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "code": "ADMIN_012",
                        "message": "상품을 찾을 수 없습니다"
                      }
                    }
                    """)
            )
        )
    })
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteProduct(
            @Parameter(description = "상품 ID", required = true, example = "1")
            @PathVariable Long productId,
            HttpSession session
    ) {
        Long adminId = validateAdminSession(session);
        adminProductService.deleteProduct(productId, adminId);
        return ApiResponse.success(null);
    }

    private Long validateAdminSession(HttpSession session) {
        Long adminId = adminAuthService.getAdminIdFromSession(session);
        if (adminId == null) {
            throw AdminException.sessionRequired();
        }
        return adminId;
    }
}
