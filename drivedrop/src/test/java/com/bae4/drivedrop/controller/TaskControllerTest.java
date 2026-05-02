package com.bae4.drivedrop.controller;

import com.bae4.drivedrop.config.SecurityConfig;
import com.bae4.drivedrop.entity.ShareTask;
import com.bae4.drivedrop.entity.User;
import com.bae4.drivedrop.service.DownloadService;
import com.bae4.drivedrop.service.GoogleDriveService;
import com.bae4.drivedrop.service.ShareTaskService;
import com.bae4.drivedrop.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TaskController.class)
@Import(SecurityConfig.class)
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private GoogleDriveService googleDriveService;

    @MockitoBean
    private ShareTaskService shareTaskService;

    @MockitoBean
    private DownloadService downloadService;

    private final String initUrl = "/api/tasks/init";
    private final String confirmUrl = "/api/tasks/confirm";

    @Test
    void testInit() throws Exception {
        String sub = "google_123";
        String fileName = "file";
        User user = new User();
        user.setRefreshToken("refresh_token");
        String uploadUrl = "url";
        ShareTask task = new ShareTask();
        when(userService.findById(sub)).thenReturn(user);
        when(googleDriveService.createResumableUploadUrl(user.getRefreshToken(), fileName)).thenReturn("url");
        when(shareTaskService.initShareTask(user, fileName, uploadUrl)).thenReturn(task);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(sub, null, List.of());

        mockMvc.perform(post(initUrl).param("fileName", fileName).with(authentication(auth))).andExpect(status().isOk());
    }

}
