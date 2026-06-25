package com.cgrs.driver.service;

import com.cgrs.driver.dto.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface FileService {
    UploadResponse uploadFile(MultipartFile file, Long parentDirId, Long userId, String ipAddress) throws Exception;
    void deleteFile(Long fileId, Long userId, String ipAddress);
    void restoreFile(Long fileId, Long userId, String ipAddress);
    List<Map<String, Object>> getRecycleBinFiles(Long userId);
    void createDirectory(String dirName, Long parentDirId, Long userId, String ipAddress);
    void deleteDirectory(Long dirId, Long userId, String ipAddress);
    void permanentlyDeleteFiles(List<Long> fileIds, Long userId, String ipAddress);
}