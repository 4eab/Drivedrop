package com.bae4.drivedrop.service;

import com.bae4.drivedrop.entity.AccessCode;
import com.bae4.drivedrop.entity.ShareTask;
import com.bae4.drivedrop.entity.User;
import com.bae4.drivedrop.enums.TaskStatus;
import com.bae4.drivedrop.repository.AccessCodeRepository;
import com.bae4.drivedrop.repository.ShareTaskRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DownloadService {
    private final ShareTaskRepository taskRepository;
    private final GoogleDriveService googleDriveService;
    private final AccessCodeService accessCodeService;
    private final AccessCodeRepository codeRepository;

    public void downloadFile(String taskId, String key, HttpServletResponse response) throws IOException {
        AccessCode code = accessCodeService.consumeAccessCode(taskId, key);
        ShareTask task = code.getTask();

        User owner = task.getOwner();
        String refreshToken = owner.getRefreshToken();

        String accessToken = googleDriveService.refreshAccessToken(refreshToken);

        response.setContentType("application/octet-stream");
        String encodedFileName = UriUtils.encode(task.getFileName(), StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"");

        String googleUrl = "https://www.googleapis.com/drive/v3/files/" + task.getGoogleFileId() + "?alt=media";

        RestTemplate restTemplate = new RestTemplate();

        try {
            restTemplate.execute(googleUrl, HttpMethod.GET, request -> {
                request.getHeaders().setBearerAuth(accessToken);
            }, clientResponse -> {
                StreamUtils.copy(clientResponse.getBody(), response.getOutputStream());
                return null;
            });
            code.setUsed(true);
            code.setUsedAt(LocalDateTime.now());
            codeRepository.save(code);
        } catch (Exception e) {
            System.err.println("Something went wrong");
        }

        boolean hasRemaining = codeRepository.existsByTaskIdAndUsedFalse(task.getId()); // TODO: EFFICIENCY?

        if (!hasRemaining) {
            task.setTaskStatus(TaskStatus.EXPIRED);
            taskRepository.save(task);
            deleteFileFromDrive(task);
        }
    }

    private void deleteFileFromDrive(ShareTask task) throws IOException {
        googleDriveService.deleteFileFromDrive(task.getOwner().getRefreshToken(), task.getGoogleFileId());
        task.setFileDeleted(true);
    }
}
