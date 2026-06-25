/*与注册和登录相关，
UserService 是一个接口，
它定义了业务层需要实现的方法规范，而具体的逻辑写在 UserServiceImpl 中。*/

package com.cgrs.driver.service;

import com.cgrs.driver.dto.LoginRequest;
import com.cgrs.driver.dto.RegisterRequest;
import com.cgrs.driver.model.User;

import java.util.Map;

public interface UserService {
    void sendVerifyCode(String email);
    void register(RegisterRequest request);
    // 登录，返回 token 和用户信息
    Map<String, Object> login(LoginRequest request);

    /**
     * 修改密码（需要原密码）
     * @param userId      当前用户ID
     * @param oldPassword 原密码（明文）
     * @param newPassword 新密码（明文）
     * @return true-成功，false-原密码错误
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 使用邮箱验证码重置密码（无需原密码）
     * @param email       用户邮箱
     * @param verifyCode  验证码
     * @param newPassword 新密码（明文）
     */
    void resetPasswordByEmail(String email, String verifyCode, String newPassword);

    /**
     * 检查并更新用户状态（VIP过期、存储空间设置）
     * @param userId 用户ID
     * @return 更新后的用户
     */
    User checkAndUpdateUserStatus(Long userId);
}