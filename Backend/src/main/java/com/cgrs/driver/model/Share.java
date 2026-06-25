package com.cgrs.driver.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 分享记录实体类
 * 对应数据库 share 表
 */
@Entity
@Table(name = "share")
@Data
public class Share {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shareId;

    @Column(nullable = false, unique = true, length = 128)
    private String shareLink;      // 分享链接唯一标识（8位随机字符）

    @Column(nullable = false)
    private Long sharerId;         // 分享者用户ID

    @Column(nullable = false)
    private Integer targetType;    // 0-文件，1-目录

    @Column(nullable = false)
    private Long targetId;         // 文件ID或目录ID

    @Column(length = 10)
    private String extractCode;    // 提取码（可为空）

    @Column(nullable = false)
    private Integer permission = 1;  // 1-只读，2-可下载，3-可转存

    @Column(nullable = false)
    private LocalDateTime expireTime;  // 过期时间

    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer viewCount = 0;  // 访问次数
}