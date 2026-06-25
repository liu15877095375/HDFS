package com.cgrs.driver.dao;

import com.cgrs.driver.model.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByHashValue(String hashValue);
    boolean existsByFileNameAndParentDirIdAndUploaderId(String fileName, Long parentDirId, Long uploaderId);
    // 新增：查询某用户未被软删除的文件
    List<FileEntity> findByUploaderIdAndIsDeletedFalse(Long uploaderId);
    Optional<FileEntity> findByFileIdAndUploaderIdAndIsDeletedFalse(Long fileId, Long uploaderId);
    List<FileEntity> findByFileIdInAndIsDeletedTrue(List<Long> fileIds);
    List<FileEntity> findByParentDirIdAndUploaderIdAndIsDeletedFalse(Long parentDirId, Long uploaderId);

    @Transactional
    @Modifying
    @Query("UPDATE FileEntity f SET f.isDeleted = true WHERE f.uploaderId = :uploaderId AND f.isDeleted = false")
    int markAllDeletedByUploaderId(@Param("uploaderId") Long uploaderId);

    List<FileEntity> findByUploaderIdAndIsDeletedTrue(Long uploaderId);

    @Transactional
    @Modifying
    @Query("UPDATE FileEntity f SET f.isDeleted = false WHERE f.uploaderId = :uploaderId AND f.isDeleted = true")
    int restoreAllByUploaderId(@Param("uploaderId") Long uploaderId);

    // 检查用户是否存在同名且未被删除的文件
    boolean existsByFileNameAndUploaderIdAndIsDeletedFalse(String fileName, Long uploaderId);
    
    // 查询指定目录下未被删除的文件
    List<FileEntity> findByParentDirIdAndIsDeletedFalse(Long parentDirId);
    
    // 查询用户已转存过的相同文件（通过hash值判断）
    List<FileEntity> findByHashValueAndUploaderIdAndIsDeletedFalse(String hashValue, Long uploaderId);
    
    // 查询用户已转存过的相同文件（包括已删除的）
    List<FileEntity> findByHashValueAndUploaderId(String hashValue, Long uploaderId);
    
    // 检查是否有其他用户也引用了同一个HDFS文件（用于秒传共享场景）
    boolean existsByHdfsPathAndUploaderIdNot(String hdfsPath, Long uploaderId);
}