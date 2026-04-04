package com.cqie.shortlink_admin.util;

/**
 * 62进制转换工具类
 * 用于将数字转换为62进制字符串（0-9, a-z, A-Z）
 */
public class Base62Util {

    private static final String CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;

    /**
     * 将十进制数字转换为62进制字符串
     *
     * @param num 十进制数字
     * @return 62进制字符串
     */
    public static String encode(long num) {
        if (num == 0) {
            return String.valueOf(CHARS.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(CHARS.charAt((int) (num % BASE)));
            num = num / BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * 将62进制字符串转换为十进制数字
     *
     * @param str 62进制字符串
     * @return 十进制数字
     */
    public static long decode(String str) {
        long result = 0;
        for (int i = 0; i < str.length(); i++) {
            result = result * BASE + CHARS.indexOf(str.charAt(i));
        }
        return result;
    }
}
