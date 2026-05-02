package com.bae4.drivedrop.service;

import com.bae4.drivedrop.entity.User;
import com.bae4.drivedrop.utils.JWTUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserService userService;
    private final JsonFactory jsonFactory;
    private final HttpTransport httpTransport;

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    @Value("${url.redirect-url}")
    private String redirectBaseUrl;

    public String generateToken(String code) throws IOException {
        GoogleUserInfo userInfo = exchangeTokenWithGoogle(code);
        User user = userService.createOrFindUser(userInfo);
        return JWTUtil.generateToken(
                user.getEmail(),
                user.getGoogleSub()
        );
    }

    GoogleUserInfo exchangeTokenWithGoogle(String code) throws IOException {
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                httpTransport,
                jsonFactory,
                clientId,
                clientSecret,
                code,
                redirectUri)
                .execute();

        GoogleIdToken idToken = tokenResponse.parseIdToken();
        if (idToken == null) {
            log.error("Google TokenResponse success but ID Token is null. Check if 'openid' scope is included.");
            throw new IllegalStateException("Identity token is missing from Google response");
        }
        GoogleIdToken.Payload payload = idToken.getPayload();

        return new GoogleUserInfo(
                payload.getSubject(),
                payload.getEmail(),
                tokenResponse.getRefreshToken()
        );
    }

    public record GoogleUserInfo(String sub, String email, String refreshToken) {}
}
