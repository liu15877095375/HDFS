package com.cgrs.driver.dao;

import com.cgrs.driver.model.Directory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DirectoryRepository extends JpaRepository<Directory, Long> {
    Optional<Directory> findByOwnerIdAndParentDirIdIsNull(Long ownerId);
    Optional<Directory> findByDirIdAndOwnerIdAndIsDeletedFalse(Long dirId, Long ownerId);
    boolean existsByDirNameAndParentDirIdAndOwnerId(String dirName, Long parentDirId, Long ownerId);
    List<Directory> findByOwnerIdAndIsDeletedFalse(Long ownerId);
    List<Directory> findByParentDirIdAndOwnerIdAndIsDeletedFalse(Long parentDirId, Long ownerId);
    // 在 DirectoryRepository 接口中添加：

    @Transactional
    @Modifying
    @Query("UPDATE Directory d SET d.isDeleted = true WHERE d.ownerId = :ownerId AND d.isDeleted = false")
    int markAllDeletedByOwnerId(@Param("ownerId") Long ownerId);

    @Transactional
    @Modifying
    @Query("UPDATE Directory d SET d.isDeleted = false WHERE d.ownerId = :ownerId AND d.isDeleted = true")
    int restoreAllByOwnerId(@Param("ownerId") Long ownerId);
}