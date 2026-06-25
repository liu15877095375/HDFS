package com.cgrs.driver.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkStorageQuotaConfigRequest {

    @NotNull(message = "用户角色不能为空")
    private Integer role;

    @NotNull(message = "存储配额不能为空")
    private Long storageQuota;

    private Long downloadSpeedLimit;
}