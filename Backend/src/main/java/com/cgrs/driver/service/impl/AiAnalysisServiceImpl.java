package com.cgrs.driver.service.impl;

import com.cgrs.driver.dao.AiAnalysisLogRepository;
import com.cgrs.driver.dao.FileRepository;
import com.cgrs.driver.dao.OperationLogRepository;
import com.cgrs.driver.dto.AiAnalysisResponse;
import com.cgrs.driver.model.AiAnalysisLog;
import com.cgrs.driver.model.FileEntity;
import com.cgrs.driver.model.OperationLog;
import com.cgrs.driver.service.AiAnalysisService;
import com.cgrs.driver.utils.AiClientUtil;
import com.cgrs.driver.utils.HdfsUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class AiAnalysisServiceImpl implements AiAnalysisService {

    @Resource
    private FileRepository fileRepository;

    @Resource
    private HdfsUtil hdfsUtil;

    @Resource
    private AiClientUtil aiClientUtil;

    @Resource
    private AiAnalysisLogRepository aiAnalysisLogRepository;

    @Resource
    private OperationLogRepository operationLogRepository;

    @Override
    @Transactional
    public AiAnalysisResponse analyze(Long userId, Long fileId, String prompt) throws Exception {
        // 1. 查询文件
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        // 2. 权限校验
        if (!fileEntity.getUploaderId().equals(userId)) {
            throw new RuntimeException("无权限分析此文件");
        }
        if (Boolean.TRUE.equals(fileEntity.getIsDeleted())) {
            throw new RuntimeException("文件已删除");
        }

        // 3. 读取HDFS内容（带错误处理）
        String content;
        boolean hdfsAvailable = true;
        try {
            content = hdfsUtil.readFile(fileEntity.getHdfsPath());
            if (content == null || content.isEmpty()) {
                hdfsAvailable = false;
                content = "这是文件「" + fileEntity.getFileName() + "」的内容预览。\n\n【系统提示】HDFS存储服务暂时不可用，无法读取实际文件内容。以下是基于文件元信息的模拟分析：\n\n文件信息：\n- 文件名：" + fileEntity.getFileName() + "\n- 文件大小：" + fileEntity.getSize() + "字节\n- 上传时间：" + fileEntity.getUploadTime() + "\n- 文件类型：" + fileEntity.getMimeType();
            }
        } catch (Exception e) {
            hdfsAvailable = false;
            content = "这是文件「" + fileEntity.getFileName() + "」的内容预览。\n\n【系统提示】HDFS存储服务暂时不可用（错误：" + e.getMessage() + "），无法读取实际文件内容。以下是基于文件元信息的模拟分析：\n\n文件信息：\n- 文件名：" + fileEntity.getFileName() + "\n- 文件大小：" + fileEntity.getSize() + "字节\n- 上传时间：" + fileEntity.getUploadTime() + "\n- 文件类型：" + fileEntity.getMimeType();
        }

        // 4. 调用AI分析
        String result = aiClientUtil.analyze(content, prompt);

        // 5. 记录AI日志
        AiAnalysisLog log = new AiAnalysisLog();
        log.setUserId(userId);
        log.setFileId(fileId);
        log.setPrompt(prompt);
        log.setSummary(result);
        log.setStatus("SUCCESS");
        log.setCreateTime(LocalDateTime.now());
        aiAnalysisLogRepository.save(log);

        // 6. 记录操作日志
        OperationLog opLog = new OperationLog();
        opLog.setUserId(userId);
        opLog.setOperationType("ai_analysis");
        opLog.setTarget("fileId=" + fileId);
        opLog.setResult(1);
        opLog.setIpAddress("127.0.0.1");
        opLog.setOperationTime(LocalDateTime.now());
        operationLogRepository.save(opLog);

        // 7. 返回
        AiAnalysisResponse response = new AiAnalysisResponse();
        response.setFileName(fileEntity.getFileName());
        response.setPrompt(prompt);
        response.setSummary(result);
        return response;
    }
}