package com.cgrs.driver.service.impl;

import com.cgrs.driver.dao.PackageConfigRepository;
import com.cgrs.driver.dao.UserRepository;
import com.cgrs.driver.dto.BulkStorageQuotaConfigRequest;
import com.cgrs.driver.dto.StorageQuotaConfigRequest;
import com.cgrs.driver.model.PackageConfig;
import com.cgrs.driver.model.User;
import com.cgrs.driver.service.AdminStorageService;
import com.cgrs.driver.service.PackageConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStorageServiceImpl implements AdminStorageService {

    private final UserRepository userRepository;
    private final PackageConfigService packageConfigService;
    private final PackageConfigRepository packageConfigRepository;

    @Override
    @Transactional
    public boolean configUserStorageQuota(StorageQuotaConfigRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (request.getStorageQuota() < user.getUsedStorage()) {
            throw new RuntimeException("配额不能小于用户已用空间：" + user.getUsedStorage() + "字节");
        }

        user.setStorageQuota(request.getStorageQuota());
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public int bulkConfigStorageQuotaByRole(BulkStorageQuotaConfigRequest request) {
        Integer role = request.getRole();
        Long storageQuota = request.getStorageQuota();

        String packageName = getPackageNameByRole(role);
        
        // 如果请求中提供了下载速度限制，则使用新值，否则保持原有配置
        Long speedLimit = request.getDownloadSpeedLimit();
        if (speedLimit == null) {
            PackageConfig existingConfig = packageConfigRepository.findByRole(role).orElse(null);
            speedLimit = existingConfig != null ? existingConfig.getDownloadSpeedLimit() : null;
        }
        
        packageConfigService.updatePackageConfig(role, storageQuota, speedLimit, packageName);

        List<User> users = userRepository.findByRole(role);
        int updatedCount = 0;
        for (User user : users) {
            if (storageQuota >= user.getUsedStorage()) {
                user.setStorageQuota(storageQuota);
                userRepository.save(user);
                updatedCount++;
            }
        }
        
        return updatedCount;
    }

    private String getPackageNameByRole(Integer role) {
        switch (role) {
            case 0: return "免费套餐";
            case 1: return "VIP套餐";
            case 2: return "管理员";
            default: return "未知套餐";
        }
    }
}