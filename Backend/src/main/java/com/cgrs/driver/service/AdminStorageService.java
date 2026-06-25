package com.cgrs.driver.service;

import com.cgrs.driver.dto.BulkStorageQuotaConfigRequest;
import com.cgrs.driver.dto.StorageQuotaConfigRequest;

public interface AdminStorageService {
    boolean configUserStorageQuota(StorageQuotaConfigRequest request);
    
    int bulkConfigStorageQuotaByRole(BulkStorageQuotaConfigRequest request);
}
