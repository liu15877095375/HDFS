/*与注册相关，生成6位随机验证码

存入 Redis（key: verify:code:邮箱，有效期5分钟）

校验时从 Redis 取出比对，成功后立刻删除（保证一次性）*/
package com.cgrs.driver.utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class VerificationCodeUtil {
    private static final String PREFIX = "verify:code:";
    private static final long EXPIRE_MINUTES = 5;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 生成6位数字验证码
    public String generateCode() {
        return String.valueOf((int)((Math.random() * 9 + 1) * 100000));
    }

    // 保存验证码与邮箱的绑定关系
    public void saveCode(String email, String code) {
        redisTemplate.opsForValue().set(PREFIX + email, code, EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    // 校验验证码是否正确（成功后删除，保证一次性）
    public boolean verifyCode(String email, String code) {
        String key = PREFIX + email;
        String cachedCode = redisTemplate.opsForValue().get(key);
        if (cachedCode != null && cachedCode.equals(code)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    // 检查是否允许发送（防止频率过高）
    public boolean canSend(String email) {
        String key = "verify:limit:" + email;
        Boolean absent = redisTemplate.opsForValue().setIfAbsent(key, "1", 60, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(absent);
    }
}