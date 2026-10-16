package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.dto.NotificationConfigDTO;
import cn.mars.marsprinterhub.notification.NotificationResult;

import java.util.List;

/**
 * 通知配置服务接口
 *
 * @author mars
 */
public interface NotificationConfigService {

    /**
     * 获取所有配置
     */
    List<NotificationConfigDTO> getAllConfigs();

    /**
     * 获取指定渠道配置
     */
    NotificationConfigDTO getConfig(String channel);

    /**
     * 更新配置
     *
     * @param channel 渠道标识
     * @param config  具体的配置对象 (如 BarkConfig)
     */
    void updateConfig(String channel, Object config);

    /**
     * 设置启用状态
     */
    void setEnabled(String channel, boolean enabled);

    /**
     * 更新渠道排序
     * @param sortedChannels 排序后的渠道列表
     */
    void updateSort(List<String> sortedChannels);

    /**
     * 测试配置
     *
     * @param channel 渠道标识
     * @param config  具体的配置对象
     */
    NotificationResult testConfig(String channel, Object config);
}
