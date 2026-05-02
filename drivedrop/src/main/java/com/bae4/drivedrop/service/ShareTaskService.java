package com.bae4.drivedrop.service;

import com.bae4.drivedrop.dto.AccessCodeDTO;
import com.bae4.drivedrop.dto.TaskResponseDTO;
import com.bae4.drivedrop.entity.AccessCode;
import com.bae4.drivedrop.entity.ShareTask;
import com.bae4.drivedrop.entity.User;
import com.bae4.drivedrop.enums.ShareMode;
import com.bae4.drivedrop.enums.TaskStatus;
import com.bae4.drivedrop.repository.AccessCodeRepository;
import com.bae4.drivedrop.repository.ShareTaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShareTaskService {

    private final ShareTaskRepository taskRepository;
    private final AccessCodeRepository accessCodeRepository;

    @Value("${code.length}")
    private int codeLength;

    @Value("${url.download-url}")
    private String url;

    @Transactional
    public ShareTask initShareTask(User user, String fileName, String uploadUrl) {
        ShareTask task = new ShareTask();
        task.setOwner(user);
        task.setFileName(fileName);
        task.setUploadUrl(uploadUrl);
        return taskRepository.save(task);
    }

    @Transactional
    public ShareTask confirmShareTask(String taskId, String fileId, ShareMode mode, int count) {
        ShareTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task Not Found"));
        task.setGoogleFileId(fileId);
        task.setShareMode(mode);
        task.setTargetDownloadCount(count);

        List<AccessCode> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String codeStr;
            do {
                codeStr = generateRandomCode(codeLength);
            } while (accessCodeRepository.existsByCode(codeStr));
            AccessCode accessCode = new AccessCode(codeStr, task);
            codes.add(accessCode);
        }

        task.setAccessCodes(codes);
        task.setTaskStatus(TaskStatus.ACTIVE);
        task.setActivatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // Drop 0, 1, I, O

    private String generateRandomCode(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public TaskResponseDTO convertToDTO(ShareTask task) {
        TaskResponseDTO dto = new TaskResponseDTO();
        dto.setTaskId(task.getId());
        dto.setFileName(task.getFileName());
        dto.setStatus(task.getTaskStatus().name());
        dto.setAuthMode(task.getShareMode() != null ? task.getShareMode().name() : null);
        dto.setTotalCodes(task.getTargetDownloadCount());

        if (task.getShareMode() == ShareMode.OPEN_CLAIM) {
            dto.setPublicUrl(url + task.getId());
            dto.setRemainingCodes((int) task.getAccessCodes().stream().filter(c -> !c.isUsed()).count());
        } else if (task.getShareMode() == ShareMode.DISTRIBUTED) {
            List<AccessCodeDTO> links = task.getAccessCodes().stream()
                    .map(code -> new AccessCodeDTO(
                            code.getCode(),
                            url + task.getId() + "?key=" + code.getCode(),
                            code.isAssigned(),
                            false
                    ))
                    .collect(Collectors.toList());
            dto.setAccessLinks(links);
        }

        return dto;
    }
}
