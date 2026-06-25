package com.cgrs.driver.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
/*与注册相关
接收前端 {"email": "xxx@qq.com"} JSON，字段用 @NotBlank、@Email 校验。*/

@Data
public class SendCodeRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式错误")
    private String email;
}