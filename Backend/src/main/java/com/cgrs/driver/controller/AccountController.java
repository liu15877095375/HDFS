package com.cgrs.driver.controller;

import com.cgrs.driver.dto.AccountDeleteRequest;
import com.cgrs.driver.dto.Result;
import com.cgrs.driver.service.AccountService;
import com.cgrs.driver.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 注销账户
     * POST /api/account/delete
     */
    @PostMapping("/delete")
    public Result<Map<String, Object>> deleteAccount(
            @RequestBody AccountDeleteRequest request,
            HttpServletRequest httpRequest) {

        // 验证请求中的 userId 与 token 中的 userId 一致（安全校验）
        Long tokenUserId = getUserIdFromRequest(httpRequest);
        if (!tokenUserId.equals(request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作");
        }

        String ipAddress = getClientIp(httpRequest);
        Map<String, Object> data = accountService.deleteAccount(
                request.getUserId(),
                request.getConfirmUsername(),
                ipAddress
        );

        return Result.success("账户注销成功", data);
    }

    /**
     * 恢复账户（公开接口，允许未登录用户恢复已注销账户）
     * POST /api/account/restore
     */
    @PostMapping("/restore")
    public Result<Map<String, Object>> restoreAccount(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱不能为空");
        }

        Map<String, Object> data = accountService.restoreAccountByEmail(email);
        return Result.success("账户恢复成功", data);
    }

    // ==================== 辅助方法 ====================

    /**
     * 从请求的 Authorization 头中解析用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return jwtUtil.getUserIdFromToken(authHeader.substring(7));
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 将 IPv6 环回地址转换为 IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }
}