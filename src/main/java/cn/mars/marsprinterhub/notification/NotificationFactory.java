package cn.mars.marsprinterhub.notification;

import cn.mars.marsprinterhub.model.dto.NotificationConfigDTO;
import cn.mars.marsprinterhub.notification.impl.BarkNotifier;
import cn.mars.marsprinterhub.notification.impl.FeishuNotifier;
import cn.mars.marsprinterhub.notification.impl.GotifyNotifier;
import cn.mars.marsprinterhub.notification.impl.TelegramNotifier;
import cn.mars.marsprinterhub.service.NotificationConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 通知工厂 - 统一管理和分发通知
 *
 * @author mars
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationFactory {

    private final NotificationConfigService configService;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;
    private volatile Map<String, Notifier> notifiers = Collections.emptyMap();

    @PostConstruct
    public void init() {
        refresh();
    }

    /**
     * 刷新通知器缓存
     */
    public void refresh() {
        initNotifiers();
    }

    /**
     * 初始化通知器
     */
    private synchronized void initNotifiers() {
        try {
            List<NotificationConfigDTO> configs = configService.getAllConfigs();
            if (configs.isEmpty()) {
                log.warn("未配置任何通知渠道");
                this.notifiers = Collections.emptyMap();
                return;
            }

            Map<String, Notifier> newNotifiers = new LinkedHashMap<>();
            for (NotificationConfigDTO config : configs) {
                if (Boolean.TRUE.equals(config.getEnabled())) {
                    Notifier notifier = createNotifier(config);
                    if (notifier != null) {
                        newNotifiers.put(config.getChannel(), notifier);
                    }
                }
            }

            this.notifiers = Collections.unmodifiableMap(newNotifiers);

            if (notifiers.isEmpty()) {
                log.info("没有已启用的通知渠道");
            } else {
                log.info("通知工厂初始化完成，共注册 {} 个通知器", notifiers.size());
            }
        } catch (Exception e) {
            log.error("初始化通知配置失败", e);
            // 发生错误时，保持旧配置不变，或者置空视策略而定。这里选择不覆盖旧配置。
        }
    }

    private Notifier createNotifier(NotificationConfigDTO config) {
        try {
            String channel = config.getChannel();
            Object configObj = config.getConfig();

            if (configObj == null) {
                return null;
            }

            Notifier notifier = null;
            switch (channel) {
                case "bark":
                    if (configObj instanceof NotificationConfigDTO.BarkConfig c) {
                        if (StringUtils.hasText(c.getDeviceKey())) {
                            notifier = new BarkNotifier(c.getServerUrl(), c.getDeviceKey());
                        }
                    }
                    break;
                case "feishu":
                    if (configObj instanceof NotificationConfigDTO.FeishuConfig c) {
                        if (StringUtils.hasText(c.getWebhookUrl())) {
                            notifier = new FeishuNotifier(c.getWebhookUrl(), c.getSecret());
                        }
                    }
                    break;
                case "telegram":
                    if (configObj instanceof NotificationConfigDTO.TelegramConfig c) {
                        String proxyHost = null;
                        Integer proxyPort = null;
                        if (c.getProxy() != null && c.getProxy().isEnabled()) {
                            proxyHost = c.getProxy().getHost();
                            proxyPort = c.getProxy().getPort();
                        }
                        if (StringUtils.hasText(c.getBotToken()) && StringUtils.hasText(c.getChatId())) {
                            notifier = new TelegramNotifier(c.getBotToken(), c.getChatId(), proxyHost, proxyPort);
                        }
                    }
                    break;
                case "gotify":
                    if (configObj instanceof NotificationConfigDTO.GotifyConfig c) {
                        if (StringUtils.hasText(c.getServerUrl()) && StringUtils.hasText(c.getAppToken())) {
                            notifier = new GotifyNotifier(c.getServerUrl(), c.getAppToken(), c.getPriority());
                        }
                    }
                    break;
                default:
                    log.warn("未知通知渠道: {}", channel);
            }
            return notifier;
        } catch (Exception e) {
            log.error("无法为频道创建通知: {}", config.getChannel(), e);
            return null;
        }
    }

    /**
     * 发送通知
     */
    public NotificationResult send(NotificationMessage message, String channel) {
        Map<String, Notifier> currentNotifiers = this.notifiers; // Capture reference

        // 如果没有任何通知器，直接返回成功
        if (currentNotifiers.isEmpty()) {
            return NotificationResult.success("none", "通知功能未配置，跳过发送");
        }

        if (!StringUtils.hasText(channel)) {
            if (!currentNotifiers.isEmpty()) {
                channel = currentNotifiers.keySet().iterator().next();
            } else {
                return NotificationResult.failure("none", "No notifiers available");
            }
        }

        Notifier notifier = currentNotifiers.get(channel);
        if (notifier == null) {
            return NotificationResult.failure(channel, "通知渠道不存在: " + channel);
        }

        return notifier.send(message);
    }

    /**
     * 异步发送通知
     */
    public CompletableFuture<NotificationResult> sendAsync(NotificationMessage message, String channel) {
        // 如果没有任何通知器，直接返回已完成的成功结果
        if (notifiers.isEmpty()) {
            return CompletableFuture.completedFuture(
                    NotificationResult.success("none", "通知功能未配置，跳过发送")
            );
        }

        return CompletableFuture.supplyAsync(() -> send(message, channel), taskExecutor);
    }
}
