package cn.mars.marsprinterhub.notification.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.mars.marsprinterhub.notification.AbstractNotifier;
import cn.mars.marsprinterhub.notification.NotificationMessage;
import cn.mars.marsprinterhub.notification.NotificationResult;
import cn.mars.marsprinterhub.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.LocalDateTime;

/**
 * Telegram 通知器
 * 支持机器人消息推送和代理配置
 *
 * @author mars
 */
@Slf4j
@RequiredArgsConstructor
public class TelegramNotifier extends AbstractNotifier {

    private final String botToken;
    private final String chatId;
    private final String proxyHost;
    private final Integer proxyPort;

    @Override
    public String getType() {
        return "telegram";
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(botToken) && StringUtils.hasText(chatId);
    }

    @Override
    protected NotificationResult doSend(NotificationMessage message) {
        try {
            String url = buildTelegramUrl();
            HttpRequest request = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(buildRequestBody(message))
                    .timeout(15000);

            // 设置代理（如果配置了有效的代理）
            if (StringUtils.hasText(proxyHost) && proxyPort != null && proxyPort > 0) {
                try {
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                    request.setProxy(proxy);
                } catch (Exception e) {
                    log.warn("[Telegram] 代理配置无效: {}:{}", proxyHost, proxyPort, e);
                }
            }

            HttpResponse response = request.execute();

            if (response.isOk()) {
                JSONObject responseJson = JSONUtil.parseObj(response.body());
                boolean ok = responseJson.getBool("ok", false);

                if (ok) {
                    JSONObject result = responseJson.getJSONObject("result");
                    String messageId = result != null ? result.getStr("message_id", "") : "";
                    return NotificationResult.success(getType(), "telegram_" + messageId);
                } else {
                    String description = responseJson.getStr("description", "未知错误");
                    return NotificationResult.failure(getType(), "Telegram API错误: " + description);
                }
            } else {
                return NotificationResult.failure(getType(),
                        "HTTP请求失败: " + response.getStatus() + " " + response.body());
            }

        } catch (Exception e) {
            log.error("[Telegram] 通知发送异常: {}", e.getMessage(), e);
            return NotificationResult.failure(getType(), "发送异常: " + e.getMessage());
        }
    }

    /**
     * 构建Telegram API URL
     */
    private String buildTelegramUrl() {
        // 确保botToken不包含非法字符
        String cleanToken = botToken != null ? botToken.trim() : "";
        return "https://api.telegram.org/" + cleanToken + "/sendMessage";
    }

    /**
     * 构建请求体
     */
    private String buildRequestBody(NotificationMessage message) {
        JSONObject body = new JSONObject();
        body.set("chat_id", chatId);
        body.set("text", formatMessage(message));
        body.set("parse_mode", "Markdown");

        // 禁用网页预览
        body.set("disable_web_page_preview", true);

        return body.toString();
    }

    /**
     * 格式化消息内容
     */
    private String formatMessage(NotificationMessage message) {
        StringBuilder text = new StringBuilder();

        // 添加类型图标
        String icon = getTypeIcon(message.getType());
        text.append(icon).append(" *").append(escapeMarkdown(message.getTitle())).append("*\n\n");

        // 消息内容
        text.append(escapeMarkdown(message.getContent()));

        // 额外信息
        if (StringUtils.hasText(message.getSource())) {
            text.append("\n\n📍 *来源:* ").append(escapeMarkdown(message.getSource()));
        }

        // 时间
        LocalDateTime createTime = message.getCreateTime();
        if (createTime != null) {
            text.append("\n⏰ *时间:* `").append(DateUtils.format(createTime)).append("`");
        }

        // 优先级标识
        if (message.getPriority() != null && message.getPriority() >= 3) {
            text.append("\n\n🔥 *高优先级消息*");
        }

        return text.toString();
    }

    /**
     * 获取消息类型图标
     */
    private String getTypeIcon(String type) {
        if (!StringUtils.hasText(type)) {
            return "ℹ️";
        }

        return switch (type.toLowerCase()) {
            case "info" -> "ℹ️";
            case "success" -> "✅";
            case "warning" -> "⚠️";
            case "error" -> "❌";
            default -> "📢";
        };
    }

    /**
     * 转义Markdown特殊字符
     */
    private String escapeMarkdown(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        return text.replace("*", "\\*")
                .replace("_", "\\_")
                .replace("`", "\\`")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }
}