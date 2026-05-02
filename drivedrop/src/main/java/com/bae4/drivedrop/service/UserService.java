package com.bae4.drivedrop.service;

import com.bae4.drivedrop.entity.User;
import com.bae4.drivedrop.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findById(String sub) {
        return userRepository.findById(sub).
                orElseThrow(() -> new EntityNotFoundException("User not found with sub: " + sub));
    }

    @Transactional
    public User createOrFindUser(GoogleAuthService.GoogleUserInfo userInfo){
        User user = userRepository.findById(userInfo.sub()).orElseGet(() -> {
            User newUser = new User();
            newUser.setGoogleSub(userInfo.sub());
            newUser.setTotalShares(0);
            return newUser;
        });

        user.setEmail(userInfo.email()); // update email
        if (userInfo.refreshToken() != null) {
            user.setRefreshToken(userInfo.refreshToken());
        }

        return userRepository.save(user);
    }
}
