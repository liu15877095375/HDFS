package com.cgrs.driver.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "directory")
@Data
public class Directory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dirId;

    @Column(nullable = false, length = 255)
    private String dirName;

    @Column(nullable = true)
    private Long parentDirId;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime modifyTime = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        modifyTime = LocalDateTime.now();
    }
}