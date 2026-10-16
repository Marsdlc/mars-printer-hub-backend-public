package cn.mars.marsprinterhub.utils;

import cn.hutool.crypto.SecureUtil;

import java.nio.charset.StandardCharsets;

/**
 * 对称加解密工具
 */
public final class CryptoUtils {

    private static final byte[] DEFAULT_AES_KEY =
            "mars-default-key".getBytes(StandardCharsets.UTF_8);

    private CryptoUtils() {
    }

    public static String encryptHex(String content) {
        return encryptHex(content, DEFAULT_AES_KEY);
    }

    public static String encryptHex(String content, byte[] key) {
        return SecureUtil.aes(key).encryptHex(content);
    }

    public static String decryptStr(String encryptedContent) {
        return decryptStr(encryptedContent, DEFAULT_AES_KEY);
    }

    public static String decryptStr(String encryptedContent, byte[] key) {
        return SecureUtil.aes(key).decryptStr(encryptedContent);
    }
}
