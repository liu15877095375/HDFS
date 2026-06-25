package com.cgrs.driver.controller;
import com.cgrs.driver.dto.LoginRequest;
import com.cgrs.driver.dto.RegisterRequest;
import com.cgrs.driver.dto.ResetPasswordRequest;
import com.cgrs.driver.dto.Result;
import com.cgrs.driver.dto.SendCodeRequest;
import com.cgrs.driver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;

/*写注册功能时创建，与注册和登录功能相关
作用：接收前端的HTTP请求，路径由 @RequestMapping("/api") 统一前缀。

两个接口：

POST /api/send-verify-code → 调用 userService.sendVerifyCode()

POST /api/register → 调用 userService.register()
跳转逻辑示例：
前端点击"发送验证码"
→ POST /api/send-verify-code
→ AuthController.sendVerifyCode()
→ 调用 UserService.sendVerifyCode()*/
@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UserService userService;

    // 发送邮箱验证码
    @PostMapping("/send-verify-code")
    public Result<Void> sendVerifyCode(@Valid @RequestBody SendCodeRequest request) {
        userService.sendVerifyCode(request.getEmail());
        return Result.success("验证码已发送");
    }

    // 注册
    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success("注册成功", null);
    }

    // 统一异常处理（可选，用于将异常转为统一响应）
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Result<Void>> handleStatusException(org.springframework.web.server.ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        Result<Void> result = Result.error(status.value(), ex.getReason());
        return new ResponseEntity<>(result, status);
    }

    // 新增登录接口
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> data = userService.login(request);
        return Result.success("登录成功", data);
    }

    // 发送重置密码验证码（无需登录）
    @PostMapping("/send-reset-code")
    public Result<Void> sendResetCode(@Valid @RequestBody SendCodeRequest request) {
        userService.sendVerifyCode(request.getEmail());
        return Result.success("验证码已发送");
    }

    // 使用邮箱验证码重置密码（无需原密码）
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPasswordByEmail(
                request.getEmail(),
                request.getVerifyCode(),
                request.getNewPassword()
        );
        return Result.success("密码重置成功，请使用新密码登录");
    }
}