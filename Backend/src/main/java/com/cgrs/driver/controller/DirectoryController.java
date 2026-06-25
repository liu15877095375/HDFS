package com.cgrs.driver.controller;

import com.cgrs.driver.dao.DirectoryRepository;
import com.cgrs.driver.dto.Result;
import com.cgrs.driver.model.Directory;
import com.cgrs.driver.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/directories")
public class DirectoryController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private DirectoryRepository directoryRepository;

    /**
     * 获取当前用户的所有文件夹列表
     * GET /api/directories/user
     */
    @GetMapping("/user")
    public Result<List<Directory>> getUserDirectories(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        List<Directory> directories = directoryRepository.findByOwnerIdAndIsDeletedFalse(userId);
        return Result.success("查询成功", directories);
    }

    /**
     * 创建新文件夹
     * POST /api/directories
     */
    @PostMapping
    public Result<Map<String, Object>> createDirectory(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        String dirName = (String) requestBody.get("dirName");
        Long parentDirId = requestBody.get("parentDirId") != null 
                ? ((Number) requestBody.get("parentDirId")).longValue() 
                : null;

        Directory directory = new Directory();
        directory.setDirName(dirName);
        directory.setParentDirId(parentDirId);
        directory.setOwnerId(userId);
        directory.setIsDeleted(false);
        
        Directory saved = directoryRepository.save(directory);
        
        Map<String, Object> result = new HashMap<>();
        result.put("dir_id", saved.getDirId());
        result.put("dir_name", saved.getDirName());
        
        return Result.success("创建成功", result);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        throw new RuntimeException("用户未登录");
    }
}