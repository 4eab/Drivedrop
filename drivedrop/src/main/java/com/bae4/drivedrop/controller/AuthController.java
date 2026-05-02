package com.bae4.drivedrop.controller;

import com.bae4.drivedrop.service.GoogleAuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final GoogleAuthService googleAuthService;

    @Value("${app.extension.id}")
    private String extensionId;

    @Value("${url.redirect-url}")
    private String redirectBaseUrl;

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

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
        if (error != null) {
            return ResponseEntity.badRequest().body(error);
        }
        if (code == null || code.isEmpty()) {
            String errorMsg = "Missing Auth Code";
            return ResponseEntity.badRequest().body(errorMsg);
        }
        try {
            String token = googleAuthService.generateToken(code);
            String redirectUrl = UriComponentsBuilder.fromUriString(redirectBaseUrl)
                    .queryParam("token", token)
                    .build().toUriString();
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();

        } catch (IOException e) {
            log.error("Network error during Google Auth for code: {}", code, e);
            return ResponseEntity.internalServerError().body("Authentication service unavailable");
        } catch (Exception e) {
            log.error("Unexpected error during Google Auth", e);
            return ResponseEntity.internalServerError().body("An unexpected error occurred");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        return ResponseEntity.ok().build();
    }
}