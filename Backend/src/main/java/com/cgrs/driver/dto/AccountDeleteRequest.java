package com.cgrs.driver.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 注销账户请求体
 */
@Data
public class AccountDeleteRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "请输入用户名确认")
    private String confirmUsername;
}