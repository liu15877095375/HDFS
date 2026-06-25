package com.cgrs.driver.controller;

import com.cgrs.driver.dto.BulkStorageQuotaConfigRequest;
import com.cgrs.driver.dto.Result;
import com.cgrs.driver.dto.StorageQuotaConfigRequest;
import com.cgrs.driver.model.PackageConfig;
import com.cgrs.driver.model.User;
import com.cgrs.driver.service.AdminStorageService;
import com.cgrs.driver.service.PackageConfigService;
import com.cgrs.driver.dao.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/storage")
@RequiredArgsConstructor
public class AdminStorageController {

    private final AdminStorageService adminStorageService;
    private final UserRepository userRepository;
    private final PackageConfigService packageConfigService;

    @GetMapping("/users")
    public Result<List<User>> listAllUsers() {
        List<User> users = userRepository.findAll();
        return Result.success("获取用户列表成功", users);
    }

    /**
     * 获取所有套餐配置
     */
    @GetMapping("/packages")
    public Result<List<PackageConfig>> listPackageConfigs() {
        List<PackageConfig> configs = packageConfigService.getAllPackageConfigs();
        return Result.success("获取套餐配置成功", configs);
    }

    @PostMapping("/quota")
    public Result<Void> configQuota(@Valid @RequestBody StorageQuotaConfigRequest request) {
        adminStorageService.configUserStorageQuota(request);
        return Result.success("存储限额配置成功");
    }

    /**
     * 批量配置指定角色用户的存储限额
     * role: 0-免费用户, 1-付费用户, 2-管理员
     */
    @PostMapping("/quota/bulk")
    public Result<Map<String, Object>> bulkConfigQuota(@Valid @RequestBody BulkStorageQuotaConfigRequest request) {
        int updatedCount = adminStorageService.bulkConfigStorageQuotaByRole(request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("updatedCount", updatedCount);
        result.put("role", request.getRole());
        
        String roleName;
        switch (request.getRole()) {
            case 0: roleName = "免费用户"; break;
            case 1: roleName = "付费用户"; break;
            case 2: roleName = "管理员"; break;
            default: roleName = "未知角色";
        }
        result.put("roleName", roleName);
        
        return Result.success("批量配置成功，共更新 " + updatedCount + " 个用户", result);
    }
}