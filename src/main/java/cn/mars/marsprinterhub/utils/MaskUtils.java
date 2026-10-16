package cn.mars.marsprinterhub.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 脱敏工具
 */
public final class MaskUtils {

    private MaskUtils() {
    }

    public static String mask(String content) {
        return mask(content, 4, 4);
    }

    public static String mask(String content, int prefixLength, int suffixLength) {
        if (StrUtil.isBlank(content)) {
            return null;
        }
        int length = content.length();
        if (length <= prefixLength + suffixLength) {
            return "*".repeat(length);
        }
        return content.substring(0, prefixLength)
                + "*".repeat(length - prefixLength - suffixLength)
                + content.substring(length - suffixLength);
    }
}
