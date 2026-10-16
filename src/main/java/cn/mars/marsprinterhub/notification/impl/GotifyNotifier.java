package cn.mars.marsprinterhub.notification.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.mars.marsprinterhub.notification.AbstractNotifier;
import cn.mars.marsprinterhub.notification.NotificationMessage;
import cn.mars.marsprinterhub.notification.NotificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * Gotify 通知器
 * 适用于自建 Gotify 服务器推送通知
 *
 * @author mars
 */
@Slf4j
@RequiredArgsConstructor
public class GotifyNotifier extends AbstractNotifier {

    private static final String TYPE = "gotify";
    private static final int SUCCESS_CODE = 200;

    private final String serverUrl;
    private final String appToken;
    private final Integer priority;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(serverUrl) && StringUtils.hasText(appToken);
    }

    @Override
    protected NotificationResult doSend(NotificationMessage message) {
        try {
            String url = buildGotifyUrl();
            String body = buildRequestBody(message);

            HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .header("X-Gotify-Key", appToken)
                    .body(body)
                    .timeout(10_000)
                    .execute();

            if (response.isOk()) {
                JSONObject responseJson = JSONUtil.parseObj(response.body());
                // Gotify 成功返回包含 id, appid, message, title, priority, date 等字段
                // 只要包含 id 字段通常表示成功
                if (responseJson.containsKey("id")) {
                    return NotificationResult.success(TYPE, "gotify_" + responseJson.get("id"));
                } else {
                    String error = responseJson.getStr("error", "未知错误");
                    String errorDescription = responseJson.getStr("errorDescription", "");
                    String msg = StringUtils.hasText(errorDescription) ? error + ": " + errorDescription : error;
                    log.warn("[Gotify] API 错误: {}", msg);
                    return NotificationResult.failure(TYPE, "Gotify API 错误: " + msg);
                }
            } else {
                log.warn("[Gotify] HTTP 请求失败: status={}, body={}", response.getStatus(), response.body());
                return NotificationResult.failure(TYPE,
                        "HTTP 请求失败: " + response.getStatus() + " " + response.body());
            }

        } catch (Exception e) {
            log.error("[Gotify] 通知发送异常: {}", e.getMessage(), e);
            return NotificationResult.failure(TYPE, "发送异常: " + e.getMessage());
        }
    }

    /**
     * 构建 Gotify URL
     */
    private String buildGotifyUrl() {
        String baseUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
        return baseUrl + "message";
    }

    /**
     * 构建请求体
     */
    private String buildRequestBody(NotificationMessage message) {
        JSONObject body = new JSONObject()
                .set("title", message.getTitle())
                .set("message", message.getContent())
                .set("priority", getPriority(message));
        
        // Map common fields to Gotify extras if needed, 
        // currently Gotify primarily supports markdown display via extras but standard message is plain text by default in some clients.
        // For simplicity we will stick to basic fields.
        // Optionally, could set extras for markdown if supported.
        JSONObject extras = new JSONObject();
        JSONObject clientDisplay = new JSONObject();
        clientDisplay.set("contentType", "text/markdown");
        extras.set("client::display", clientDisplay);
        body.set("extras", extras);

        return body.toString();
    }

    /**
     * 获取优先级
     */
    private int getPriority(NotificationMessage message) {
        // Use configured default priority as baseline or map message types
        int basePriority = this.priority != null ? this.priority : 5;
        
        if (message.getPriority() != null) {
            // Map 1-10 scale (if used) roughly to Gotify 0-10
            return message.getPriority();
        }

        if (StringUtils.hasText(message.getType())) {
            return switch (message.getType().toLowerCase()) {
                case "error" -> Math.min(basePriority + 3, 10); // Higher importance
                case "warning" -> Math.min(basePriority + 1, 9);
                case "success" -> basePriority;
                default -> basePriority;
            };
        }
        
        return basePriority;
    }
}
