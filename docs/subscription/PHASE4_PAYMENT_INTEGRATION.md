# Phase 4: 결제 연동 (토스페이먼츠) - BASIC 플랜만

## 🎯 목표
- **BASIC 플랜 결제 연동** (2,900원/월, PRO는 제외)
- 토스페이먼츠 API 연동
- PENDING → ACTIVE 전환
- **결제 실패 시 FREE 플랜으로 다운그레이드**

---

## 📝 작업 체크리스트

### 4-1. 환경 설정
- [ ] 토스페이먼츠 가입 및 API 키 발급
- [ ] application.yml에 설정 추가
- [ ] 의존성 추가 (Spring WebClient 또는 RestTemplate)

### 4-2. Payment 도메인 구현
- [ ] PaymentRepository 구현
- [ ] PaymentService 구현
- [ ] PaymentController 구현

### 4-3. API 구현
- [ ] 결제 요청 API
- [ ] 결제 승인 API
- [ ] 결제 실패 처리
- [ ] 웹훅 처리

### 4-4. 테스트
- [ ] 결제 플로우 통합 테스트
- [ ] 실패 시나리오 테스트
- [ ] 웹훅 테스트

---

## 🔧 구현 코드

### 1. application.yml 설정

```yaml
# application.yml
toss:
  payments:
    client-key: ${TOSS_CLIENT_KEY}  # 환경변수
    secret-key: ${TOSS_SECRET_KEY}  # 환경변수
    success-url: ${BASE_URL}/api/payment/toss/success
    fail-url: ${BASE_URL}/api/payment/toss/fail
    api-url: https://api.tosspayments.com/v1
```

```yaml
# application-local.yml
toss:
  payments:
    client-key: test_ck_...
    secret-key: test_sk_...

spring:
  profiles:
    active: local
```

---

### 2. PaymentRepository 구현

```java
package com.ipzy.domain.payment.repository;

import com.ipzy.domain.payment.entity.Payment;
import com.ipzy.domain.user.entity.User;
import com.ipzy._global.common.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 사용자의 최신 결제 조회
     */
    Optional<Payment> findTopByUserOrderByCreatedAtDesc(User user);

    /**
     * 사용자의 모든 결제 히스토리
     */
    List<Payment> findAllByUserOrderByCreatedAtDesc(User user);

    /**
     * 특정 상태의 결제 조회
     */
    List<Payment> findAllByStatus(PaymentStatus status);

    /**
     * 외부 거래 ID로 조회 (토스페이먼츠 paymentKey)
     */
    Optional<Payment> findByTransactionId(String transactionId);
}
```

---

### 3. PaymentService 구현

```java
package com.ipzy.domain.payment.service;

import com.ipzy._global.common.enums.PaymentMethod;
import com.ipzy._global.common.enums.PaymentStatus;
import com.ipzy._global.common.enums.SubscriptionStatus;
import com.ipzy.domain.payment.dto.request.PaymentApproveRequest;
import com.ipzy.domain.payment.dto.request.PaymentCreateRequest;
import com.ipzy.domain.payment.dto.response.PaymentResponse;
import com.ipzy.domain.payment.entity.Payment;
import com.ipzy.domain.payment.exception.PaymentException;
import com.ipzy.domain.payment.repository.PaymentRepository;
import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.subscription.repository.SubscriptionRepository;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.exception.UserException;
import com.ipzy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final TossPaymentsClient tossPaymentsClient;  // 토스 API 클라이언트

    /**
     * 결제 요청 (BASIC 플랜만)
     *
     * @param userId 사용자 ID
     * @param request 결제 요청 정보
     * @return 결제 정보 (결제 URL 포함)
     */
    @Transactional
    public PaymentResponse createPayment(Long userId, PaymentCreateRequest request) {
        User user = findUserById(userId);

        // PENDING 구독 확인
        Subscription subscription = subscriptionRepository
            .findValidSubscription(user)
            .filter(s -> s.getStatus() == SubscriptionStatus.PENDING)
            .orElseThrow(() -> PaymentException.noPendingSubscription());

        // Payment 생성
        Payment payment = Payment.builder()
            .user(user)
            .subscription(subscription)
            .amount(subscription.getPlan().getPrice())
            .currency("KRW")
            .method(PaymentMethod.CARD)  // 현재는 카드만
            .status(PaymentStatus.PENDING)
            .description(subscription.getPlan().getDisplayName() + " 구독")
            .build();

        paymentRepository.save(payment);

        // 토스페이먼츠 결제 요청
        String paymentUrl = tossPaymentsClient.requestPayment(
            payment.getId().toString(),  // orderId
            payment.getAmount(),
            payment.getDescription()
        );

        log.info("결제 요청 생성: paymentId={}, amount={}", payment.getId(), payment.getAmount());

        return PaymentResponse.of(payment, paymentUrl);
    }

    /**
     * 결제 승인 (토스페이먼츠 콜백)
     *
     * @param request 승인 요청 정보
     * @return 결제 정보
     */
    @Transactional
    public PaymentResponse approvePayment(PaymentApproveRequest request) {
        // 1. Payment 조회
        Payment payment = paymentRepository.findById(Long.parseLong(request.orderId()))
            .orElseThrow(() -> PaymentException.notFound(request.orderId()));

        // 2. 토스페이먼츠에 승인 요청
        TossPaymentResponse tossResponse = tossPaymentsClient.confirmPayment(
            request.paymentKey(),
            request.orderId(),
            request.amount()
        );

        // 3. Payment 완료 처리
        payment.complete(
            tossResponse.paymentKey(),
            tossResponse.receiptUrl()
        );

        // 4. ⭐ Subscription ACTIVE 전환
        Subscription subscription = payment.getSubscription();
        subscription.activate();

        log.info("결제 승인 완료: paymentId={}, subscriptionId={}",
                payment.getId(), subscription.getId());

        return PaymentResponse.of(payment);
    }

    /**
     * 결제 실패 처리
     *
     * @param orderId 주문 ID
     * @param errorCode 에러 코드
     * @param errorMessage 에러 메시지
     */
    @Transactional
    public void failPayment(String orderId, String errorCode, String errorMessage) {
        Payment payment = paymentRepository.findById(Long.parseLong(orderId))
            .orElseThrow(() -> PaymentException.notFound(orderId));

        // Payment 실패 처리
        payment.fail(errorCode + ": " + errorMessage);

        // Subscription CANCELLED 처리
        Subscription subscription = payment.getSubscription();
        subscription.cancel("결제 실패: " + errorMessage);

        log.warn("결제 실패: paymentId={}, errorCode={}, message={}",
                payment.getId(), errorCode, errorMessage);

        // TODO (Phase 5): FREE (영구) 플랜으로 다운그레이드
        // downgradeToFreePlan(subscription.getUser());
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> UserException.notFound(userId));
    }
}
```

---

### 4. TossPaymentsClient 구현

```java
package com.ipzy.domain.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentsClient {

    @Value("${toss.payments.secret-key}")
    private String secretKey;

    @Value("${toss.payments.api-url}")
    private String apiUrl;

    @Value("${toss.payments.success-url}")
    private String successUrl;

    @Value("${toss.payments.fail-url}")
    private String failUrl;

    private final WebClient webClient;

    /**
     * 결제 요청 URL 생성
     */
    public String requestPayment(String orderId, Integer amount, String orderName) {
        // 토스페이먼츠 결제창 URL
        return "https://payment.toss.im/pay?clientKey=" + clientKey
                + "&orderId=" + orderId
                + "&amount=" + amount
                + "&orderName=" + orderName
                + "&successUrl=" + successUrl
                + "&failUrl=" + failUrl;
    }

    /**
     * 결제 승인 요청
     */
    public TossPaymentResponse confirmPayment(String paymentKey, String orderId, Integer amount) {
        String auth = Base64.getEncoder().encodeToString(
            (secretKey + ":").getBytes(StandardCharsets.UTF_8)
        );

        return webClient.post()
            .uri(apiUrl + "/payments/confirm")
            .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount
            ))
            .retrieve()
            .bodyToMono(TossPaymentResponse.class)
            .block();
    }
}
```

---

### 5. PaymentController 구현

```java
package com.ipzy.domain.payment.controller;

import com.ipzy._global.common.ApiResponse;
import com.ipzy.domain.auth.dto.CustomUserPrincipal;
import com.ipzy.domain.payment.dto.request.PaymentApproveRequest;
import com.ipzy.domain.payment.dto.request.PaymentCreateRequest;
import com.ipzy.domain.payment.dto.response.PaymentResponse;
import com.ipzy.domain.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment", description = "결제 관리 API (BASIC 플랜)")
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 요청 (BASIC 플랜만)
     */
    @Operation(summary = "결제 요청", description = "BASIC 플랜 결제를 요청합니다. (PENDING 구독 필요)")
    @PostMapping("/request")
    public ApiResponse<PaymentResponse> requestPayment(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody PaymentCreateRequest request) {

        PaymentResponse response = paymentService.createPayment(
                principal.getUserId(),
                request
        );

        return ApiResponse.success(response);
    }

    /**
     * 결제 승인 (토스페이먼츠 콜백)
     */
    @Operation(summary = "결제 승인", description = "토스페이먼츠 결제 성공 콜백")
    @PostMapping("/toss/success")
    public ApiResponse<PaymentResponse> approvePayment(
            @Valid @RequestBody PaymentApproveRequest request) {

        PaymentResponse response = paymentService.approvePayment(request);

        return ApiResponse.success(response);
    }

    /**
     * 결제 실패 (토스페이먼츠 콜백)
     */
    @Operation(summary = "결제 실패", description = "토스페이먼츠 결제 실패 콜백")
    @GetMapping("/toss/fail")
    public ApiResponse<Void> failPayment(
            @RequestParam String orderId,
            @RequestParam String code,
            @RequestParam String message) {

        paymentService.failPayment(orderId, code, message);

        return ApiResponse.success(null);
    }
}
```

---

## 📊 결제 플로우

```
사용자: BASIC 플랜 선택
    ↓
POST /api/subscription/subscriptions { planId: BASIC }
    ↓
BASIC (PENDING) 구독 생성
    ↓
POST /api/payment/request
    ↓
Payment (PENDING) 생성 + 토스 결제 URL 반환
    ↓
사용자: 토스페이먼츠 결제창에서 결제
    ↓
    ┌──────┴──────┐
    ↓             ↓
성공 콜백      실패 콜백
    ↓             ↓
POST /toss/success   GET /toss/fail
    ↓             ↓
Payment COMPLETED   Payment FAILED
    ↓             ↓
Subscription ACTIVE  Subscription CANCELLED
```

---

## ✅ 테스트 시나리오

### 1. 결제 성공 플로우

```java
@SpringBootTest
class PaymentIntegrationTest {

    @Test
    @DisplayName("BASIC 플랜 결제 성공 플로우")
    void basicPlanPaymentSuccess() {
        // Given: BASIC (PENDING) 구독 생성
        User user = createTestUser();
        Subscription subscription = createPendingSubscription(user, basicPlan);

        // When: 결제 요청
        PaymentCreateRequest createRequest = new PaymentCreateRequest();
        PaymentResponse paymentResponse = paymentService.createPayment(
            user.getId(),
            createRequest
        );

        // Then: 결제 URL 반환
        assertThat(paymentResponse.getPaymentUrl()).isNotNull();
        assertThat(paymentResponse.getStatus()).isEqualTo(PaymentStatus.PENDING);

        // When: 결제 승인
        PaymentApproveRequest approveRequest = new PaymentApproveRequest(
            "test_payment_key",
            paymentResponse.getId().toString(),
            basicPlan.getPrice()
        );
        paymentService.approvePayment(approveRequest);

        // Then: Subscription ACTIVE
        Subscription updated = subscriptionRepository.findById(subscription.getId())
            .orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

        // Payment COMPLETED
        Payment payment = paymentRepository.findById(paymentResponse.getId())
            .orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }
}
```

---

## 🎯 완료 기준

- [ ] 토스페이먼츠 테스트 계정 연동
- [ ] 결제 요청 API 동작
- [ ] 결제 승인 시 ACTIVE 전환
- [ ] 결제 실패 시 CANCELLED 전환
- [ ] Swagger 문서화
- [ ] 통합 테스트 통과

---

## 🚨 주의사항

### PRO 플랜 처리

```java
// SubscriptionController.java
@PostMapping("/subscriptions")
public ApiResponse<SubscriptionResponse> createSubscription(...) {

    // PRO 플랜 선택 시 예외 처리
    if (plan.getName().equals("PRO")) {
        throw SubscriptionException.planNotAvailable(
            "PRO 플랜은 현재 준비 중입니다."
        );
    }

    // BASIC만 허용
    // ...
}
```

---

**Estimated Time**: 1주일
**Priority**: 🔥 High (수익화 핵심)
**Dependencies**: Phase 2, 3 완료 후 진행