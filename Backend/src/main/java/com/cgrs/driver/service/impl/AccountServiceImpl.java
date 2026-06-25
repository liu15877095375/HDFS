package com.cgrs.driver.service.impl;

import com.cgrs.driver.dao.*;
import com.cgrs.driver.model.*;
import com.cgrs.driver.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private DirectoryRepository directoryRepository;
    @Autowired
    private RecycleBinRepository recycleBinRepository;
    @Autowired
    private OperationLogRepository operationLogRepository;

    @Override
    @Transactional
    public Map<String, Object> deleteAccount(Long userId, String confirmUsername, String ipAddress) {
        // 1. 查询用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        // 2. 检查状态
        if (user.getStatus() == 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账户已注销");
        }

        // 3. 验证用户名
        if (!user.getUsername().equals(confirmUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名输入不正确");
        }

        // 4. 更新用户状态
        user.setStatus(2);
        user.setModifyTime(LocalDateTime.now());
        userRepository.save(user);

        // 5. 标记文件为删除
        int deletedFiles = fileRepository.markAllDeletedByUploaderId(userId);

        // 6. 标记目录为删除
        int deletedDirs = directoryRepository.markAllDeletedByOwnerId(userId);

        // 7. 移入回收站
        List<FileEntity> files = fileRepository.findByUploaderIdAndIsDeletedTrue(userId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusDays(7);

        for (FileEntity file : files) {
            RecycleBin recycle = new RecycleBin();
            recycle.setUserId(userId);
            recycle.setFileId(file.getFileId());
            recycle.setDeleteTime(now);
            recycle.setExpireTime(expireTime);
            recycle.setIsPermanent(false);
            recycleBinRepository.save(recycle);
        }

        // 8. 记录日志
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType("account_delete");
        log.setTarget("用户 " + user.getUsername() + " 注销账户");
        log.setResult(1);
        log.setIpAddress(ipAddress);
        log.setOperationTime(now);
        operationLogRepository.save(log);

        // 9. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("username", user.getUsername());
        result.put("deletedFiles", deletedFiles);
        result.put("deletedDirs", deletedDirs);
        result.put("permanentDeleteTime", expireTime);

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> restoreAccount(Long userId) {
        // 1. 查询用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        // 2. 检查状态（必须是已注销状态）
        if (user.getStatus() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账户状态异常，无法恢复");
        }

        // 3. 更新用户状态为正常
        user.setStatus(1);
        user.setModifyTime(LocalDateTime.now());
        userRepository.save(user);

        // 4. 恢复用户文件
        int restoredFiles = fileRepository.restoreAllByUploaderId(userId);

        // 5. 恢复用户目录
        int restoredDirs = directoryRepository.restoreAllByOwnerId(userId);

        // 6. 删除回收站记录
        recycleBinRepository.deleteByUserId(userId);

        // 7. 记录日志
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType("account_restore");
        log.setTarget("用户 " + user.getUsername() + " 恢复账户");
        log.setResult(1);
        log.setOperationTime(LocalDateTime.now());
        operationLogRepository.save(log);

        // 8. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("username", user.getUsername());
        result.put("restoredFiles", restoredFiles);
        result.put("restoredDirs", restoredDirs);

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> restoreAccountByEmail(String email) {
        // 1. 根据邮箱查找用户
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        // 2. 检查状态（必须是已注销状态）
        if (user.getStatus() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账户状态异常，无法恢复");
        }

        // 3. 更新用户状态为正常
        user.setStatus(1);
        user.setModifyTime(LocalDateTime.now());
        userRepository.save(user);

        // 4. 恢复用户文件
        int restoredFiles = fileRepository.restoreAllByUploaderId(user.getUserId());

        // 5. 恢复用户目录
        int restoredDirs = directoryRepository.restoreAllByOwnerId(user.getUserId());

        // 6. 删除回收站记录
        recycleBinRepository.deleteByUserId(user.getUserId());

        // 7. 记录日志
        OperationLog log = new OperationLog();
        log.setUserId(user.getUserId());
        log.setOperationType("account_restore");
        log.setTarget("用户 " + user.getUsername() + " 通过邮箱恢复账户");
        log.setResult(1);
        log.setOperationTime(LocalDateTime.now());
        operationLogRepository.save(log);

        // 8. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("restoredFiles", restoredFiles);
        result.put("restoredDirs", restoredDirs);

        return result;
    }
}