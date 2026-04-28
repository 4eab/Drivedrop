package com.bae4.drivedrop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccessCodeDTO {
    private String code;
    private String downloadUrl;
    private boolean assigned;
    private boolean used;
}