package com.ipzy.domain.user.repository;

import com.ipzy._global.common.enums.UserStatus;
import com.ipzy.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * 사용자 저장소 - JPA CRUD + 커스텀 쿼리
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndStatusNot(Long id, UserStatus status);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :userId")
    Optional<User> findByIdForUpdate(Long userId);
}
