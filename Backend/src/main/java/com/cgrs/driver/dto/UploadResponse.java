package com.cgrs.driver.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadResponse {
    private Long fileId;
    private String fileName;
    private Long size;
    private boolean instant;   // 是否秒传（已移除，保留字段兼容）
}
