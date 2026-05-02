package com.bae4.drivedrop.service;

import com.bae4.drivedrop.entity.ShareTask;
import com.bae4.drivedrop.enums.ShareMode;
import com.bae4.drivedrop.enums.TaskStatus;
import com.bae4.drivedrop.repository.ShareTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShareTaskServiceTest {

    @Mock
    private ShareTaskRepository taskRepository;

    @InjectMocks
    private ShareTaskService shareTaskService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(shareTaskService, "codeLength", 8);
    }

    @Test
    void shouldActivateTaskAndGenerateCodes() {
        String taskId = "test-task-id";
        ShareTask mockTask = new ShareTask();
        mockTask.setId(taskId);
        mockTask.setTaskStatus(TaskStatus.PENDING_CONFIG);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(mockTask));
        when(taskRepository.save(any(ShareTask.class))).thenAnswer(i -> i.getArguments()[0]);

        ShareTask result = shareTaskService.confirmShareTask("test-task-id", "file-123", ShareMode.OPEN_CLAIM, 3);

        assertNotNull(result);
        assertEquals(TaskStatus.ACTIVE, result.getTaskStatus());
        assertEquals(3, result.getAccessCodes().size()); // 验证是否生成了 3 个码
        assertEquals("file-123", result.getGoogleFileId());

        verify(taskRepository, times(1)).save(any(ShareTask.class));
    }

    @Test
    void shouldThrowExceptionWhenTaskNotFound() {
        when(taskRepository.findById("wrong-id")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            shareTaskService.confirmShareTask("wrong-id", "file", ShareMode.OPEN_CLAIM, 1);
        });
    }
}
