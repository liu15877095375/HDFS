package com.cgrs.driver.dao;

import com.cgrs.driver.model.RecycleBin;
//import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RecycleBinRepository extends JpaRepository<RecycleBin, Long> {
    // 查询某用户所有未永久删除的回收站记录
    List<RecycleBin> findByUserIdAndIsPermanentFalse(Long userId);// 查用户全部有效回收站记录
    List<RecycleBin> findByFileIdAndUserIdAndIsPermanentFalse(Long fileId, Long userId);// 按文件ID查
    @Transactional
    @Modifying
    @Query("DELETE FROM RecycleBin r WHERE r.fileId = :fileId AND r.userId = :userId")
    void deleteByFileIdAndUserId(Long fileId, Long userId);// 批量删除回收站记录

    @Transactional
    @Modifying
    @Query("DELETE FROM RecycleBin r WHERE r.userId = :userId")
    void deleteByUserId(Long userId);// 按用户清空回收站
}