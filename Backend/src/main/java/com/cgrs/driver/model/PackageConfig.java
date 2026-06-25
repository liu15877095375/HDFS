package com.cgrs.driver.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "package_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long configId;

    @Column(nullable = false, unique = true)
    private Integer role;

    @Column(nullable = false)
    private Long storageQuota;

    @Column(nullable = false)
    private String packageName;

    @Column(name = "download_speed_limit")
    private Long downloadSpeedLimit;

    @Column(name = "modify_time")
    private LocalDateTime modifyTime;
}