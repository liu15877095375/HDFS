package com.cgrs.driver.service.impl;
import com.cgrs.driver.dao.DirectoryRepository;
import com.cgrs.driver.dao.UserRepository;
import com.cgrs.driver.dto.LoginRequest;
import com.cgrs.driver.dto.RegisterRequest;
import com.cgrs.driver.model.Directory;
import com.cgrs.driver.model.User;
import com.cgrs.driver.service.PackageConfigService;
import com.cgrs.driver.service.UserService;
import com.cgrs.driver.utils.EmailUtil;
import com.cgrs.driver.utils.JwtUtil;
import com.cgrs.driver.utils.VerificationCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/*写注册功能时创建，与注册和登录功能相关
作用：核心业务逻辑都写在这一层。

sendVerifyCode(email) 的跳转逻辑：

调用 VerificationCodeUtil 检查发送频率限制（60s内不能重复发）
调用 VerificationCodeUtil.generateCode() 生成6位随机码
调用 VerificationCodeUtil.saveCode(email, code) 将验证码存入 Redis（有效期5分钟）
调用 EmailUtil.sendVerificationCode(email, code) 发送邮件
register(request) 的跳转逻辑：

调用 VerificationCodeUtil.verifyCode(email, code) 校验验证码
调用 UserRepository.existsByUsername() 检查用户名是否重复
调用 UserRepository.existsByEmail() 检查邮箱是否重复
调用 BCryptPasswordEncoder.encode(password) 加密密码
创建 User 实体对象并调用 UserRepository.save(user) 存入数据库*/
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VerificationCodeUtil codeUtil;
    @Autowired
    private EmailUtil emailUtil;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private DirectoryRepository directoryRepository;
    @Autowired
    private PackageConfigService packageConfigService;   // 新增

    @Override
    public void sendVerifyCode(String email) {
        // 检查发送频率限制（60秒内不可重复）
        if (!codeUtil.canSend(email)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "发送频率过高，请稍后再试");
        }
        String code = codeUtil.generateCode();
        codeUtil.saveCode(email, code);
        emailUtil.sendVerificationCode(email, code);
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 1. 验证码校验
        if (!codeUtil.verifyCode(request.getEmail(), request.getVerifyCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码错误或已过期");
        }
        // 2. 用户名/邮箱唯一性检查
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已被注册");
        }

        // 3. 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        // role, storageQuota 等使用默认值
        User savedUser = userRepository.save(user);

        // 4. 创建根目录
        Directory rootDir = new Directory();
        rootDir.setDirName("我的文件");
        rootDir.setParentDirId(null);
        rootDir.setOwnerId(savedUser.getUserId());
        rootDir.setIsDeleted(false);
        rootDir.setCreateTime(LocalDateTime.now());
        rootDir.setModifyTime(LocalDateTime.now());
        directoryRepository.save(rootDir);
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        String account = request.getAccount().trim();
        // 查找用户（按用户名或邮箱）
        Optional<User> optionalUser = userRepository.findByUsernameOrEmail(account);
        if (optionalUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        User user = optionalUser.get();

        // 检查账号状态（0-冻结 2-注销）
        if (user.getStatus() != 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已被冻结或注销");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        // 生成 JWT
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());

        // 构造返回的用户信息（不包含密码）
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("user_id", user.getUserId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("role", user.getRole());
        userInfo.put("storage_quota", user.getStorageQuota());
        userInfo.put("used_storage", user.getUsedStorage());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", userInfo);
        return result;
    }


    @Override
    @Transactional
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 校验原密码是否匹配数据库中的哈希值
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return false;
        }

        // 加密新密码并保存
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public void resetPasswordByEmail(String email, String verifyCode, String newPassword) {
        // 1. 校验验证码
        if (!codeUtil.verifyCode(email, verifyCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码错误或已过期");
        }

        // 2. 查找用户
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "该邮箱未注册"));

        // 3. 检查账号状态
        if (user.getStatus() != 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已被冻结或注销");
        }

        // 4. 加密新密码并保存
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public User checkAndUpdateUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        LocalDateTime now = LocalDateTime.now();
        boolean needUpdate = false;

        // 检查VIP是否过期
        if (user.getRole() == 1 && user.getVipExpire() != null && user.getVipExpire().isBefore(now)) {
            // VIP过期，降级为免费用户
            user.setRole(0);
            user.setVipExpire(null);
            needUpdate = true;
        }

        // 商业套餐逻辑：每次登录都根据角色应用对应的存储配额
        // 商业套餐优先级最高，覆盖管理员的手动配置
        Long expectedQuota = packageConfigService.getStorageQuotaByRole(user.getRole());
        if (!user.getStorageQuota().equals(expectedQuota)) {
            user.setStorageQuota(expectedQuota);
            needUpdate = true;
        }

        if (needUpdate) {
            return userRepository.save(user);
        }
        return user;
    }
}

