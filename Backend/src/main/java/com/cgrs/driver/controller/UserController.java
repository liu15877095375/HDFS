package com.cgrs.driver.controller;

import com.cgrs.driver.dao.UserRepository;
import com.cgrs.driver.dto.ChangePasswordRequest;
import com.cgrs.driver.dto.Result;
import com.cgrs.driver.model.User;
import com.cgrs.driver.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.cgrs.driver.service.UserService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;
    private final UserService userService;
    @Autowired
    private JwtUtil jwtUtil;


    // 构造器注入
    public UserController(UserRepository userRepository,
                          JwtUtil jwtUtil,
                          UserService userService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(HttpServletRequest request) {
        // 1. 从请求头获取 token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        String token = authHeader.substring(7);

        // 2. 解析 token 获取用户 ID
        Long userId;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "token无效");
        }

        // 3. 检查并更新用户状态（VIP过期、存储空间设置）
        User user = userService.checkAndUpdateUserStatus(userId);

        // 4. 构造返回数据（不包含密码）
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("user_id", user.getUserId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("role", user.getRole());
        userInfo.put("storage_quota", user.getStorageQuota());
        userInfo.put("used_storage", user.getUsedStorage());
        userInfo.put("vip_expire", user.getVipExpire());

        return Result.success("获取成功", userInfo);
    }

    /**
     * 修改密码
     * 请求头需携带 Authorization: Bearer <token>
     */
    @PostMapping("/change-password")
    public Result<?> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {

        // 1. 从 JWT 提取用户ID
        String token = authHeader.replace("Bearer ", "");
        Long userId;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "token无效");
        }

        // 2. 调用UserService 修改密码
        boolean success = userService.changePassword(
                userId,
                request.getOldPassword(),
                request.getNewPassword()
        );

        if (!success) {
            return Result.error(400, "原密码错误");
        }

        return Result.success("密码修改成功，请重新登录", null);
    }
}