package com.bae4.drivedrop.service;

import com.bae4.drivedrop.dto.AccessCodeDTO;
import com.bae4.drivedrop.dto.TaskResponseDTO;
import com.bae4.drivedrop.entity.AccessCode;
import com.bae4.drivedrop.entity.ShareTask;
import com.bae4.drivedrop.enums.ShareMode;
import com.bae4.drivedrop.enums.TaskStatus;
import com.bae4.drivedrop.repository.AccessCodeRepository;
import com.bae4.drivedrop.repository.ShareTaskRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShareTaskService {
    @Value("${code.length}")
    private int codeLength;

    @Value("${url.downloadUrl}")
    private String url;

    @Autowired
    private ShareTaskRepository taskRepository;

    @Autowired
    private AccessCodeRepository accessCodeRepository;

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
            // 抢票模式：返回公共链接
            dto.setPublicUrl(url + task.getId());
            dto.setRemainingCodes((int) task.getAccessCodes().stream().filter(c -> !c.isUsed()).count());
        } else if (task.getShareMode() == ShareMode.DISTRIBUTED) {
            // 定向分发：构建链接列表
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
