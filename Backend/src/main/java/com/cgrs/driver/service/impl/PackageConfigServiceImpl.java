package com.cgrs.driver.service.impl;

import com.cgrs.driver.dao.PackageConfigRepository;
import com.cgrs.driver.model.PackageConfig;
import com.cgrs.driver.service.PackageConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PackageConfigServiceImpl implements PackageConfigService {

    private final PackageConfigRepository packageConfigRepository;

    private static final long FREE_USER_QUOTA = 10L * 1024 * 1024 * 1024; // 10GB
    private static final long VIP_USER_QUOTA = 1024L * 1024 * 1024 * 1024; // 1TB
    private static final long ADMIN_QUOTA = 1024L * 1024 * 1024 * 1024; // 1TB
    
    private static final long FREE_USER_SPEED_LIMIT = 1024L * 1024; // 1MB/s
    private static final long VIP_USER_SPEED_LIMIT = 1024L * 1024 * 1024; // 1GB/s (实际上不限速)
    private static final long ADMIN_SPEED_LIMIT = 1024L * 1024 * 1024; // 1GB/s (实际上不限速)

    @Override
    @Transactional
    public void initDefaultConfigs() {
        if (!packageConfigRepository.existsByRole(0)) {
            PackageConfig freeConfig = new PackageConfig();
            freeConfig.setRole(0);
            freeConfig.setStorageQuota(FREE_USER_QUOTA);
            freeConfig.setPackageName("免费套餐");
            freeConfig.setDownloadSpeedLimit(FREE_USER_SPEED_LIMIT);
            freeConfig.setModifyTime(LocalDateTime.now());
            packageConfigRepository.save(freeConfig);
        }

        if (!packageConfigRepository.existsByRole(1)) {
            PackageConfig vipConfig = new PackageConfig();
            vipConfig.setRole(1);
            vipConfig.setStorageQuota(VIP_USER_QUOTA);
            vipConfig.setPackageName("VIP套餐");
            vipConfig.setDownloadSpeedLimit(VIP_USER_SPEED_LIMIT);
            vipConfig.setModifyTime(LocalDateTime.now());
            packageConfigRepository.save(vipConfig);
        }

        if (!packageConfigRepository.existsByRole(2)) {
            PackageConfig adminConfig = new PackageConfig();
            adminConfig.setRole(2);
            adminConfig.setStorageQuota(ADMIN_QUOTA);
            adminConfig.setPackageName("管理员");
            adminConfig.setDownloadSpeedLimit(ADMIN_SPEED_LIMIT);
            adminConfig.setModifyTime(LocalDateTime.now());
            packageConfigRepository.save(adminConfig);
        }
    }

    @Override
    public Long getStorageQuotaByRole(Integer role) {
        return packageConfigRepository.findByRole(role)
                .map(PackageConfig::getStorageQuota)
                .orElse(FREE_USER_QUOTA);
    }

    @Override
    public Long getDownloadSpeedLimitByRole(Integer role) {
        return packageConfigRepository.findByRole(role)
                .map(PackageConfig::getDownloadSpeedLimit)
                .orElse(FREE_USER_SPEED_LIMIT);
    }

    @Override
    @Transactional
    public PackageConfig updatePackageConfig(Integer role, Long storageQuota, Long downloadSpeedLimit, String packageName) {
        PackageConfig config = packageConfigRepository.findByRole(role)
                .orElseThrow(() -> new RuntimeException("套餐配置不存在"));
        
        config.setStorageQuota(storageQuota);
        if (downloadSpeedLimit != null) {
            config.setDownloadSpeedLimit(downloadSpeedLimit);
        }
        if (packageName != null && !packageName.isEmpty()) {
            config.setPackageName(packageName);
        }
        config.setModifyTime(LocalDateTime.now());
        
        return packageConfigRepository.save(config);
    }

    @Override
    public List<PackageConfig> getAllPackageConfigs() {
        return packageConfigRepository.findAll();
    }
}