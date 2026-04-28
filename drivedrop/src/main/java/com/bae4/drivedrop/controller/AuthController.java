package com.bae4.drivedrop.controller;

import com.bae4.drivedrop.entity.User;
import com.bae4.drivedrop.repository.UserRepository;
import com.bae4.drivedrop.utils.JWTUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${app.extension.id}")
    private String extensionId;

    @Value("${url.redirectUrl}")
    private String redirectBaseUrl;

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    @Autowired
    private UserRepository userRepository;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    @GetMapping("/login")
    public void redirectToGoogle(HttpServletResponse response) throws IOException {
        String url = new GoogleAuthorizationCodeRequestUrl(
                clientId,
                redirectUri,
                Arrays.asList(
                        "https://www.googleapis.com/auth/drive.file",
                        "https://www.googleapis.com/auth/userinfo.email"))
                .setAccessType("offline")
                .setApprovalPrompt("force") // TODO: REMEMBER TO CHANGE IT IN PROD.
                .build();
        response.sendRedirect(url);
    }

    @GetMapping("/callback")
    public ResponseEntity<?> handleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error
    ) {
        // TODO: ERROR(SUCH AS ACCESS DENY) HANDLING
        try {
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    HTTP_TRANSPORT,
                    JSON_FACTORY,
                    clientId,
                    clientSecret,
                    code,
                    redirectUri)
                    .execute();

            GoogleIdToken idToken = tokenResponse.parseIdToken();
            GoogleIdToken.Payload payload = idToken.getPayload();

            String sub = payload.getSubject();
            String email = payload.getEmail();

            User user = userRepository.findById(sub).orElseGet(() -> {
                User newUser = new User();
                newUser.setGoogleSub(sub); // TODO: A BETTER SOLUTION FOR Persist Problem?
                return newUser;
            });

            user.setEmail(email);
            if (tokenResponse.getRefreshToken() != null) {
                user.setRefreshToken(tokenResponse.getRefreshToken());
            }

            user.setTotalShares(0);

            userRepository.save(user);

            String token = JWTUtil.generateToken(
                    user.getEmail(),
                    user.getGoogleSub()
            );

            String redirectUrl = redirectBaseUrl + token;

            return ResponseEntity.status(302).header("Location", redirectUrl).build();

        } catch (IOException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String auth) {
        String token = auth.replace("Bearer ", "");
        Claims claims = JWTUtil.parseToken(token);
        return ResponseEntity.ok(claims);
    }
}