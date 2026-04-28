package com.bae4.drivedrop.service;

import com.bae4.drivedrop.entity.AccessCode;
import com.bae4.drivedrop.entity.ShareTask;
import com.bae4.drivedrop.enums.ShareMode;
import com.bae4.drivedrop.repository.AccessCodeRepository;
import com.bae4.drivedrop.repository.ShareTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccessCodeService {

    private final ShareTaskRepository taskRepository;
    private final AccessCodeRepository codeRepository;

    // TODO: After 2 hours (?) Make All assigned but unused code valid again
    // TODO: If a task has an unused / not assigned code after 24 hours, the user should receive a message and can delete the file

    @Transactional
    public AccessCode consumeAccessCode(String taskId, String key) {
        if (key != null && !key.isBlank()) {
            return consumeInDistributedMode(taskId, key);
        }

        return consumeInOpenMode(taskId);
    }

    // TODO: If Code.isUsed()... is this required?
    private AccessCode consumeInOpenMode(String taskId) {
        ShareTask task = taskRepository.findWithLockById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));


        if (task.getShareMode() != ShareMode.OPEN_CLAIM) {
            throw new RuntimeException("Invalid Access: Share Mode Mismatched");
        }

        AccessCode accessCode =  task.getAccessCodes().stream()
                .filter(c -> !c.isAssigned())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No more spots available"));
        accessCode.setAssigned(true);
        accessCode.setAssignedAt(LocalDateTime.now());

        codeRepository.saveAndFlush(accessCode);
        return accessCode;
    }
    private AccessCode consumeInDistributedMode(String taskId, String key) {
        int updatedRows = codeRepository.markAsAssigned(taskId, key);

        if (updatedRows == 0) {
            throw new RuntimeException("Invalid or expired Key");
        }

        return codeRepository.findByCodeAndTaskId(key, taskId)
                .orElseThrow(() -> new RuntimeException("Code not found"));
    }
}