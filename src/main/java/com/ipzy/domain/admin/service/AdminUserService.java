package com.ipzy.domain.admin.service;

import com.ipzy._global.common.enums.UserStatus;
import com.ipzy.domain.admin.dto.AdminUserDetailResponse;
import com.ipzy.domain.admin.dto.AdminUserResponse;
import com.ipzy.domain.admin.dto.AdminUserSearchRequest;
import com.ipzy.domain.admin.dto.AdminUserStatusChangeRequest;
import com.ipzy.domain.admin.exception.AdminException;
import com.ipzy.domain.admin.specification.AdminUserSpecification;
import com.ipzy.domain.subscription.entity.Subscription;
import com.ipzy.domain.subscription.repository.SubscriptionRepository;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 관리자용 회원 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    // TODO: 감사 로그 기능 추가 시 AdminAuditLogRepository 주입

    /**
     * 회원 목록 조회 (검색 조건 적용)
     */
    public Page<AdminUserResponse> findUsers(AdminUserSearchRequest request, Pageable pageable) {
        Specification<User> spec = Specification
                .where(AdminUserSpecification.emailOrNameContains(request.keyword()))
                .and(AdminUserSpecification.statusEquals(request.status()))
                .and(AdminUserSpecification.roleEquals(request.role()))
                .and(AdminUserSpecification.createdAtBetween(request.createdFrom(), request.createdTo()))
                .and(AdminUserSpecification.notDeleted());

        Page<User> users = userRepository.findAll(spec, pageable);

        // N+1 방지: User ID 목록으로 Subscription 일괄 조회
        List<Long> userIds = users.getContent().stream()
                .map(User::getId)
                .toList();

        if (userIds.isEmpty()) {
            return users.map(user -> AdminUserResponse.from(user, null));
        }

        Map<Long, Subscription> subscriptionMap = subscriptionRepository
                .findLatestByUserIds(userIds)
                .stream()
                .collect(Collectors.toMap(
                        s -> s.getUser().getId(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        return users.map(user -> AdminUserResponse.from(
                user,
                subscriptionMap.get(user.getId())
        ));
    }

    /**
     * 회원 상세 조회
     */
    public AdminUserDetailResponse findUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(AdminException::userNotFound);

        return AdminUserDetailResponse.from(user, findLatestSubscription(user));
    }

    /**
     * 회원 상태 변경
     */
    @Transactional
    public AdminUserDetailResponse changeUserStatus(
            Long userId,
            AdminUserStatusChangeRequest request,
            Long adminId
    ) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(AdminException::userNotFound);

        UserStatus beforeStatus = user.getStatus();
        UserStatus afterStatus = request.status();

        // 상태 변경
        user.changeStatus(afterStatus);

        // TODO: 감사 로그 기록 기능 추가 예정

        log.info("회원 상태 변경: userId={}, {} -> {}, adminId={}",
                userId, beforeStatus, afterStatus, adminId);

        return AdminUserDetailResponse.from(user, findLatestSubscription(user));
    }

    /**
     * 사용자의 최신 구독 조회
     */
    private Subscription findLatestSubscription(User user) {
        return subscriptionRepository
                .findTopByUserOrderByCreatedAtDesc(user)
                .orElse(null);
    }
}
