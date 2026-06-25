package com.cgrs.driver.config;

import com.cgrs.driver.service.PackageConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PackageConfigInitializer {

    private final PackageConfigService packageConfigService;

    @PostConstruct
    public void init() {
        packageConfigService.initDefaultConfigs();
    }
}