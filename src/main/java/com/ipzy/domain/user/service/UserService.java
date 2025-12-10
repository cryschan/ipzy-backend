package com.ipzy.domain.user.service;

import com.ipzy._global.common.enums.UserStatus;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.exception.UserException;
import com.ipzy.domain.user.repository.UserRepository;
import com.ipzy.domain.user.vo.UserStylePreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    public User getMyProfile(Long userId) {
        return findActiveUser(userId);
    }

    @Transactional
    public User updateProfile(Long userId, String name, String phone, String profileImageUrl) {

        User user = findActiveUser(userId);
        user.updateProfile(name, phone, profileImageUrl);

        return user;
    }

    @Transactional
    public User updatePreferences(Long userId, Map<String, Object> preferences) {

        User user = findActiveUser(userId);
        user.updatePreferences(preferences);

        return user;
    }

    @Transactional
    public void deleteAccount(Long userId) {

        User user = findActiveUser(userId);
        user.delete();
    }

    /**
     * 사용자를 물리적으로 삭제합니다. (테스트 전용)
     *
     * <p>주의: 프로덕션에서 사용 금지. 연관 데이터 FK 제약으로 실패할 수 있습니다.
     *
     * @param userId 삭제할 사용자 ID
     */
    @Transactional
    public void hardDelete(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserException.notFound(userId));

        userRepository.delete(user);
    }

    @Transactional
    public User updateStylePreference(Long userId, UserStylePreference stylePreference) {

        User user = findActiveUser(userId);
        user.updateStylePreference(stylePreference);

        return user;
    }

    private User findActiveUser(Long userId) {
        return userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED)
                .orElseThrow(() -> UserException.notFound(userId));
    }
}
