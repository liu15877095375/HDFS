package com.cgrs.driver.controller;

import com.cgrs.driver.dao.DirectoryRepository;
import com.cgrs.driver.dao.FileRepository;
import com.cgrs.driver.dao.OperationLogRepository;
import com.cgrs.driver.dao.UserRepository;
import com.cgrs.driver.dto.Result;
import com.cgrs.driver.dto.UploadResponse;
import com.cgrs.driver.model.Directory;
import com.cgrs.driver.model.FileEntity;
import com.cgrs.driver.model.OperationLog;
import com.cgrs.driver.model.User;
import com.cgrs.driver.service.FileService;
import com.cgrs.driver.service.PackageConfigService;
import com.cgrs.driver.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private DirectoryRepository directoryRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PackageConfigService packageConfigService;

    @Value("${hdfs.uri}")
    private String hdfsUri;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Result<UploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "parent_dir_id", defaultValue = "0") Long parentDirId,
            HttpServletRequest request) throws Exception {

        Long userId = getUserIdFromRequest(request);
        String ipAddress = getClientIp(request);
        UploadResponse response = fileService.uploadFile(file, parentDirId, userId, ipAddress);
        return Result.success("上传成功", response);
    }

    /**
     * 获取当前目录下的文件和文件夹列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listFiles(
            @RequestParam(value = "parent_dir_id", required = false) Long parentDirId,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);

        // 如果没有指定目录，则使用用户的根目录
        if (parentDirId == null) {
            Directory rootDir = directoryRepository.findByOwnerIdAndParentDirIdIsNull(userId)
                    .orElse(null);
            if (rootDir == null) {
                return Result.success("查询成功", Collections.emptyList());
            }
            parentDirId = rootDir.getDirId();
        }

        // 查询该目录下的文件（未被软删除）
        List<FileEntity> files = fileRepository.findByParentDirIdAndUploaderIdAndIsDeletedFalse(parentDirId, userId);
        // 查询该目录下的子目录（未被软删除）
        List<Directory> dirs = directoryRepository.findByParentDirIdAndOwnerIdAndIsDeletedFalse(parentDirId, userId);

        List<Map<String, Object>> result = new ArrayList<>();

        // 先添加目录
        for (Directory dir : dirs) {
            Map<String, Object> map = new HashMap<>();
            map.put("file_id", dir.getDirId());
            map.put("file_name", dir.getDirName());
            map.put("size", 0L);
            map.put("mime_type", "文件夹");
            map.put("upload_time", dir.getCreateTime().toString());
            map.put("is_dir", true);
            result.add(map);
        }

        // 再添加文件
        for (FileEntity fileEntity : files) {
            Map<String, Object> map = new HashMap<>();
            map.put("file_id", fileEntity.getFileId());
            map.put("file_name", fileEntity.getFileName());
            map.put("size", fileEntity.getSize());
            map.put("mime_type", fileEntity.getMimeType());
            map.put("upload_time", fileEntity.getUploadTime().toString());
            map.put("is_dir", false);
            result.add(map);
        }

        return Result.success("查询成功", result);
    }

    /**
     * 获取用户的根目录ID
     */
    @GetMapping("/root-dir")
    public Result<Map<String, Object>> getRootDir(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        Directory rootDir = directoryRepository.findByOwnerIdAndParentDirIdIsNull(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "根目录不存在"));
        Map<String, Object> map = new HashMap<>();
        map.put("dir_id", rootDir.getDirId());
        return Result.success("查询成功", map);
    }

    /**
     * 移动文件到指定文件夹
     * PUT /api/files/move
     */
    @PutMapping("/move")
    public Result<String> moveFile(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        Long fileId = ((Number) requestBody.get("fileId")).longValue();
        Long targetDirId = ((Number) requestBody.get("targetDirId")).longValue();

        // 验证文件存在且属于当前用户
        FileEntity fileEntity = fileRepository.findByFileIdAndUploaderIdAndIsDeletedFalse(fileId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文件不存在"));

        // 验证目标文件夹存在且属于当前用户
        Directory targetDir = directoryRepository.findByDirIdAndOwnerIdAndIsDeletedFalse(targetDirId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "目标文件夹不存在"));

        // 更新文件的父目录
        fileEntity.setParentDirId(targetDirId);
        fileEntity.setModifyTime(LocalDateTime.now());
        fileRepository.save(fileEntity);

        // 记录操作日志
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType("MOVE");
        log.setTarget("文件: " + fileEntity.getFileName());
        operationLogRepository.save(log);

        return Result.success("移动成功");
    }

    /**
     * 下载文件（带限速）
     */
    @GetMapping("/download/{fileId}")
    public void downloadFile(@PathVariable Long fileId,
                             HttpServletRequest request,
                             HttpServletResponse response) throws Exception {
        Long userId = getUserIdFromRequest(request);
        String ipAddress = getClientIp(request);

        FileEntity fileEntity = fileRepository
                .findByFileIdAndUploaderIdAndIsDeletedFalse(fileId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文件不存在"));

        // 获取用户信息，确定限速策略
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileEntity.getFileName() + "\"");
        response.setHeader("Content-Length", String.valueOf(fileEntity.getSize()));

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsUri);
        conf.set("dfs.client.use.datanode.hostname", "true");
        conf.set("io.native.lib.available", "false");
        FileSystem fs = FileSystem.get(new URI(hdfsUri), conf, "root");
        Path hdfsPath = new Path(fileEntity.getHdfsPath());

        // 从套餐配置获取下载速度限制
        final long NANOS_PER_SECOND = 1_000_000_000L;
        long speedLimit = packageConfigService.getDownloadSpeedLimitByRole(user.getRole());
        
        // 判断是否需要限速（阈值：100MB/s，超过则视为不限速）
        final long NO_LIMIT_THRESHOLD = 100L * 1024 * 1024; // 100MB/s
        final boolean needLimit = speedLimit > 0 && speedLimit <= NO_LIMIT_THRESHOLD;

        try (InputStream in = fs.open(hdfsPath);
             OutputStream out = response.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalSent = 0;
            long startTime = System.nanoTime();

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalSent += bytesRead;

                // 根据套餐配置进行限速
                if (needLimit) {
                    long elapsedNanos = System.nanoTime() - startTime;
                    long expectedBytes = (elapsedNanos * speedLimit) / NANOS_PER_SECOND;

                    if (totalSent > expectedBytes) {
                        long sleepNanos = (totalSent * NANOS_PER_SECOND) / speedLimit - elapsedNanos;
                        if (sleepNanos > 0) {
                            long sleepMillis = sleepNanos / 1_000_000;
                            int sleepNanosRem = (int) (sleepNanos % 1_000_000);
                            try {
                                Thread.sleep(sleepMillis, sleepNanosRem);
                            } catch (InterruptedException ignored) {}
                        }
                    }
                }
            }
            out.flush();

            // 记录下载日志
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setOperationType("download");
            log.setTarget(fileEntity.getFileName());
            log.setResult(1);
            log.setIpAddress(ipAddress);
            log.setOperationTime(LocalDateTime.now());
            operationLogRepository.save(log);

        } finally {
            fs.close();
        }
    }

    /**
     * 删除文件（软删除，移入回收站）
     */
    @DeleteMapping("/delete/{fileId}")
    public Result<String> deleteFile(@PathVariable Long fileId, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        String ipAddress = getClientIp(request);
        fileService.deleteFile(fileId, userId, ipAddress);
        return Result.success("删除成功", null);
    }

    /**
     * 回收站文件列表
     */
    @GetMapping("/recycle/list")
    public Result<List<Map<String, Object>>> recycleBinList(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        List<Map<String, Object>> list = fileService.getRecycleBinFiles(userId);
        return Result.success("查询成功", list);
    }

    /**
     * 从回收站恢复文件
     */
    @PutMapping("/restore/{fileId}")
    public Result<String> restoreFile(@PathVariable Long fileId, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        String ipAddress = getClientIp(request);
        fileService.restoreFile(fileId, userId, ipAddress);
        return Result.success("恢复成功", null);
    }

    @DeleteMapping("/permanent-delete")
    public Result<String> permanentDeleteFiles(@RequestBody Map<String, List<Long>> body,
                                               HttpServletRequest request) {
        List<Long> fileIds = body.get("file_ids");
        if (fileIds == null || fileIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要删除的文件");
        }
        Long userId = getUserIdFromRequest(request);
        String ipAddress = getClientIp(request);
        fileService.permanentlyDeleteFiles(fileIds, userId, ipAddress);
        return Result.success("彻底删除成功", null);
    }

    /**
     * 新建文件夹
     */
    @PostMapping("/mkdir")
    public Result<String> createDirectory(@RequestBody Map<String, Object> body,
                                          HttpServletRequest request) {
        String dirName = (String) body.get("dir_name");
        Long parentDirId = body.containsKey("parent_dir_id") ? ((Number) body.get("parent_dir_id")).longValue() : null;
        Long userId = getUserIdFromRequest(request);
        String ipAddress = getClientIp(request);
        fileService.createDirectory(dirName, parentDirId, userId, ipAddress);
        return Result.success("目录创建成功", null);
    }

     /**
     * 删除文件
     */
    @DeleteMapping("/delete-dir/{dirId}")
    public Result<String> deleteDirectory(@PathVariable Long dirId, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        String ipAddress = getClientIp(request);
        fileService.deleteDirectory(dirId, userId, ipAddress);
        return Result.success("删除成功", null);
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
     * 获取客户端真实IP，并处理IPv6环回地址
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