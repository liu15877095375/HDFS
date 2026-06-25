/*写注册功能时创建
作用：映射数据库 user 表。
字段名与数据库列名对应（如 user_id、username、password_hash）。

JPA 用 @Entity 标记表示这是一个数据表，
项目启动时会自动创建表（ddl-auto=update）。*/

package com.cgrs.driver.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private Integer role = 0;        // 0-免费 1-付费 2-超级管理员

    @Column(nullable = false)
    private Long storageQuota = 10737418240L;   // 10GB

    @Column(nullable = false)
    private Long usedStorage = 0L;

    private LocalDateTime vipExpire;

    @Column(nullable = false)
    private Integer status = 1;      // 0-冻结 1-正常 2-注销

    @Column(nullable = false, updatable = false)
    private LocalDateTime regTime = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime modifyTime = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        modifyTime = LocalDateTime.now();
    }
}