package com.bae4.drivedrop.service;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class GoogleDriveService {

    @Autowired
    private JsonFactory jsonFactory;
    @Autowired
    private HttpTransport httpTransport;

    @Value("${google.client-id}")
    private String clientId;
    @Value("${google.client-secret}")
    private String clientSecret;

    public String createResumableUploadUrl(String refreshToken, String fileName) throws IOException {
        TokenResponse tokenResponse = new GoogleRefreshTokenRequest(
                httpTransport, jsonFactory, refreshToken, clientId, clientSecret)
                .execute();
        String accessToken = tokenResponse.getAccessToken();

        String url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Upload-Content-Type", "application/octet-stream");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("name", fileName);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(metadata, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        return Objects.requireNonNull(response.getHeaders().getLocation()).toString();
    }

    public String refreshAccessToken(String refreshToken) throws IOException {
        TokenResponse response = new GoogleRefreshTokenRequest(
                httpTransport,
                jsonFactory,
                refreshToken,
                clientId,
                clientSecret)
                .execute();

        return response.getAccessToken();
    }
}
