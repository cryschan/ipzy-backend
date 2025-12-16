# Phase 6: 스케줄러 및 자동화

## 🎯 목표
- 만료된 구독 자동 처리
- 다운그레이드 자동화
- 알림 발송

---

## 📝 작업 체크리스트

### 6-1. 스케줄러 구현
- [ ] 구독 만료 스케줄러
- [ ] 다운그레이드 스케줄러
- [ ] 알림 발송 스케줄러

### 6-2. 알림 시스템
- [ ] 이메일 서비스 연동
- [ ] 알림 템플릿 작성
- [ ] 알림 이력 저장

### 6-3. 모니터링
- [ ] 스케줄러 실행 로그
- [ ] 실패 시 재시도 로직
- [ ] 관리자 알림

---

## 🔧 구현 코드

### 1. 스케줄러 설정

```java
package com.ipzy._global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Spring Scheduling 활성화
}
```

```yaml
# application.yml
spring:
  task:
    scheduling:
      pool:
        size: 5  # 스케줄러 스레드 풀 크기
```

---

### 2. SubscriptionScheduler 구현

```java
package com.ipzy.domain.subscription.scheduler;

import com.ipzy._global.common.enums.SubscriptionStatus;
import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.subscription.repository.SubscriptionRepository;
import com.ipzy.domain.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;

    /**
     * ⭐ 만료된 구독 자동 처리
     *
     * 매일 새벽 2시 실행
     * - FREE_TRIAL 만료 → FREE (영구) 다운그레이드
     * - BASIC 만료 → FREE (영구) 다운그레이드
     */
    @Scheduled(cron = "0 0 2 * * *")  // 매일 02:00
    @Transactional
    public void processExpiredSubscriptions() {
        log.info("만료된 구독 자동 처리 시작");

        LocalDateTime now = LocalDateTime.now();

        // ACTIVE 상태이지만 endDate가 지난 구독 조회
        List<Subscription> expiredSubscriptions = subscriptionRepository
            .findAllByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, now);

        int processedCount = 0;
        int downgradedCount = 0;

        for (Subscription subscription : expiredSubscriptions) {
            try {
                String planName = subscription.getPlan().getName();

                // 1. EXPIRED 상태로 변경
                subscription.expire();

                // 2. FREE_TRIAL 또는 BASIC → FREE 다운그레이드
                if ("FREE_TRIAL".equals(planName) || "BASIC".equals(planName)) {
                    subscriptionService.downgradeToFreePlan(subscription.getUser());
                    downgradedCount++;

                    // 3. 사용자에게 알림
                    notificationService.sendDowngradeNotification(
                        subscription.getUser(),
                        planName
                    );
                }

                processedCount++;

            } catch (Exception e) {
                log.error("구독 만료 처리 실패: subscriptionId={}, error={}",
                        subscription.getId(), e.getMessage());
            }
        }

        log.info("만료된 구독 자동 처리 완료: 총 {}건 처리, {}건 다운그레이드",
                processedCount, downgradedCount);
    }

    /**
     * 만료 예정 알림 발송
     *
     * 매일 오전 10시 실행
     * - 3일 후 만료: 1차 알림
     * - 1일 후 만료: 2차 알림
     */
    @Scheduled(cron = "0 0 10 * * *")  // 매일 10:00
    @Transactional(readOnly = true)
    public void sendExpirationWarnings() {
        log.info("만료 예정 알림 발송 시작");

        LocalDateTime threeDaysLater = LocalDateTime.now().plusDays(3);
        LocalDateTime oneDayLater = LocalDateTime.now().plusDays(1);

        // 3일 후 만료 예정
        List<Subscription> expiringSoon3Days = subscriptionRepository
            .findAllByStatusAndEndDateBetween(
                SubscriptionStatus.ACTIVE,
                threeDaysLater.minusHours(1),
                threeDaysLater.plusHours(1)
            );

        for (Subscription subscription : expiringSoon3Days) {
            notificationService.sendExpirationWarning(
                subscription.getUser(),
                subscription,
                3
            );
        }

        // 1일 후 만료 예정
        List<Subscription> expiringSoon1Day = subscriptionRepository
            .findAllByStatusAndEndDateBetween(
                SubscriptionStatus.ACTIVE,
                oneDayLater.minusHours(1),
                oneDayLater.plusHours(1)
            );

        for (Subscription subscription : expiringSoon1Day) {
            notificationService.sendExpirationWarning(
                subscription.getUser(),
                subscription,
                1
            );
        }

        log.info("만료 예정 알림 발송 완료: 3일={}, 1일={}",
                expiringSoon3Days.size(), expiringSoon1Day.size());
    }
}
```

---

### 3. SubscriptionRepository 메서드 추가

```java
// SubscriptionRepository.java

/**
 * 만료된 ACTIVE 구독 조회
 */
@Query("SELECT s FROM Subscription s " +
       "WHERE s.status = :status " +
       "AND s.endDate < :now")
List<Subscription> findAllByStatusAndEndDateBefore(
    @Param("status") SubscriptionStatus status,
    @Param("now") LocalDateTime now
);

/**
 * 특정 기간 내 만료 예정 구독 조회
 */
@Query("SELECT s FROM Subscription s " +
       "WHERE s.status = :status " +
       "AND s.endDate BETWEEN :startDate AND :endDate")
List<Subscription> findAllByStatusAndEndDateBetween(
    @Param("status") SubscriptionStatus status,
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate
);
```

---

### 4. NotificationService 구현

```java
package com.ipzy.domain.notification.service;

import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    /**
     * 다운그레이드 알림
     */
    public void sendDowngradeNotification(User user, String fromPlan) {
        String subject = "[IPZY] 구독 플랜 변경 안내";
        String body = String.format(
            """
            안녕하세요, %s님.

            %s 플랜 기간이 종료되어 무료 플랜으로 자동 전환되었습니다.

            무료 플랜에서는 AI 스타일링 기능이 제한됩니다.
            계속해서 AI 기능을 사용하시려면 BASIC 플랜을 구독해주세요.

            감사합니다.
            IPZY 팀
            """,
            user.getName(),
            fromPlan.equals("FREE_TRIAL") ? "무료 체험" : "베이직"
        );

        sendEmail(user.getEmail(), subject, body);
    }

    /**
     * 만료 예정 알림
     */
    public void sendExpirationWarning(User user, Subscription subscription, int daysLeft) {
        String planName = subscription.getPlan().getDisplayName();
        String subject = String.format("[IPZY] %s 플랜 만료 예정 (%d일 남음)", planName, daysLeft);
        String body = String.format(
            """
            안녕하세요, %s님.

            현재 사용 중인 %s 플랜이 %d일 후 만료됩니다.
            만료 일시: %s

            만료 후에는 무료 플랜으로 자동 전환되며, AI 스타일링 기능이 제한됩니다.

            계속 사용하시려면 플랜을 갱신해주세요.

            감사합니다.
            IPZY 팀
            """,
            user.getName(),
            planName,
            daysLeft,
            subscription.getEndDate()
        );

        sendEmail(user.getEmail(), subject, body);
    }

    /**
     * 결제 실패 알림
     */
    public void sendPaymentFailureNotification(User user, String errorMessage) {
        String subject = "[IPZY] 결제 실패 안내";
        String body = String.format(
            """
            안녕하세요, %s님.

            구독 결제가 실패하여 무료 플랜으로 자동 전환되었습니다.
            실패 사유: %s

            다시 시도하시려면 결제 정보를 확인 후 재시도해주세요.

            감사합니다.
            IPZY 팀
            """,
            user.getName(),
            errorMessage
        );

        sendEmail(user.getEmail(), subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@ipzy.com");

            mailSender.send(message);

            log.info("이메일 발송 성공: to={}, subject={}", to, subject);

        } catch (Exception e) {
            log.error("이메일 발송 실패: to={}, error={}", to, e.getMessage());
        }
    }
}
```

---

### 5. 이메일 설정

```yaml
# application.yml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
    default-encoding: UTF-8
```

---

## 📊 스케줄러 실행 시간

| 스케줄러 | 실행 시간 | 작업 내용 |
|---------|----------|-----------|
| **만료 처리** | 매일 02:00 | EXPIRED 전환 + 다운그레이드 |
| **만료 예정 알림** | 매일 10:00 | 3일/1일 전 알림 발송 |

---

## ✅ 테스트 시나리오

### 1. 스케줄러 수동 실행 테스트

```java
@SpringBootTest
class SubscriptionSchedulerTest {

    @Autowired
    private SubscriptionScheduler scheduler;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    @DisplayName("만료된 구독 자동 처리")
    void processExpiredSubscriptions() {
        // Given: 만료된 BASIC 구독
        User user = createTestUser();
        Subscription basic = createExpiredBasicSubscription(user);

        // When: 스케줄러 실행
        scheduler.processExpiredSubscriptions();

        // Then: EXPIRED + FREE 다운그레이드
        Subscription expired = subscriptionRepository.findById(basic.getId())
            .orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);

        // FREE 구독 생성 확인
        Subscription free = subscriptionRepository.findValidSubscription(user)
            .orElseThrow();
        assertThat(free.getPlan().getName()).isEqualTo("FREE");
    }
}
```

### 2. 알림 발송 테스트

```java
@Test
@DisplayName("만료 3일 전 알림 발송")
void sendExpirationWarning3Days() {
    // Given: 3일 후 만료 예정
    User user = createTestUser();
    Subscription subscription = createSubscriptionExpiringIn3Days(user);

    // When: 스케줄러 실행
    scheduler.sendExpirationWarnings();

    // Then: 이메일 발송 확인
    // (실제로는 Mock 또는 테스트 메일 서버로 확인)
    verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
}
```

---

## 🎯 완료 기준

- [ ] 스케줄러 구현 완료
- [ ] 만료 자동 처리 동작 확인
- [ ] 다운그레이드 자동화 확인
- [ ] 이메일 알림 발송 확인
- [ ] 로그 모니터링 가능
- [ ] 실패 시 재시도 로직

---

## 🚨 주의사항

### 1. 트랜잭션 관리

```java
// 개별 구독 처리 시 트랜잭션 분리
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void processExpiredSubscription(Subscription subscription) {
    // 하나의 구독 처리 실패가 전체 배치에 영향 주지 않도록
}
```

### 2. 배치 성능 최적화

```java
// 대량 처리 시 페이징
@Scheduled(cron = "0 0 2 * * *")
public void processExpiredSubscriptionsBatch() {
    int pageSize = 100;
    int pageNumber = 0;

    while (true) {
        Page<Subscription> page = subscriptionRepository
            .findExpiredSubscriptions(PageRequest.of(pageNumber, pageSize));

        if (page.isEmpty()) break;

        page.forEach(this::processExpiredSubscription);

        pageNumber++;
    }
}
```

### 3. 중복 실행 방지

```java
@Scheduled(cron = "0 0 2 * * *")
@SchedulerLock(name = "processExpiredSubscriptions",
               lockAtMostFor = "10m",
               lockAtLeastFor = "1m")
public void processExpiredSubscriptions() {
    // ShedLock 라이브러리로 중복 실행 방지
}
```

---

## 📈 모니터링

### 로그 예시

```
2025-12-15 02:00:00 [INFO] 만료된 구독 자동 처리 시작
2025-12-15 02:00:05 [INFO] FREE_TRIAL 만료 → FREE 다운그레이드: userId=123
2025-12-15 02:00:10 [INFO] BASIC 만료 → FREE 다운그레이드: userId=456
2025-12-15 02:00:15 [INFO] 만료된 구독 자동 처리 완료: 총 50건 처리, 50건 다운그레이드
2025-12-15 02:00:15 [INFO] 이메일 발송 성공: to=user@example.com
```

---

**Estimated Time**: 2-3일
**Priority**: 🟡 Medium
**Dependencies**: Phase 5 완료 후 진행