package com.cgrs.driver.service;

import com.cgrs.driver.model.PackageConfig;

import java.util.List;

public interface PackageConfigService {
    void initDefaultConfigs();
    Long getStorageQuotaByRole(Integer role);
    Long getDownloadSpeedLimitByRole(Integer role);
    PackageConfig updatePackageConfig(Integer role, Long storageQuota, Long downloadSpeedLimit, String packageName);
    List<PackageConfig> getAllPackageConfigs();
}