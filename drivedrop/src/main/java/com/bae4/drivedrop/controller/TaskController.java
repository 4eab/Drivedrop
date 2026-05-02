package com.bae4.drivedrop.controller;

import com.bae4.drivedrop.dto.TaskConfirmRequest;
import com.bae4.drivedrop.dto.TaskResponseDTO;
import com.bae4.drivedrop.entity.ShareTask;
import com.bae4.drivedrop.entity.User;
import com.bae4.drivedrop.service.DownloadService;
import com.bae4.drivedrop.service.GoogleDriveService;
import com.bae4.drivedrop.service.ShareTaskService;
import com.bae4.drivedrop.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final UserService userService;
    private final GoogleDriveService googleDriveService;
    private final ShareTaskService shareTaskService;
    private final DownloadService downloadService;

    public record InitUploadResponse(String taskId, String uploadUrl) {}

    @PostMapping("/init")
    public ResponseEntity<?> initUpload(
            @AuthenticationPrincipal String googleSub,
            @RequestParam String fileName) {
        try {

            User user = userService.findById(googleSub);

            if (user.getRefreshToken() == null) {
                log.warn("User {} tried to upload without refresh token", user.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Google Auth Expired");
            }

            String uploadUrl = googleDriveService.createResumableUploadUrl(user.getRefreshToken(), fileName);
            ShareTask task = shareTaskService.initShareTask(user, fileName, uploadUrl);
            return ResponseEntity.ok(new InitUploadResponse(task.getId(), task.getUploadUrl()));

        } catch (IOException e) { // TODO: global error handling
            log.error("Google UploadUrl Generation Error", e);
            return ResponseEntity.internalServerError().body("Task Initialization Failed");
        } catch (Exception e) {
            log.error("Unexpected Error", e);
            return ResponseEntity.internalServerError().body("Initialization Failed: " + e.getMessage());
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<TaskResponseDTO> confirmUpload(
            @RequestBody TaskConfirmRequest taskConfirmRequest) {
        ShareTask task = shareTaskService.confirmShareTask(
                taskConfirmRequest.getTaskId(),
                taskConfirmRequest.getGoogleFileId(),
                taskConfirmRequest.getShareMode(),
                taskConfirmRequest.getMaxDownloads());
        TaskResponseDTO dto = shareTaskService.convertToDTO(task);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/download/{taskId}")
    public void downloadFile(
            @PathVariable String taskId,
            @RequestParam(required = false) String key,
            HttpServletResponse response
    ) throws IOException {
        downloadService.downloadFile(taskId, key, response);
    }
}
