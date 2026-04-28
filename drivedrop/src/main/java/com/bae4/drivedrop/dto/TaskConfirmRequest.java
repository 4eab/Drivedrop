package com.bae4.drivedrop.dto;

import com.bae4.drivedrop.enums.ShareMode;
import lombok.Data;

@Data
public class TaskConfirmRequest {
    private String taskId;
    private String googleFileId;
    private String fileName;
    private Integer maxDownloads;
    private ShareMode shareMode;
}
