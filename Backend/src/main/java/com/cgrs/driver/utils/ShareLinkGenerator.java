package com.cgrs.driver.utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 分享链接生成工具
 */
public class ShareLinkGenerator {

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int LINK_LENGTH = 8;
    private static final int CODE_LENGTH = 4;

    /**
     * 生成分享链接标识（8位随机字符）
     */
    public static String generateLink() {
        StringBuilder sb = new StringBuilder(LINK_LENGTH);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < LINK_LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 生成随机提取码（4位大写字母+数字）
     */
    public static String generateExtractCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
