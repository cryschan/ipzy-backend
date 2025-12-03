package com.ipzy.domain.user.service;

import com.ipzy.domain.auth.exception.AuthErrorCode;
import com.ipzy.domain.auth.exception.AuthException;
import com.ipzy.domain.user.entity.User;
import com.ipzy.domain.user.exception.UserErrorCode;
import com.ipzy.domain.user.exception.UserException;
import com.ipzy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional
    public User save(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new AuthException(AuthErrorCode.DUPLICATE_EMAIL);
        }
        return userRepository.save(user);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserException(UserErrorCode.USER_NOT_FOUND);
        }
        userRepository.deleteById(id);
    }
}
