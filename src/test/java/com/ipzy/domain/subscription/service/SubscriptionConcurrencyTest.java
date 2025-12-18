package com.ipzy.domain.subscription.service;

import com.ipzy._global.common.enums.SubscriptionStatus;
import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.common.enums.UserStatus;
import com.ipzy.domain.quiz.repository.QuizSessionRepository;
import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.subscription.repository.SubscriptionRepository;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SubscriptionConcurrencyTest {

    @Autowired
    SubscriptionService subscriptionService;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    QuizSessionRepository quizSessionRepository;

    @AfterEach
    void cleanup() {
        // 외래키 순서: quiz_sessions → subscriptions → users
        quizSessionRepository.deleteAll();  // 먼저 quiz_sessions 삭제
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("동시에 구독 생성을 요청해도 FREE 구독은 하나만 생성된다")
    void 동시에_구독_생성을_요청해도_FREE_구독은_하나만_생성된다() throws Exception {
        // given

        User user = userRepository.save(
                User.builder()
                        .email("test11@test.com")
                        .name("test")
                        .provider("KAKAO")
                        .providerId("test-12311")
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .build()
        );

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // ⭐ 2개의 CountDownLatch 사용
        CountDownLatch readyLatch = new CountDownLatch(threadCount);  // 모든 스레드 준비 대기
        CountDownLatch startLatch = new CountDownLatch(1);            // 동시 시작 신호
        CountDownLatch doneLatch = new CountDownLatch(threadCount);   // 모든 스레드 완료 대기

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();  // 준비 완료
                    startLatch.await();       // 시작 신호 대기

                    // ⭐ 동시에 구독 생성 시도
                    subscriptionService.ensureDefaultSubscription(user.getId());
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    doneLatch.countDown();   // 완료 신호
                }
            });
        }

        readyLatch.await();  // 모든 스레드가 준비될 때까지 대기
        startLatch.countDown();  // ⭐ 동시 시작!
        doneLatch.await();   // 모든 스레드 완료 대기

        executorService.shutdown();

        // then
        System.out.println("=== 테스트 결과 ===");
        System.out.println("성공: " + successCount.get());
        System.out.println("에러: " + errorCount.get());

        // 유효한 구독 개수 확인 (ACTIVE 또는 PENDING)
        List<Subscription> validSubscriptions = subscriptionRepository
                .findAllByUserOrderByCreatedAtDesc(user);

        System.out.println("validSubscriptions: " + validSubscriptions.size());
        validSubscriptions.forEach(System.out::println);

        long activeOrPendingCount = validSubscriptions.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE
                        || s.getStatus() == SubscriptionStatus.PENDING)
                .count();

        System.out.println("생성된 ACTIVE/PENDING 구독 수: " + activeOrPendingCount);
        System.out.println("전체 구독 수: " + validSubscriptions.size());

        // ⭐ 핵심 검증: 유효한 구독은 1개만 존재해야 함
        assertThat(activeOrPendingCount)
                .as("동시 요청에도 불구하고 유효한 구독은 1개만 생성되어야 함")
                .isEqualTo(1);

        // 모든 요청이 성공해야 함 (에러 없이)
        assertThat(successCount.get())
                .as("모든 스레드가 성공적으로 구독을 조회/생성해야 함")
                .isEqualTo(threadCount);
    }

    @Test
    @DisplayName("구독이 이미 있는 경우 ensureDefaultSubscription 동시 요청해도 같은 구독을 반환한다")
    void concurrentEnsure_returnsSameSubscription() throws Exception {
        // given
        User user = userRepository.save(
                User.builder()
                        .email("existing@test.com")
                        .name("existing")
                        .provider("KAKAO")
                        .providerId("existing-456")
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .build()
        );

        // 미리 구독 생성
        Subscription existingSubscription = subscriptionService.ensureDefaultSubscription(user.getId());
        Long existingSubscriptionId = existingSubscription.getId();

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // when: 이미 구독이 있는 상태에서 동시 요청
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    Subscription subscription = subscriptionService.ensureDefaultSubscription(user.getId());

                    // 반환된 구독이 기존 구독과 같은지 확인
                    assertThat(subscription.getId()).isEqualTo(existingSubscriptionId);

                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();

        executorService.shutdown();

        // then: 여전히 구독은 1개만 존재
        List<Subscription> subscriptions = subscriptionRepository
                .findAllByUserOrderByCreatedAtDesc(user);

        long validCount = subscriptions.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE
                        || s.getStatus() == SubscriptionStatus.PENDING)
                .count();

        assertThat(validCount).isEqualTo(1);
        assertThat(subscriptions.getFirst().getId()).isEqualTo(existingSubscriptionId);
    }

    @Test
    @DisplayName("⚠️ 구독이 이미 있는 상태에서 플랜 변경을 동시에 요청하면 race condition 발생")
    void concurrentPlanChange_shouldHandleRaceCondition() throws Exception {
        // given
        User user = userRepository.save(
                User.builder()
                        .email("planchange@test.com")
                        .name("planchange")
                        .provider("KAKAO")
                        .providerId("planchange-789")
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .build()
        );

        // FREE 구독 생성
        Subscription freeSubscription = subscriptionService.ensureDefaultSubscription(user.getId());
        assertThat(freeSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(freeSubscription.getPlan().getName()).isEqualTo("FREE");

        // BASIC 플랜 조회
        Long basicPlanId = subscriptionService.findAllPlans().stream()
                .filter(p -> "BASIC".equals(p.name()))
                .findFirst()
                .orElseThrow()
                .id();

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger pendingCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // when: FREE → BASIC 플랜 변경을 동시에 요청
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    // ⚠️ 실제 비즈니스 시나리오: 플랜 변경 신청
                    com.ipzy.domain.subscription.dto.Request.CreateSubscriptionRequest request =
                            new com.ipzy.domain.subscription.dto.Request.CreateSubscriptionRequest(basicPlanId);

                    com.ipzy.domain.subscription.dto.Response.SubscriptionResponse response =
                            subscriptionService.createSubscription(user.getId(), request);

                    successCount.incrementAndGet();

                    if (response.status() == SubscriptionStatus.PENDING) {
                        pendingCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("❌ Error in thread: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();  // ⭐ 동시에 플랜 변경 시작!
        doneLatch.await();

        executorService.shutdown();

        // then: 결과 출력
        System.out.println("\n=== 플랜 변경 동시성 테스트 결과 ===");
        System.out.println("성공: " + successCount.get());
        System.out.println("PENDING 상태: " + pendingCount.get());
        System.out.println("에러: " + errorCount.get());

        List<Subscription> subscriptions = subscriptionRepository
                .findAllByUserOrderByCreatedAtDesc(user);

        System.out.println("전체 구독 수: " + subscriptions.size());

        // 구독 상태 출력
        subscriptions.forEach(s -> {
            System.out.println("  - ID: " + s.getId() +
                    ", Plan: " + s.getPlan().getName() +
                    ", Status: " + s.getStatus() +
                    ", Created: " + s.getCreatedAt());
        });

        long pendingSubscriptions = subscriptions.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.PENDING)
                .count();

        System.out.println("PENDING 상태 구독 수: " + pendingSubscriptions);

        // ⭐ 핵심 검증: 구독은 정확히 1개만 존재해야 함
        assertThat(subscriptions.size())
                .as("⚠️ 동일한 구독이 수정되어야 하므로 구독은 1개만 존재해야 함")
                .isEqualTo(1);

        // PENDING 상태는 정확히 1개여야 함 (FREE → BASIC 변경이므로)
        assertThat(pendingSubscriptions)
                .as("⚠️ Race condition 발생 시 여러 PENDING 구독이 생성되거나 0개일 수 있음")
                .isEqualTo(1);

        // 모든 요청이 성공해야 함
        assertThat(successCount.get())
                .as("플랜 변경 요청이 모두 성공해야 함")
                .isEqualTo(threadCount);
    }
}