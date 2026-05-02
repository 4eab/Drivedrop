package com.bae4.drivedrop.service;

import com.bae4.drivedrop.entity.User;
import com.bae4.drivedrop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Test
    void shouldCreateUser() throws IOException {
        GoogleAuthService.GoogleUserInfo userInfo = new GoogleAuthService.GoogleUserInfo("google_123", "new@test.com", "refresh_token");

        when(userRepository.findById(userInfo.sub())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User user = userService.createOrFindUser(userInfo);
        assertNotNull(user);
        assertEquals(0, user.getTotalShares());
        assertEquals(userInfo.sub(), user.getGoogleSub());
        assertEquals(userInfo.email(), user.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldFindUser() {
        String sub = "google_123";
        User existingUser = new User();
        existingUser.setGoogleSub(sub);
        existingUser.setGoogleSub("old@test.com");
        existingUser.setTotalShares(99);

        GoogleAuthService.GoogleUserInfo newInfo = new GoogleAuthService.GoogleUserInfo(sub, "new@test.com", "refresh_token");

        when(userRepository.findById(sub)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.createOrFindUser(newInfo);

        assertNotNull(result);
        assertEquals(99, result.getTotalShares());
        assertEquals("new@test.com", result.getEmail());
        verify(userRepository).save(any(User.class));
    }
}
