package com.bae4.drivedrop.service;

import com.bae4.drivedrop.entity.User;
import com.bae4.drivedrop.utils.JWTUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GoogleAuthServiceTest {

    @Mock
    private UserService userService;

    @Spy
    @InjectMocks
    private GoogleAuthService googleAuthService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(googleAuthService, "clientId", "test-id");
        ReflectionTestUtils.setField(googleAuthService, "clientSecret", "test-secret");
        ReflectionTestUtils.setField(googleAuthService, "redirectUri", "redirect-uri");
        ReflectionTestUtils.setField(googleAuthService, "redirectBaseUrl", "redirect-url");
    }

    @Test
    void ShouldReturnValidJwt() throws IOException {
        String code = "code";
        String sub = "google_123";
        String email = "test@test.com";
        User mockUser = new User();
        mockUser.setGoogleSub(sub);
        mockUser.setEmail(email);
        GoogleAuthService.GoogleUserInfo userInfo = new GoogleAuthService.GoogleUserInfo(sub, email, mockUser.getRefreshToken());

        doReturn(userInfo).when(googleAuthService).exchangeTokenWithGoogle(code);
        doReturn(mockUser).when(userService).createOrFindUser(any());

        String token = googleAuthService.generateToken(code);
        Claims claims = JWTUtil.parseToken(token);

        assertNotNull(claims);
        assertEquals(sub, claims.getSubject());
        assertEquals(email, claims.get("email"));
    }

    @Test
    void shouldThrowException() throws IOException {
        String code = "code";

        doThrow(new IOException("Google API Down")).when(googleAuthService).exchangeTokenWithGoogle(code);

        assertThrows(IOException.class, () -> googleAuthService.generateToken(code));
        verify(userService, never()).createOrFindUser(any(GoogleAuthService.GoogleUserInfo.class));
    }

    @Test
    void shouldThrowExceptionWhenDatabaseFails() throws IOException {
        String code = "code";
        GoogleAuthService.GoogleUserInfo userInfo = new GoogleAuthService.GoogleUserInfo("google_123", "new@test.com", "refresh_token");

        doReturn(userInfo).when(googleAuthService).exchangeTokenWithGoogle(code);

        when(userService.createOrFindUser(userInfo))
                .thenThrow(new RuntimeException("Database Connection Failed"));

        assertThrows(RuntimeException.class, () -> googleAuthService.generateToken(code));
    }
}
