package com.cgrs.driver.service.impl;

import com.cgrs.driver.dao.*;
import com.cgrs.driver.dto.ShareCreateRequest;
import com.cgrs.driver.model.*;
import com.cgrs.driver.service.ShareService;
import com.cgrs.driver.utils.ShareLinkGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ShareServiceImpl implements ShareService {

    @Autowired
    private ShareRepository shareRepository;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private DirectoryRepository directoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OperationLogRepository operationLogRepository;

    @Value("${app.share.domain:http://localhost:8080}")
    private String appDomain;

    @Override
    @Transactional
    public Map<String, Object> createShare(Long sharerId, ShareCreateRequest request) {
        // 1. 验证用户
        User sharer = userRepository.findById(sharerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (sharer.getStatus() != 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账户状态异常，无法创建分享");
        }

        // 2. 验证目标
        String targetName;
        if (request.getTargetType() == 0) {
            FileEntity file = fileRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文件不存在"));

            if (file.getIsDeleted()) {
                throw new ResponseStatusException(HttpStatus.GONE, "文件已被删除，无法分享");
            }
            targetName = file.getFileName();
        } else {
            Directory dir = directoryRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "目录不存在"));

            if (dir.getIsDeleted()) {
                throw new ResponseStatusException(HttpStatus.GONE, "目录已被删除，无法分享");
            }
            targetName = dir.getDirName();
        }

        // 3. 生成分享链接
        String shareLink = ShareLinkGenerator.generateLink();

        // 4. 计算过期时间
        LocalDateTime expireTime;
        if (request.getExpireDays() == 365) {
            // 永久：设置100年有效期
            expireTime = LocalDateTime.now().plusYears(100);
        } else {
            expireTime = LocalDateTime.now().plusDays(request.getExpireDays());
        }

        // 5. 创建分享记录
        Share share = new Share();
        share.setShareLink(shareLink);
        share.setSharerId(sharerId);
        share.setTargetType(request.getTargetType());
        share.setTargetId(request.getTargetId());
        share.setExtractCode(request.getExtractCode());
        share.setPermission(request.getPermission());
        share.setExpireTime(expireTime);
        share.setCreateTime(LocalDateTime.now());

        shareRepository.save(share);

        // 6. 记录操作日志
        OperationLog log = new OperationLog();
        log.setUserId(sharerId);
        log.setOperationType("share");
        log.setTarget("分享" + (request.getTargetType() == 0 ? "文件" : "目录") + ": " + targetName);
        log.setResult(1);
        log.setOperationTime(LocalDateTime.now());
        operationLogRepository.save(log);

        // 7. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("shareId", share.getShareId());
        result.put("shareLink", shareLink);
        result.put("shareUrl", appDomain + "/share/" + shareLink);
        result.put("extractCode", request.getExtractCode());
        result.put("permission", request.getPermission());
        result.put("expireTime", expireTime);
        result.put("targetName", targetName);

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> getShareDetail(String link, String extractCode) {
        // 1. 查询分享
        Share share = shareRepository.findByShareLink(link)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "分享不存在"));

        // 2. 检查过期
        if (share.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "分享已过期");
        }

        // 3. 验证提取码
        if (share.getExtractCode() != null && !share.getExtractCode().isEmpty()) {
            if (extractCode == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("needExtractCode", true);
                return result;
            }
            if (!share.getExtractCode().equalsIgnoreCase(extractCode)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "提取码错误");
            }
        }

        // 4. 增加访问计数
        share.setViewCount(share.getViewCount() + 1);
        shareRepository.save(share);

        // 5. 获取分享者信息
        User sharer = userRepository.findById(share.getSharerId()).orElse(null);

        // 6. 获取目标信息
        Map<String, Object> targetInfo = new HashMap<>();
        if (share.getTargetType() == 0) {
            FileEntity file = fileRepository.findById(share.getTargetId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文件已被删除"));

            if (file.getIsDeleted()) {
                throw new ResponseStatusException(HttpStatus.GONE, "文件已被删除");
            }

            targetInfo.put("fileId", file.getFileId());
            targetInfo.put("fileName", file.getFileName());
            targetInfo.put("fileSize", formatFileSize(file.getSize()));
            // 添加下载URL
            if (share.getPermission() >= 2) {
                targetInfo.put("downloadUrl", appDomain + "/api/files/download/" + file.getFileId());
            }
        } else {
            Directory dir = directoryRepository.findById(share.getTargetId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "目录已被删除"));

            if (dir.getIsDeleted()) {
                throw new ResponseStatusException(HttpStatus.GONE, "目录已被删除");
            }

            targetInfo.put("dirId", dir.getDirId());
            targetInfo.put("dirName", dir.getDirName());
            
            // 获取目录下的文件列表
            List<Map<String, Object>> fileList = new java.util.ArrayList<>();
            List<FileEntity> files = fileRepository.findByParentDirIdAndIsDeletedFalse(share.getTargetId());
            
            for (FileEntity file : files) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("fileId", file.getFileId());
                fileInfo.put("fileName", file.getFileName());
                fileInfo.put("fileSize", formatFileSize(file.getSize()));
                fileInfo.put("uploadTime", file.getUploadTime());
                if (share.getPermission() >= 2) {
                    fileInfo.put("downloadUrl", appDomain + "/api/files/download/" + file.getFileId());
                }
                fileList.add(fileInfo);
            }
            targetInfo.put("files", fileList);
        }

        // 7. 返回结果
        String[] permissionTexts = {"", "只读", "可下载", "可转存"};

        Map<String, Object> result = new HashMap<>();
        result.put("shareId", share.getShareId());
        result.put("targetType", share.getTargetType());
        result.put("targetTypeName", share.getTargetType() == 0 ? "文件" : "目录");
        result.put("permission", share.getPermission());
        result.put("permissionText", permissionTexts[share.getPermission()]);
        result.put("expireTime", share.getExpireTime());
        result.put("createTime", share.getCreateTime());
        result.put("viewCount", share.getViewCount());
        result.put("sharer", sharer != null ? Map.of("id", sharer.getUserId(), "name", sharer.getUsername()) : null);
        result.put("target", targetInfo);

        return result;
    }

    @Override
    public Share getShare(String link) {
        return shareRepository.findByShareLink(link).orElse(null);
    }

    @Override
    public FileEntity getShareFile(Long fileId) {
        return fileRepository.findById(fileId).orElse(null);
    }

    @Override
    @Transactional
    public void deleteShare(String link, Long userId) {
        Share share = shareRepository.findByShareLink(link)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "分享不存在"));

        if (!share.getSharerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权删除此分享");
        }

        shareRepository.delete(share);
    }

    @Override
    public List<Map<String, Object>> getMyShares(Long userId) {
        List<Share> shares = shareRepository.findBySharerIdOrderByCreateTimeDesc(userId);
        
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Share share : shares) {
            Map<String, Object> item = new HashMap<>();
            item.put("shareId", share.getShareId());
            item.put("shareLink", share.getShareLink());
            item.put("targetType", share.getTargetType());
            item.put("targetId", share.getTargetId());
            item.put("extractCode", share.getExtractCode());
            item.put("permission", share.getPermission());
            item.put("expireTime", share.getExpireTime());
            item.put("createTime", share.getCreateTime());
            item.put("viewCount", share.getViewCount());
            
            // 获取目标名称
            String targetName = "";
            if (share.getTargetType() == 0) {
                FileEntity file = fileRepository.findById(share.getTargetId()).orElse(null);
                targetName = file != null ? file.getFileName() : "未知文件";
            } else {
                Directory dir = directoryRepository.findById(share.getTargetId()).orElse(null);
                targetName = dir != null ? dir.getDirName() : "未知目录";
            }
            item.put("targetName", targetName);
            
            result.add(item);
        }
        
        return result;
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int i = (int) (Math.log(bytes) / Math.log(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, i), units[i]);
    }

    @Override
    @Transactional
    public void saveToDrive(Long userId, String shareLink, String extractCode, Long targetId) {
        // 1. 查询分享
        Share share = shareRepository.findByShareLink(shareLink)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "分享不存在"));

        // 2. 检查过期
        if (share.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "分享已过期");
        }

        // 3. 验证提取码
        if (share.getExtractCode() != null && !share.getExtractCode().isEmpty()) {
            if (extractCode == null || !share.getExtractCode().equalsIgnoreCase(extractCode)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "提取码错误");
            }
        }

        // 4. 验证权限
        if (share.getPermission() < 3) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无转存权限");
        }

        // 5. 验证用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (user.getStatus() != 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账户状态异常");
        }

        // 6. 获取要转存的文件ID
        Long fileIdToSave = targetId;
        String sourceFileName = "";
        
        if (share.getTargetType() == 0) {
            // 分享的是单个文件
            fileIdToSave = share.getTargetId();
        }
        // 如果是目录分享且指定了targetId，使用指定的文件ID

        if (fileIdToSave == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请指定要转存的文件");
        }

        // 7. 获取源文件信息
        FileEntity sourceFile = fileRepository.findById(fileIdToSave)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "源文件不存在"));

        if (sourceFile.getIsDeleted()) {
            throw new ResponseStatusException(HttpStatus.GONE, "源文件已被删除");
        }

        sourceFileName = sourceFile.getFileName();

        Directory userRootDir = directoryRepository.findByOwnerIdAndParentDirIdIsNull(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "用户根目录不存在"));

        // 检查用户是否已经转存过相同的文件
        // 转存文件的哈希值格式: userId + "_" + 原始hash
        String newHashValue = userId + "_" + sourceFile.getHashValue();
        List<FileEntity> existingFiles = fileRepository.findByHashValueAndUploaderId(
            newHashValue, userId
        );
        
        if (existingFiles.size() > 0) {
            FileEntity existingFile = existingFiles.get(0);
            boolean wasDeleted = existingFile.getIsDeleted();
            
            if (wasDeleted) {
                existingFile.setIsDeleted(false);
                existingFile.setUploadTime(LocalDateTime.now());
            }
            if (!userRootDir.getDirId().equals(existingFile.getParentDirId())) {
                existingFile.setParentDirId(userRootDir.getDirId());
            }
            fileRepository.save(existingFile);
            
            // 如果文件之前被删除了，恢复时需要增加存储空间
            if (wasDeleted) {
                user.setUsedStorage(user.getUsedStorage() + sourceFile.getSize());
                userRepository.save(user);
            }
            return;
        }
        
        // 处理文件名冲突
        String newFileName = sourceFile.getFileName();
        String baseName = newFileName;
        String extension = "";
        int dotIndex = newFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = newFileName.substring(0, dotIndex);
            extension = newFileName.substring(dotIndex);
        }

        int counter = 1;
        while (fileRepository.existsByFileNameAndUploaderIdAndIsDeletedFalse(newFileName, userId)) {
            newFileName = baseName + "(" + counter + ")" + extension;
            counter++;
        }

        // 创建新文件记录
        FileEntity newFile = new FileEntity();
        newFile.setFileName(newFileName);
        newFile.setSize(sourceFile.getSize());
        newFile.setUploaderId(userId);
        newFile.setParentDirId(userRootDir.getDirId());
        newFile.setIsDeleted(false);
        newFile.setUploadTime(LocalDateTime.now());
        newFile.setModifyTime(LocalDateTime.now());
        
        // 转存时使用用户特定的哈希值和源文件的HDFS路径（秒传机制）
        newFile.setHdfsPath(sourceFile.getHdfsPath());
        newFile.setHashValue(newHashValue);
        
        fileRepository.save(newFile);

        // 更新用户已用空间
        user.setUsedStorage(user.getUsedStorage() + sourceFile.getSize());
        userRepository.save(user);

        // 11. 记录操作日志
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType("save_to_drive");
        log.setTarget("转存分享文件: " + sourceFileName);
        log.setResult(1);
        log.setOperationTime(LocalDateTime.now());
        operationLogRepository.save(log);
    }
}