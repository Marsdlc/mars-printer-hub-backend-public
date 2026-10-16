package cn.mars.marsprinterhub.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 对象存储内容类型工具
 */
public final class ObjectStorageContentTypeUtils {

    private ObjectStorageContentTypeUtils() {
    }

    public static String detectContentType(String key) {
        if (StrUtil.isBlank(key)) {
            return "application/octet-stream";
        }
        String lowerKey = key.toLowerCase();
        if (lowerKey.endsWith(".png")) {
            return "image/png";
        }
        if (lowerKey.endsWith(".webp")) {
            return "image/webp";
        }
        if (lowerKey.endsWith(".jpg") || lowerKey.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerKey.endsWith(".gif")) {
            return "image/gif";
        }
        return "application/octet-stream";
    }
}
