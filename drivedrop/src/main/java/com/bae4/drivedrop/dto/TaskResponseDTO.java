package com.bae4.drivedrop.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskResponseDTO {
    private String taskId;
    private String fileName;
    private String status;
    private String authMode;

    // For OPEN_CLAIM Mode
    private String publicUrl;

    // For DISTRIBUTED Mode
    private List<AccessCodeDTO> accessLinks;

    private LocalDateTime createdAt;
    private Integer totalCodes;
    private Integer remainingCodes;
}
