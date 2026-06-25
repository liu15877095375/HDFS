package com.cgrs.driver.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file")
@Data
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 512)
    private String hdfsPath;

    @Column(nullable = false, length = 128)
    private String hashValue;

    @Column(nullable = false)
    private Long size;

    @Column(length = 100)
    private String mimeType;

    @Column(nullable = false)
    private Long uploaderId;

    @Column(nullable = false)
    private Long parentDirId;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    private LocalDateTime deleteTime;

    @Column(nullable = false)
    private LocalDateTime uploadTime = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime modifyTime = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        modifyTime = LocalDateTime.now();
    }
}