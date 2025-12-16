package com.ipzy.domain.subscription.service;

import com.ipzy._global.common.enums.SubscriptionStatus;
import com.ipzy._global.common.enums.UserRole;
import com.ipzy._global.common.enums.UserStatus;
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

    @AfterEach
    void cleanup() {
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("동시에 구독 생성을 요청해도 FREE 구독은 하나만 생성된다")
    void 동시에_구독_생성을_요청해도_FREE_구독은_하나만_생성된다() throws Exception {
        // given
        User user = userRepository.save(
            User.builder()
                .email("test@test.com")
                .name("test")
                .provider("GOOGLE")
                .providerId("test-123")
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
    @DisplayName("구독이 이미 있는 경우 동시 요청해도 같은 구독을 반환한다")
    void 구독이_이미_있는_경우_동시_요청해도_같은_구독을_반환한다() throws Exception {
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
        assertThat(subscriptions.get(0).getId()).isEqualTo(existingSubscriptionId);
    }
}