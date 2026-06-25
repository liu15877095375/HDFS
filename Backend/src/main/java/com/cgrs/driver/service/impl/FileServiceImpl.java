package com.cgrs.driver.service.impl;

import com.cgrs.driver.dao.*;
import com.cgrs.driver.dto.UploadResponse;
import com.cgrs.driver.model.*;
import com.cgrs.driver.service.FileService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Collections;
import java.io.IOException;

@Service
public class FileServiceImpl implements FileService {

    @Value("${hdfs.uri}")
    private String hdfsUri;

    @Value("${hdfs.upload.dir}")
    private String uploadDir;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private DirectoryRepository directoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecycleBinRepository recycleBinRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;


    @PostConstruct
    public void init() {
        System.setProperty("hadoop.home.dir", "C:\\hadoop");
        System.setProperty("hadoop.native.lib", "false");
    }

    @Override
    @Transactional
    public UploadResponse uploadFile(MultipartFile file, Long parentDirId, Long userId, String ipAddress) throws Exception {
        // 1. 计算文件哈希 (SHA-256)
        String hash;
        try (InputStream is = file.getInputStream()) {
            hash = DigestUtils.sha256Hex(is);
        }

        // 2. 确定目标目录（若未指定，则使用或创建用户根目录）
        if (parentDirId == null || parentDirId == 0) {
            Directory rootDir = directoryRepository
                    .findByOwnerIdAndParentDirIdIsNull(userId)
                    .orElseGet(() -> {
                        Directory newRoot = new Directory();
                        newRoot.setDirName("我的文件");
                        newRoot.setParentDirId(null);
                        newRoot.setOwnerId(userId);
                        newRoot.setIsDeleted(false);
                        newRoot.setCreateTime(LocalDateTime.now());
                        newRoot.setModifyTime(LocalDateTime.now());
                        return directoryRepository.save(newRoot);
                    });
            parentDirId = rootDir.getDirId();
        }

        // 3. 检查文件名是否重复，如果重复则重命名
        String originalFileName = file.getOriginalFilename();
        String newFileName = originalFileName;
        String baseName = originalFileName;
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = originalFileName.substring(0, dotIndex);
            extension = originalFileName.substring(dotIndex);
        }

        int counter = 1;
        while (fileRepository.existsByFileNameAndParentDirIdAndUploaderId(newFileName, parentDirId, userId)) {
            newFileName = baseName + "(" + counter + ")" + extension;
            counter++;
        }

        // 4. 获取用户信息，检查存储配额
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getUsedStorage() + file.getSize() > user.getStorageQuota()) {
            throw new RuntimeException("存储空间不足，无法上传");
        }

        // 5. 检查其他用户是否上传过相同文件（共享文件，不重复上传到HDFS）
        Optional<FileEntity> globalExisting = fileRepository.findByHashValue(hash);
        String hdfsFilePath;
        String newHashValue = hash;

        if (globalExisting.isPresent()) {
            // 其他用户已上传过，共享HDFS文件（不占用额外空间）
            // 使用 userId_hash 的格式创建唯一标识，避免唯一约束冲突
            hdfsFilePath = globalExisting.get().getHdfsPath();
            newHashValue = userId + "_" + hash;
        } else {
            // 构造 HDFS 存储路径
            String dateStr = LocalDateTime.now().toLocalDate().toString().replace("-", "");
            String hdfsDir = uploadDir + "/" + dateStr;
            String uniqueFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            hdfsFilePath = hdfsDir + "/" + uniqueFileName;

            // 配置 HDFS 并上传文件
            Configuration conf = new Configuration();
            conf.set("fs.defaultFS", hdfsUri);
            conf.set("dfs.client.use.datanode.hostname", "true");
            conf.set("io.native.lib.available", "false");
            conf.set("fs.hdfs.impl.disable.cache", "true");

            FileSystem fs = FileSystem.get(new URI(hdfsUri), conf, "root");
            Path hdfsPath = new Path(hdfsFilePath);
            fs.mkdirs(new Path(hdfsDir));

            try (InputStream is = file.getInputStream()) {
                org.apache.hadoop.fs.FSDataOutputStream out = fs.create(hdfsPath, true);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
                out.close();
            } finally {
                fs.close();
            }
        }

        // 6. 保存文件元数据到数据库
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(newFileName);
        fileEntity.setHdfsPath(hdfsFilePath);
        fileEntity.setHashValue(newHashValue);
        fileEntity.setSize(file.getSize());
        fileEntity.setMimeType(file.getContentType());
        fileEntity.setUploaderId(userId);
        fileEntity.setParentDirId(parentDirId);
        fileEntity.setIsDeleted(false);
        fileEntity.setUploadTime(LocalDateTime.now());
        fileEntity.setModifyTime(LocalDateTime.now());
        fileRepository.save(fileEntity);

        // 7. 更新用户的已用空间
        user.setUsedStorage(user.getUsedStorage() + file.getSize());
        userRepository.save(user);

        // 记录操作日志
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType("upload");
        log.setTarget(newFileName);
        log.setResult(1);
        log.setIpAddress(ipAddress);
        log.setOperationTime(LocalDateTime.now());
        operationLogRepository.save(log);

        return UploadResponse.builder()
                .fileId(fileEntity.getFileId())
                .fileName(fileEntity.getFileName())
                .size(fileEntity.getSize())
                .instant(false)
                .build();
    }

    @Override
    @Transactional
    public void deleteFile(Long fileId, Long userId, String ipAddress) {
        FileEntity file = fileRepository.findByFileIdAndUploaderIdAndIsDeletedFalse(fileId, userId)
                .orElseThrow(() -> new RuntimeException("文件不存在或无权操作"));

        // 软删除：标记为 true
        file.setIsDeleted(true);
        file.setDeleteTime(LocalDateTime.now());
        fileRepository.save(file);

        // 更新用户已用空间
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setUsedStorage(user.getUsedStorage() - file.getSize());
        userRepository.save(user);

        // 写入回收站记录
        RecycleBin bin = new RecycleBin();
        bin.setUserId(userId);
        bin.setFileId(fileId);
        bin.setDirId(null);               // 文件，非目录
        bin.setDeleteTime(LocalDateTime.now());
        bin.setExpireTime(LocalDateTime.now().plusDays(30));   // 30天后自动清理
        bin.setIsPermanent(false);
        recycleBinRepository.save(bin);

        // 记录日志
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType("delete");
        log.setTarget(file.getFileName());
        log.setResult(1);
        log.setIpAddress(ipAddress);
        log.setOperationTime(LocalDateTime.now());
        operationLogRepository.save(log);
    }
    @Override
    public List<Map<String, Object>> getRecycleBinFiles(Long userId) {
        List<RecycleBin> bins = recycleBinRepository.findByUserIdAndIsPermanentFalse(userId);
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> result = new ArrayList<>();

        List<Long> fileIds = bins.stream()
                .filter(bin -> bin.getFileId() != null)
                .map(RecycleBin::getFileId)
                .collect(Collectors.toList());

        if (!fileIds.isEmpty()) {
            List<FileEntity> files = fileRepository.findByFileIdInAndIsDeletedTrue(fileIds);
            for (FileEntity file : files) {
                Map<String, Object> map = new HashMap<>();
                map.put("file_id", file.getFileId());
                map.put("file_name", file.getFileName());
                map.put("size", file.getSize());
                map.put("mime_type", file.getMimeType());
                map.put("delete_time", file.getDeleteTime().toString());
                map.put("is_dir", false);

                LocalDateTime expireTime = bins.stream()
                        .filter(b -> b.getFileId() != null && b.getFileId().equals(file.getFileId()))
                        .findFirst()
                        .map(RecycleBin::getExpireTime)
                        .orElse(file.getDeleteTime().plusDays(30));

                map.put("expire_time", expireTime.toString());

                long remainSeconds = java.time.Duration.between(now, expireTime).getSeconds();
                if (remainSeconds <= 0) {
                    map.put("remaining", "即将清理");
                } else {
                    long days = remainSeconds / 86400;
                    long hours = (remainSeconds % 86400) / 3600;
                    map.put("remaining", days + "天" + hours + "小时");
                }
                result.add(map);
            }
        }

        List<Long> dirIds = bins.stream()
                .filter(bin -> bin.getDirId() != null)
                .map(RecycleBin::getDirId)
                .collect(Collectors.toList());

        if (!dirIds.isEmpty()) {
            List<Directory> dirs = directoryRepository.findAllById(dirIds);
            for (Directory dir : dirs) {
                Map<String, Object> map = new HashMap<>();
                map.put("file_id", dir.getDirId());
                map.put("file_name", dir.getDirName());
                map.put("size", 0L);
                map.put("mime_type", "文件夹");
                map.put("delete_time", dir.getModifyTime().toString());
                map.put("is_dir", true);

                LocalDateTime expireTime = bins.stream()
                        .filter(b -> b.getDirId() != null && b.getDirId().equals(dir.getDirId()))
                        .findFirst()
                        .map(RecycleBin::getExpireTime)
                        .orElse(LocalDateTime.now().plusDays(30));

                map.put("expire_time", expireTime.toString());

                long remainSeconds = java.time.Duration.between(now, expireTime).getSeconds();
                if (remainSeconds <= 0) {
                    map.put("remaining", "即将清理");
                } else {
                    long days = remainSeconds / 86400;
                    long hours = (remainSeconds % 86400) / 3600;
                    map.put("remaining", days + "天" + hours + "小时");
                }
                result.add(map);
            }
        }

        return result;
    }

    @Override
    @Transactional
    public void restoreFile(Long fileId, Long userId, String ipAddress) {
        // 查找文件，必须是被当前用户软删除的
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在"));
        if (!file.getIsDeleted() || !file.getUploaderId().equals(userId)) {
            throw new RuntimeException("无权恢复该文件");
        }

        // 检查父目录是否被删除，如果是，将文件恢复到根目录
        Long parentDirId = file.getParentDirId();
        if (parentDirId != null) {
            Optional<Directory> parentDir = directoryRepository.findById(parentDirId);
            if (parentDir.isPresent() && parentDir.get().getIsDeleted()) {
                // 父目录被删除，查找用户的根目录
                Directory rootDir = directoryRepository
                        .findByOwnerIdAndParentDirIdIsNull(userId)
                        .orElseGet(() -> {
                            // 如果根目录不存在，创建一个
                            Directory newRoot = new Directory();
                            newRoot.setDirName("我的文件");
                            newRoot.setParentDirId(null);
                            newRoot.setOwnerId(userId);
                            newRoot.setIsDeleted(false);
                            newRoot.setCreateTime(LocalDateTime.now());
                            newRoot.setModifyTime(LocalDateTime.now());
                            return directoryRepository.save(newRoot);
                        });
                file.setParentDirId(rootDir.getDirId());
            }
        }

        // 恢复文件
        file.setIsDeleted(false);
        file.setDeleteTime(null);
        fileRepository.save(file);

        // 更新用户已用空间
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setUsedStorage(user.getUsedStorage() + file.getSize());
        userRepository.save(user);

        // 删除或标记回收站记录（删除即可）
        List<RecycleBin> bins = recycleBinRepository.findByFileIdAndUserIdAndIsPermanentFalse(fileId, userId);
        recycleBinRepository.deleteAll(bins);

        // 记录日志
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType("restore");
        log.setTarget(file.getFileName());
        log.setResult(1);
        log.setIpAddress(ipAddress);
        log.setOperationTime(LocalDateTime.now());
        operationLogRepository.save(log);
    }

    @Override
    @Transactional
    public void createDirectory(String dirName, Long parentDirId, Long userId, String ipAddress) {
        if (parentDirId == null || parentDirId == 0) {
            Directory rootDir = directoryRepository
                    .findByOwnerIdAndParentDirIdIsNull(userId)
                    .orElseGet(() -> {
                        Directory newRoot = new Directory();
                        newRoot.setDirName("我的文件");
                        newRoot.setParentDirId(null);
                        newRoot.setOwnerId(userId);
                        newRoot.setIsDeleted(false);
                        newRoot.setCreateTime(LocalDateTime.now());
                        newRoot.setModifyTime(LocalDateTime.now());
                        return directoryRepository.save(newRoot);
                    });
            parentDirId = rootDir.getDirId();
        }

        if (directoryRepository.existsByDirNameAndParentDirIdAndOwnerId(dirName, parentDirId, userId)) {
            throw new RuntimeException("该目录下已存在同名文件夹");
        }

        Directory dir = new Directory();
        dir.setDirName(dirName);
        dir.setParentDirId(parentDirId);   // 根目录为 null
        dir.setOwnerId(userId);
        dir.setIsDeleted(false);
        dir.setCreateTime(LocalDateTime.now());
        dir.setModifyTime(LocalDateTime.now());
        directoryRepository.save(dir);

        // 记录操作日志
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType("mkdir");
        log.setTarget(dirName);
        log.setResult(1);
        log.setIpAddress(ipAddress);
        log.setOperationTime(LocalDateTime.now());
        operationLogRepository.save(log);
    }

    @Override
    @Transactional
    public void permanentlyDeleteFiles(List<Long> fileIds, Long userId, String ipAddress) {
        for (Long fileId : fileIds) {
            FileEntity file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("文件不存在: " + fileId));
            if (!file.getIsDeleted() || !file.getUploaderId().equals(userId)) {
                throw new RuntimeException("无权操作该文件: " + fileId);
            }

            // 删除回收站记录
            recycleBinRepository.deleteByFileIdAndUserId(fileId, userId);

            // 删除文件元数据
            fileRepository.delete(file);

            // 注意：存储空间已经在软删除时扣除过了，这里不再扣除

            // 记录操作日志
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setOperationType("permanent_delete");
            log.setTarget(file.getFileName());
            log.setResult(1);
            log.setIpAddress(ipAddress);
            log.setOperationTime(LocalDateTime.now());
            operationLogRepository.save(log);
        }
    }

    @Override
    @Transactional
    public void deleteDirectory(Long dirId, Long userId, String ipAddress) {
        Directory dir = directoryRepository.findByDirIdAndOwnerIdAndIsDeletedFalse(dirId, userId)
                .orElseThrow(() -> new RuntimeException("目录不存在或无权操作"));

        Directory rootDir = directoryRepository
                .findByOwnerIdAndParentDirIdIsNull(userId)
                .orElseThrow(() -> new RuntimeException("用户根目录不存在"));

        long totalDeletedSize = deleteDirectoryRecursive(dirId, userId, rootDir.getDirId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setUsedStorage(user.getUsedStorage() - totalDeletedSize);
        userRepository.save(user);

        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType("delete_dir");
        log.setTarget(dir.getDirName());
        log.setResult(1);
        log.setIpAddress(ipAddress);
        log.setOperationTime(LocalDateTime.now());
        operationLogRepository.save(log);
    }

    private long deleteDirectoryRecursive(Long dirId, Long userId, Long rootDirId) {
        long totalSize = 0L;

        List<FileEntity> files = fileRepository.findByParentDirIdAndUploaderIdAndIsDeletedFalse(dirId, userId);
        for (FileEntity file : files) {
            file.setIsDeleted(true);
            file.setDeleteTime(LocalDateTime.now());
            file.setParentDirId(rootDirId);
            fileRepository.save(file);

            RecycleBin bin = new RecycleBin();
            bin.setUserId(userId);
            bin.setFileId(file.getFileId());
            bin.setDirId(null);
            bin.setDeleteTime(LocalDateTime.now());
            bin.setExpireTime(LocalDateTime.now().plusDays(30));
            bin.setIsPermanent(false);
            recycleBinRepository.save(bin);

            totalSize += file.getSize();
        }

        List<Directory> subDirs = directoryRepository.findByParentDirIdAndOwnerIdAndIsDeletedFalse(dirId, userId);
        for (Directory subDir : subDirs) {
            totalSize += deleteDirectoryRecursive(subDir.getDirId(), userId, rootDirId);
        }

        directoryRepository.deleteById(dirId);

        return totalSize;
    }
}