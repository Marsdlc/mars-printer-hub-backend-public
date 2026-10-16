package cn.mars.marsprinterhub.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.mapper.NotificationConfigMapper;
import cn.mars.marsprinterhub.model.entity.NotificationConfig;
import cn.mars.marsprinterhub.model.dto.NotificationConfigDTO;
import cn.mars.marsprinterhub.notification.NotificationMessage;
import cn.mars.marsprinterhub.notification.NotificationResult;
import cn.mars.marsprinterhub.notification.Notifier;
import cn.mars.marsprinterhub.notification.impl.BarkNotifier;
import cn.mars.marsprinterhub.notification.impl.FeishuNotifier;
import cn.mars.marsprinterhub.notification.impl.GotifyNotifier;
import cn.mars.marsprinterhub.notification.impl.TelegramNotifier;
import cn.mars.marsprinterhub.service.NotificationConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 通知配置服务实现
 *
 * @author mars
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConfigServiceImpl extends ServiceImpl<NotificationConfigMapper, NotificationConfig> implements NotificationConfigService {

    private final ObjectMapper objectMapper;

    @Override
    public List<NotificationConfigDTO> getAllConfigs() {
        List<NotificationConfig> entities = list(new LambdaQueryWrapper<NotificationConfig>()
                .orderByAsc(NotificationConfig::getSort));
        List<NotificationConfigDTO> list = new ArrayList<>();
        for (NotificationConfig entity : entities) {
            list.add(convertToDTO(entity));
        }
        return list;
    }

    @Override
    public NotificationConfigDTO getConfig(String channel) {
        NotificationConfig entity = getOne(new LambdaQueryWrapper<NotificationConfig>()
                .eq(NotificationConfig::getChannel, channel));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Notification channel not found: " + channel);
        }
        return convertToDTO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(String channel, Object config) {
        NotificationConfig entity = getOne(new LambdaQueryWrapper<NotificationConfig>()
                .eq(NotificationConfig::getChannel, channel));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Notification channel not found: " + channel);
        }

        try {
            // 确保config是正确的类型，或者直接转JSON
            String jsonConfig = objectMapper.writeValueAsString(config);
            entity.setConfig(jsonConfig);
            updateById(entity);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Failed to serialize config");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setEnabled(String channel, boolean enabled) {
        NotificationConfig entity = getOne(new LambdaQueryWrapper<NotificationConfig>()
                .eq(NotificationConfig::getChannel, channel));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Notification channel not found: " + channel);
        }
        entity.setEnabled(enabled);
        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSort(List<String> sortedChannels) {
        if (sortedChannels == null || sortedChannels.isEmpty()) {
            return;
        }

        // 检查所有渠道是否存在
        List<NotificationConfig> allConfigs = list();

        // 批量更新
        List<NotificationConfig> updates = new ArrayList<>();
        for (int i = 0; i < sortedChannels.size(); i++) {
            String channel = sortedChannels.get(i);
            // 查找对应的实体
            NotificationConfig entity = allConfigs.stream()
                    .filter(e -> e.getChannel().equals(channel))
                    .findFirst()
                    .orElse(null);

            if (entity != null) {
                entity.setSort((i + 1) * 10); // 间隔10，方便后续插入
                updates.add(entity);
            }
        }

        if (!updates.isEmpty()) {
            updateBatchById(updates);
        }
    }

    @Override
    public NotificationResult testConfig(String channel, Object config) {
        Notifier notifier = createNotifier(channel, config);
        if (notifier == null) {
            return NotificationResult.failure(channel, "未知渠道或无效配置");
        }

        NotificationMessage testMessage = NotificationMessage.info(
                "测试通知",
                "这是一条来自Mars Printer Hub的测试通知内容。"
        );

        try {
            return notifier.send(testMessage);
        } catch (Exception e) {
            log.error("测试通知失败", e);
            return NotificationResult.failure(channel, "测试失败: " + e.getMessage());
        }
    }

    private NotificationConfigDTO convertToDTO(NotificationConfig entity) {
        NotificationConfigDTO dto = new NotificationConfigDTO();
        dto.setChannel(entity.getChannel());
        dto.setName(entity.getName());
        dto.setEnabled(entity.getEnabled());
        dto.setSort(entity.getSort());

        try {
            Object configObj = null;
            if (StrUtil.isNotBlank(entity.getConfig())) {
                configObj = switch (entity.getChannel()) {
                    case "bark" -> objectMapper.readValue(entity.getConfig(), NotificationConfigDTO.BarkConfig.class);
                    case "feishu" ->
                            objectMapper.readValue(entity.getConfig(), NotificationConfigDTO.FeishuConfig.class);
                    case "telegram" ->
                            objectMapper.readValue(entity.getConfig(), NotificationConfigDTO.TelegramConfig.class);
                    case "gotify" ->
                            objectMapper.readValue(entity.getConfig(), NotificationConfigDTO.GotifyConfig.class);
                    default -> entity.getConfig(); // Fallback
                };
            }
            dto.setConfig(configObj);
        } catch (JsonProcessingException e) {
            log.error("无法解析渠道配置: {} ", entity.getChannel(), e);
            dto.setConfig(null);
        }

        return dto;
    }

    private Notifier createNotifier(String channel, Object config) {
        try {
            switch (channel) {
                case "bark":
                    NotificationConfigDTO.BarkConfig barkConfig = objectMapper.convertValue(config, NotificationConfigDTO.BarkConfig.class);
                    return new BarkNotifier(barkConfig.getServerUrl(), barkConfig.getDeviceKey());
                case "feishu":
                    NotificationConfigDTO.FeishuConfig feishuConfig = objectMapper.convertValue(config, NotificationConfigDTO.FeishuConfig.class);
                    return new FeishuNotifier(feishuConfig.getWebhookUrl(), feishuConfig.getSecret());
                case "telegram":
                    NotificationConfigDTO.TelegramConfig teleConfig = objectMapper.convertValue(config, NotificationConfigDTO.TelegramConfig.class);
                    String proxyHost = null;
                    Integer proxyPort = null;
                    if (teleConfig.getProxy() != null && teleConfig.getProxy().isEnabled()) {
                        proxyHost = teleConfig.getProxy().getHost();
                        proxyPort = teleConfig.getProxy().getPort();
                    }
                    return new TelegramNotifier(teleConfig.getBotToken(), teleConfig.getChatId(), proxyHost, proxyPort);
                case "gotify":
                    NotificationConfigDTO.GotifyConfig gotifyConfig = objectMapper.convertValue(config, NotificationConfigDTO.GotifyConfig.class);
                    return new GotifyNotifier(gotifyConfig.getServerUrl(), gotifyConfig.getAppToken(), gotifyConfig.getPriority());
                default:
                    return null;
            }
        } catch (IllegalArgumentException e) {
            log.error("Failed to create notifier from config", e);
            return null;
        }
    }
}
