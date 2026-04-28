package com.bae4.drivedrop.controller;

import com.bae4.drivedrop.dto.TaskConfirmRequest;
import com.bae4.drivedrop.dto.TaskResponseDTO;
import com.bae4.drivedrop.entity.ShareTask;
import com.bae4.drivedrop.entity.User;
import com.bae4.drivedrop.repository.ShareTaskRepository;
import com.bae4.drivedrop.repository.UserRepository;
import com.bae4.drivedrop.service.DownloadService;
import com.bae4.drivedrop.service.GoogleDriveService;
import com.bae4.drivedrop.service.ShareTaskService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final UserRepository userRepository;
    private final ShareTaskRepository taskRepository;
    private final GoogleDriveService googleDriveService;
    private final ShareTaskService shareTaskService;
    private final DownloadService downloadService;

    @PostMapping("/init")
    public ResponseEntity<?> initUpload(
            @AuthenticationPrincipal String googleSub, // TODO: WOULD IT BE POSSIBLE TO REMOVE USER? AT LEAST MOVE THE CODE TO SERVICE
            @RequestParam String fileName) {

        User user = userRepository.findById(googleSub)
                .orElseThrow(() -> new RuntimeException("Invalid User"));

        try {
            String uploadUrl = googleDriveService.createResumableUploadUrl(user.getRefreshToken(), fileName);

            ShareTask task = new ShareTask();
            task.setOwner(user);
            task.setFileName(fileName);
            task.setUploadUrl(uploadUrl);
            taskRepository.save(task);

            Map<String, Object> result = new HashMap<>();
            result.put("taskId", task.getId());
            result.put("uploadUrl", uploadUrl);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Initialization Failed: " + e.getMessage());
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
