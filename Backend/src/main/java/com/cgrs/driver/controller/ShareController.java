package com.cgrs.driver.controller;

import com.cgrs.driver.dto.Result;
import com.cgrs.driver.dto.ShareCreateRequest;
import com.cgrs.driver.model.FileEntity;
import com.cgrs.driver.model.Share;
import com.cgrs.driver.service.ShareService;
import com.cgrs.driver.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/share")
public class ShareController {

    @Autowired
    private ShareService shareService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${hdfs.uri}")
    private String hdfsUri;

    /**
     * 创建分享
     * POST /api/share
     */
    @PostMapping
    public Result<Map<String, Object>> createShare(
            @RequestBody ShareCreateRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        Map<String, Object> data = shareService.createShare(userId, request);
        return Result.success("分享创建成功", data);
    }

    /**
     * 获取分享详情（公开接口，无需登录）
     * GET /api/share/{link}
     */
    @GetMapping("/{link}")
    public Result<Map<String, Object>> getShare(
            @PathVariable String link,
            @RequestParam(required = false) String extractCode) {

        Map<String, Object> data = shareService.getShareDetail(link, extractCode);
        return Result.success("获取成功", data);
    }

    /**
     * 删除分享（取消分享）
     * DELETE /api/share/{link}
     */
    @DeleteMapping("/{link}")
    public Result<String> deleteShare(
            @PathVariable String link,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        shareService.deleteShare(link, userId);
        return Result.success("分享已删除", null);
    }

    /**
     * 我的分享列表
     * GET /api/share/my
     */
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> getMyShares(HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        List<Map<String, Object>> shares = shareService.getMyShares(userId);
        return Result.success("获取成功", shares);
    }

    /**
     * 分享文件下载（公开接口，无需登录）
     * GET /api/share/download/{link}
     */
    @GetMapping("/download/{link}")
    public void downloadShareFile(
            @PathVariable String link,
            @RequestParam(required = false) String extractCode,
            HttpServletResponse response) throws Exception {

        // 1. 查询分享
        Share share = shareService.getShare(link);
        if (share == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "分享不存在");
        }

        // 2. 检查过期
        if (share.getExpireTime().isBefore(java.time.LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "分享已过期");
        }

        // 3. 验证提取码
        if (share.getExtractCode() != null && !share.getExtractCode().isEmpty()) {
            if (extractCode == null || !share.getExtractCode().equalsIgnoreCase(extractCode)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "提取码错误");
            }
        }

        // 4. 验证权限
        if (share.getPermission() < 2) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无下载权限");
        }

        // 5. 获取文件信息
        FileEntity fileEntity = shareService.getShareFile(share.getTargetId());
        if (fileEntity == null || fileEntity.getIsDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文件不存在或已被删除");
        }

        // 6. 设置响应头
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileEntity.getFileName() + "\"");
        response.setHeader("Content-Length", String.valueOf(fileEntity.getSize()));

        // 7. 从HDFS读取并返回文件
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsUri);
        conf.set("dfs.client.use.datanode.hostname", "true");
        conf.set("io.native.lib.available", "false");
        FileSystem fs = FileSystem.get(new URI(hdfsUri), conf, "root");
        Path hdfsPath = new Path(fileEntity.getHdfsPath());

        try (InputStream in = fs.open(hdfsPath);
             OutputStream out = response.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }

    /**
     * 转存分享文件到我的网盘
     * POST /api/share/save-to-drive
     */
    @PostMapping("/save-to-drive")
    public Result<String> saveToDrive(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromRequest(httpRequest);
            String shareLink = (String) request.get("shareLink");
            String extractCode = (String) request.get("extractCode");
            Long targetId = null;
            
            // 处理 targetId 参数
            if (request.get("targetId") != null) {
                Object targetIdObj = request.get("targetId");
                if (targetIdObj instanceof Number) {
                    targetId = ((Number) targetIdObj).longValue();
                } else if (targetIdObj instanceof String) {
                    targetId = Long.parseLong((String) targetIdObj);
                }
            }
            
            shareService.saveToDrive(userId, shareLink, extractCode, targetId);
            return Result.success("转存成功", null);
        } catch (Exception e) {
            throw e;
        }
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
}
