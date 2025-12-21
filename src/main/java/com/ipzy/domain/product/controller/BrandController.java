package com.ipzy.domain.product.controller;

import com.ipzy.domain.product.dto.BrandRequest;
import com.ipzy.domain.product.dto.BrandResponse;
import com.ipzy.domain.product.dto.BrandValidationResult;
import com.ipzy.domain.product.service.BrandService;
import com.ipzy._global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Brand Management", description = "브랜드 관리 API (관리자용)")
@RestController
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
@Validated
public class BrandController {

    private final BrandService brandService;

    @Operation(
            summary = "브랜드 등록",
            description = "새로운 브랜드를 등록합니다. validateMusinsa=true 시 무신사에 브랜드가 존재하는지 검증합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "브랜드 등록 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 1,
                                        "name": "Musinsa Standard",
                                        "logoUrl": "https://example.com/logo.png",
                                        "primaryStyle": "minimalist",
                                        "brandType": "CLOTHING",
                                        "createdAt": "2025-12-09T11:00:00",
                                        "modifiedAt": "2025-12-09T11:00:00"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "인증 실패 (추후 적용 예정)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "AUTH_001",
                                        "message": "관리자 권한이 필요합니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 존재하는 브랜드",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PROD_005",
                                        "message": "이미 존재하는 브랜드입니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "무신사 브랜드 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PROD_006",
                                        "message": "무신사에서 브랜드를 찾을 수 없습니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(@Valid @RequestBody BrandRequest request) {
        log.info("브랜드 등록 API 호출: name={}, validateMusinsa={}", request.getName(), request.isValidateMusinsa());
        BrandResponse response = brandService.createBrand(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(
            summary = "브랜드 수정",
            description = "브랜드 정보를 수정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "브랜드 수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "인증 실패 (추후 적용 예정)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "AUTH_001",
                                        "message": "관리자 권한이 필요합니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "브랜드를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PROD_003",
                                        "message": "브랜드를 찾을 수 없습니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @PutMapping("/{brandId}")
    public ApiResponse<BrandResponse> updateBrand(
            @PathVariable Long brandId,
            @Valid @RequestBody BrandRequest request
    ) {
        log.info("브랜드 수정 API 호출: id={}, name={}", brandId, request.getName());
        BrandResponse response = brandService.updateBrand(brandId, request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "브랜드 삭제",
            description = "브랜드를 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "브랜드 삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "인증 실패 (추후 적용 예정)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "AUTH_001",
                                        "message": "관리자 권한이 필요합니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "브랜드를 찾을 수 없음"
            )
    })
    @DeleteMapping("/{brandId}")
    public ApiResponse<Void> deleteBrand(@PathVariable Long brandId) {
        log.info("브랜드 삭제 API 호출: id={}", brandId);
        brandService.deleteBrand(brandId);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "브랜드 단건 조회",
            description = "특정 브랜드의 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "브랜드 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "인증 실패 (추후 적용 예정)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "AUTH_001",
                                        "message": "관리자 권한이 필요합니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "브랜드를 찾을 수 없음"
            )
    })
    @GetMapping("/{brandId}")
    public ApiResponse<BrandResponse> getBrand(@PathVariable Long brandId) {
        log.info("브랜드 조회 API 호출: id={}", brandId);
        BrandResponse response = brandService.getBrand(brandId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "브랜드 전체 조회",
            description = "등록된 모든 브랜드를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "브랜드 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "id": 1,
                                          "name": "Musinsa Standard",
                                          "logoUrl": "https://example.com/logo.png",
                                          "primaryStyle": "minimalist",
                                          "brandType": "CLOTHING",
                                          "createdAt": "2025-12-09T11:00:00",
                                          "modifiedAt": "2025-12-09T11:00:00"
                                        }
                                      ]
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "인증 실패 (추후 적용 예정)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "AUTH_001",
                                        "message": "관리자 권한이 필요합니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @GetMapping
    public ApiResponse<List<BrandResponse>> getAllBrands() {
        log.info("브랜드 전체 조회 API 호출");
        List<BrandResponse> response = brandService.getAllBrands();
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "스타일별 브랜드 조회",
            description = "특정 스타일의 브랜드를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "브랜드 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "인증 실패 (추후 적용 예정)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "AUTH_001",
                                        "message": "관리자 권한이 필요합니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/style/{style}")
    public ApiResponse<List<BrandResponse>> getBrandsByStyle(
            @PathVariable @NotBlank(message = "스타일은 필수입니다") String style
    ) {
        log.info("스타일별 브랜드 조회 API 호출: style={}", style);
        List<BrandResponse> response = brandService.getBrandsByStyle(style);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "무신사 브랜드 검증",
            description = "무신사에 브랜드가 존재하는지 검증합니다. (등록 전 테스트용)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "검증 완료",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "exists": true,
                                        "brandCode": "musinsastandard",
                                        "message": "무신사에서 브랜드를 찾았습니다"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "인증 실패 (추후 적용 예정)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "AUTH_001",
                                        "message": "관리자 권한이 필요합니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/validate")
    public ApiResponse<BrandValidationResult> validateMusinsaBrand(
            @RequestParam @NotBlank(message = "브랜드명은 필수입니다") String brandName
    ) {
        log.info("브랜드 검증 API 호출: brandName={}", brandName);
        BrandValidationResult response = brandService.validateMusinsaBrand(brandName);
        return ApiResponse.success(response);
    }
}
