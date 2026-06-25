/*与注册相关，封装邮件发送逻辑。

通过 @Value("${spring.mail.username}") 读取配置文件中的邮箱地址

再用 JavaMailSender 发送到目标邮箱*/
package com.cgrs.driver.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailUtil {

    @Autowired
    private JavaMailSender mailSender;

    // 从配置文件注入发件人邮箱
    @Value("${spring.mail.username}")
    private String from;

    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);   // ← 必须与授权用户相同
        message.setTo(to);
        message.setSubject("【HDFS智能网盘】邮箱验证码");
        message.setText("您的验证码是：" + code + "，有效期5分钟，请勿泄露。");
        mailSender.send(message);
    }
}