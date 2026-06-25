package com.cgrs.driver.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StorageQuotaConfigRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "存储配额不能为空")
    @Min(value = 1024 * 1024 * 1024L, message = "配额不能小于1GB（1073741824字节）")
    private Long storageQuota; // 单位：字节
}
