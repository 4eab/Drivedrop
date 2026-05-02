package com.bae4.drivedrop.controller;

import com.bae4.drivedrop.config.SecurityConfig;
import com.bae4.drivedrop.entity.User;
import com.bae4.drivedrop.service.GoogleAuthService;
import com.bae4.drivedrop.utils.JWTUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleAuthService googleAuthService;

    private final String callbackUri = "/api/auth/callback";
    private final String meUri = "/api/auth/me";

    @Test
    void testHandleCallback() throws Exception {
        String code = "code";
        User mockUser = new User();
        mockUser.setEmail("test@gmail.com");
        mockUser.setGoogleSub("google_123");

        String token = JWTUtil.generateToken(mockUser.getEmail(), mockUser.getGoogleSub());

        when(googleAuthService.generateToken(code)).thenReturn(token);

        mockMvc.perform(get(callbackUri).param("code", code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("token=")));
    }

    @Test
    void testHandleCallbackWithServiceDown() throws Exception {
        String code = "code";

        String expectedErrorMsg = "Authentication service unavailable";

        when(googleAuthService.generateToken(code)).thenThrow(new IOException());

        mockMvc.perform(get(callbackUri).param("code", code))
                .andExpect(status().is5xxServerError())
                .andExpect(content().string(expectedErrorMsg));
    }

    @Test
    void testHandleCallbackWithUnknownError() throws Exception {
        String code = "code";

        String unknownErrorMsg = "An unexpected error occurred";

        when(googleAuthService.generateToken(code)).thenThrow(new RuntimeException());

        mockMvc.perform(get(callbackUri).param("code", code))
                .andExpect(status().is5xxServerError())
                .andExpect(content().string(unknownErrorMsg));
    }

    @Test
    void testHandleCallbackWithError() throws Exception {
        String errorMsg = "access_denied";
        mockMvc.perform(get(callbackUri).param("error", errorMsg))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("access_denied"));
    }

    @Test
    void testHandleCallbackWithInvalidCode() throws Exception {
        String errorMsg = "Missing Auth Code";

        mockMvc.perform(get(callbackUri).param("code", ""))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(errorMsg));

        mockMvc.perform(get(callbackUri))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(errorMsg));
    }

    @Test
    void testMeWithValidToken() throws Exception {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("google_123", null, List.of());

        mockMvc.perform(get(meUri)
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }
}
