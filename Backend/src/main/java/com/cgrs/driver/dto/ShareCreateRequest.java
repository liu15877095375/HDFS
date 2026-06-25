package com.cgrs.driver.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Length;


/**
 * 创建分享请求体
 */
@Data
public class ShareCreateRequest {

    @NotNull(message = "目标类型不能为空")
    @Min(value = 0, message = "目标类型必须为0或1")
    @Max(value = 1, message = "目标类型必须为0或1")
    private Integer targetType;    // 0-文件，1-目录

    @NotNull(message = "目标ID不能为空")
    private Long targetId;         // 文件ID或目录ID

    @NotNull(message = "权限不能为空")
    @Min(value = 1, message = "权限必须为1、2或3")
    @Max(value = 3, message = "权限必须为1、2或3")
    private Integer permission;    // 1-只读，2-可下载，3-可转存

    @Length(max = 10, message = "提取码最长10位")
    private String extractCode;    // 提取码（可选）

    @NotNull(message = "有效期不能为空")
    private Integer expireDays;    // 有效期天数：1/7/30/365
}