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
 * Bark 通知器
 * 适用于 iOS 设备的轻量级推送通知
 *
 * @author mars
 */
@Slf4j
@RequiredArgsConstructor
public class BarkNotifier extends AbstractNotifier {

    private static final String TYPE = "bark";
    private static final int SUCCESS_CODE = 200;

    private final String serverUrl;
    private final String deviceKey;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(serverUrl) && StringUtils.hasText(deviceKey);
    }

    @Override
    protected NotificationResult doSend(NotificationMessage message) {
        try {
            String url = buildBarkUrl();
            String body = buildRequestBody(message);

            HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .timeout(10_000)
                    .execute();

            if (response.isOk()) {
                JSONObject responseJson = JSONUtil.parseObj(response.body());
                int code = responseJson.getInt("code", -1);

                if (code == SUCCESS_CODE) {
                    return NotificationResult.success(TYPE, "bark_" + System.currentTimeMillis());
                } else {
                    String msg = responseJson.getStr("message", "未知错误");
                    log.warn("[Bark] API 错误: code={}, message={}", code, msg);
                    return NotificationResult.failure(TYPE, "Bark API 错误: " + msg);
                }
            } else {
                log.warn("[Bark] HTTP 请求失败: status={}, body={}", response.getStatus(), response.body());
                return NotificationResult.failure(TYPE,
                        "HTTP 请求失败: " + response.getStatus() + " " + response.body());
            }

        } catch (Exception e) {
            log.error("[Bark] 通知发送异常: {}", e.getMessage(), e);
            return NotificationResult.failure(TYPE, "发送异常: " + e.getMessage());
        }
    }

    /**
     * 构建 Bark URL
     */
    private String buildBarkUrl() {
        String baseUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
        return baseUrl + deviceKey;
    }

    /**
     * 构建请求体
     */
    private String buildRequestBody(NotificationMessage message) {
        JSONObject body = new JSONObject()
                .set("title", message.getTitle())
                .set("body", message.getContent());

        putIfHasText(body, "sound", getSoundByType(message.getType()));
        putIfHasText(body, "icon", getIconByType(message.getType()));
        putIfHasText(body, "group", message.getSource());

        return body.toString();
    }

    private void putIfHasText(JSONObject body, String key, String value) {
        if (StringUtils.hasText(value)) {
            body.set(key, value);
        }
    }

    /**
     * 根据消息类型获取声音
     */
    private String getSoundByType(String type) {
        if (!StringUtils.hasText(type)) {
            return "default";
        }
        return switch (type.toLowerCase()) {
            case "success" -> "birdsong";
            case "warning" -> "alarm";
            case "error" -> "multiwayinvitation";
            default -> "default";
        };
    }

    /**
     * 根据消息类型获取图标
     */
    private String getIconByType(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        return switch (type.toLowerCase()) {
            case "success" ->
                    "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Symbols/Check%20Mark%20Button.png";
            case "warning" ->
                    "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Symbols/Warning.png";
            case "error" ->
                    "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Symbols/Cross%20Mark.png";
            default ->
                    "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Symbols/Information.png";
        };
    }
}