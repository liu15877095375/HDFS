package com.cgrs.driver.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_log")
@Data
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String operationType;

    @Column(length = 255)
    private String target;

    @Column(nullable = false)
    private Integer result = 1;   // 1 代表成功，0 代表失败

    @Column(length = 45)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime operationTime = LocalDateTime.now();
}