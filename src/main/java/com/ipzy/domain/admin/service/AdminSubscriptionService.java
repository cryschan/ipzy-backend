package com.ipzy.domain.admin.service;

import com.ipzy.domain.admin.dto.AdminSubscriptionDetailResponse;
import com.ipzy.domain.admin.dto.AdminSubscriptionResponse;
import com.ipzy.domain.admin.dto.AdminSubscriptionSearchRequest;
import com.ipzy.domain.admin.exception.AdminException;
import com.ipzy.domain.admin.specification.AdminSubscriptionSpecification;
import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminSubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public Page<AdminSubscriptionResponse> findSubscriptions(AdminSubscriptionSearchRequest request, Pageable pageable) {
        Specification<Subscription> spec = Specification
            .where(AdminSubscriptionSpecification.userEmailOrNameContains(request.keyword()))
            .and(AdminSubscriptionSpecification.statusEquals(request.status()))
            .and(AdminSubscriptionSpecification.planNameEquals(request.planName()))
            .and(AdminSubscriptionSpecification.createdAtBetween(request.createdFrom(), request.createdTo()));

        return subscriptionRepository.findAll(spec, pageable)
            .map(AdminSubscriptionResponse::from);
    }

    public AdminSubscriptionDetailResponse findSubscriptionDetail(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(AdminException::subscriptionNotFound);
        return AdminSubscriptionDetailResponse.from(subscription);
    }

    @Transactional
    public AdminSubscriptionDetailResponse cancelSubscription(Long subscriptionId, String reason, Long adminId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(AdminException::subscriptionNotFound);

        if (!subscription.isActive()) {
            throw AdminException.invalidSubscriptionCancel();
        }

        subscription.cancel(reason != null ? reason : "관리자에 의한 취소");
        log.info("구독 취소 처리: subscriptionId={}, adminId={}", subscriptionId, adminId);

        return AdminSubscriptionDetailResponse.from(subscription);
    }
}
