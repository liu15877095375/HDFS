package com.cgrs.driver.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recycle_bin")
@Data
public class RecycleBin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recycleId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = true)
    private Long fileId;

    @Column(nullable = true)
    private Long dirId;

    @Column(nullable = false)
    private LocalDateTime deleteTime;

    @Column(nullable = false)
    private LocalDateTime expireTime;

    @Column(nullable = false)
    private Boolean isPermanent = false;   // 0-未彻底删除，1-已物理删除
}