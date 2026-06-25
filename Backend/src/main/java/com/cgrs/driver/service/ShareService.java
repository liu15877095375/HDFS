package com.cgrs.driver.service;

import com.cgrs.driver.dto.ShareCreateRequest;
import com.cgrs.driver.model.FileEntity;
import com.cgrs.driver.model.Share;

import java.util.List;
import java.util.Map;

/**
 * 分享服务接口
 */
public interface ShareService {

    /**
     * 创建分享
     */
    Map<String, Object> createShare(Long sharerId, ShareCreateRequest request);

    /**
     * 获取分享详情
     */
    Map<String, Object> getShareDetail(String link, String extractCode);

    /**
     * 获取分享实体（用于内部调用）
     */
    Share getShare(String link);

    /**
     * 获取分享的文件实体
     */
    FileEntity getShareFile(Long fileId);

    /**
     * 删除分享
     */
    void deleteShare(String link, Long userId);

    /**
     * 获取我的分享列表
     */
    List<Map<String, Object>> getMyShares(Long userId);

    /**
     * 转存分享文件到我的网盘
     */
    void saveToDrive(Long userId, String shareLink, String extractCode, Long targetId);
}