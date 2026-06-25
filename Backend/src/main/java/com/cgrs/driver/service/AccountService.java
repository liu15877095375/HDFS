package com.cgrs.driver.service;

import java.util.Map;

/**
 * 账户服务接口
 */
public interface AccountService {

    /**
     * 注销账户
     */
    Map<String, Object> deleteAccount(Long userId, String confirmUsername, String ipAddress);

    /**
     * 恢复账户
     */
    Map<String, Object> restoreAccount(Long userId);

    /**
     * 通过邮箱恢复账户
     */
    Map<String, Object> restoreAccountByEmail(String email);
}