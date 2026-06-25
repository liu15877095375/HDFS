package com.cgrs.driver.dao;

import com.cgrs.driver.model.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    @Query("SELECT COUNT(o) FROM OperationLog o WHERE o.operationTime >= :startOfDay AND o.operationTime < :endOfDay")
    Long countToday(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT COUNT(o) FROM OperationLog o WHERE o.operationType = :type")
    Long countByOperationType(@Param("type") String type);

    @Query("SELECT COUNT(o) FROM OperationLog o WHERE o.operationTime >= :startOfDay AND o.operationTime < :endOfDay")
    Long countByDate(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT o.userId, u.username, COUNT(o) FROM OperationLog o JOIN User u ON o.userId = u.userId GROUP BY o.userId, u.username ORDER BY COUNT(o) DESC")
    List<Object[]> findTopActiveUsers();
}