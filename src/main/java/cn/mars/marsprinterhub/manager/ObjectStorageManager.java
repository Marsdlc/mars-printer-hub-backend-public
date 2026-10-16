package cn.mars.marsprinterhub.manager;

import java.io.File;
import java.io.InputStream;

/**
 * 对象存储管理器接口
 * 简化版本，只包含上传和基本功能
 */
public interface ObjectStorageManager {

    /**
     * 上传对象（文件）
     *
     * @param key  唯一键
     * @param file 文件
     * @return 上传是否成功
     */
    boolean putObject(String key, File file);

    /**
     * 上传对象（输入流）
     *
     * @param key         唯一键
     * @param inputStream 输入流
     * @param length      流的总字节数
     * @return 上传是否成功
     */
    boolean putObject(String key, InputStream inputStream, long length);

    /**
     * 上传对象（字节数组）
     *
     * @param key   唯一键
     * @param bytes 文件字节数组
     * @return 上传是否成功
     */
    boolean putObject(String key, byte[] bytes);

    /**
     * 获取对象访问URL
     *
     * @param key 文件key
     * @return 访问URL
     */
    String getUrl(String key);

    /**
     * 获取对象存储类型
     *
     * @return 存储类型（如："COS", "RUSTFS"）
     */
    String getStorageType();

    /**
     * 检查服务是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();
}