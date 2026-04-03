package com.cqie.shortlink_project.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ShortLinkUtil {
    private static final String CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * 使用 URL + 当前毫秒时间戳 作为输入，生成 6 位短码
     */
    public static String generateShortCode(String url) {
        long timestamp = System.currentTimeMillis();
        String input = url + timestamp;
        return md5ToBase62(input);
    }

    /**
     * 对任意字符串进行 MD5，然后转换为 6 位 Base62 编码
     */
    private static String md5ToBase62(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            BigInteger num = new BigInteger(1, digest);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                BigInteger[] divRem = num.divideAndRemainder(BigInteger.valueOf(62));
                sb.append(CHARS.charAt(divRem[1].intValue()));
                num = divRem[0];
            }
            return sb.reverse().toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }
}
