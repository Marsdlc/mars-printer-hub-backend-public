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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 飞书机器人通知器
 * 支持文本消息推送及签名校验
 *
 * @author mars
 */
@Slf4j
@RequiredArgsConstructor
public class FeishuNotifier extends AbstractNotifier {

    private static final String TYPE = "feishu";
    private static final int SUCCESS_CODE = 0;

    private final String webhookUrl;
    private final String secret;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(webhookUrl);
    }

    @Override
    protected NotificationResult doSend(NotificationMessage message) {
        try {
            JSONObject requestBody = buildRequestBody(message);

            HttpResponse response = HttpRequest.post(webhookUrl)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .body(requestBody.toString())
                    .timeout(10_000)
                    .execute();

            if (!response.isOk()) {
                return NotificationResult.failure(TYPE,
                        "HTTP 请求失败: " + response.getStatus() + " " + response.body());
            }

            if (!StringUtils.hasText(response.body())) {
                return NotificationResult.success(TYPE, TYPE + "_" + System.currentTimeMillis());
            }

            JSONObject responseJson = JSONUtil.parseObj(response.body());
            int code = extractStatusCode(responseJson);
            String messageId = responseJson.getStr("msg_id");

            if (code == SUCCESS_CODE) {
                if (!StringUtils.hasText(messageId)) {
                    messageId = TYPE + "_" + System.currentTimeMillis();
                }
                return NotificationResult.success(TYPE, messageId);
            }

            String apiMessage = responseJson.getStr("StatusMessage",
                    responseJson.getStr("msg", "未知错误"));
            log.warn("[Feishu] API 错误: code={}, message={}", code, apiMessage);
            return NotificationResult.failure(TYPE,
                    "Feishu API 错误: " + apiMessage + " (code=" + code + ")");

        } catch (Exception e) {
            log.error("[Feishu] 通知发送异常: {}", e.getMessage(), e);
            return NotificationResult.failure(TYPE, "发送异常: " + e.getMessage());
        }
    }

    /**
     * 构建请求体
     */
    private JSONObject buildRequestBody(NotificationMessage message) {
        JSONObject body = new JSONObject()
                .set("msg_type", "text")
                .set("content", new JSONObject().set("text", formatMessage(message)));

        if (StringUtils.hasText(secret)) {
            long timestamp = System.currentTimeMillis() / 1000;
            body.set("timestamp", String.valueOf(timestamp));
            body.set("sign", generateSignature(timestamp));
        }

        return body;
    }

    /**
     * 生成飞书签名
     */
    private String generateSignature(long timestamp) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signData);
        } catch (Exception e) {
            throw new IllegalStateException("生成飞书签名失败", e);
        }
    }

    /**
     * 格式化消息内容
     */
    private String formatMessage(NotificationMessage message) {
        StringBuilder builder = new StringBuilder();

        String title = StringUtils.hasText(message.getTitle()) ? message.getTitle() : "系统通知";
        builder.append(getTypeIcon(message.getType())).append(" ").append(title);

        if (StringUtils.hasText(message.getContent())) {
            builder.append("\n").append(message.getContent());
        }

        if (StringUtils.hasText(message.getSource())) {
            builder.append("\n来源: ").append(message.getSource());
        }

        LocalDateTime createTime = message.getCreateTime();
        if (createTime != null) {
            builder.append("\n时间: ").append(DateUtils.format(createTime));
        }

        Integer priority = message.getPriority();
        if (priority != null && priority >= 3) {
            builder.append("\n优先级: ").append(priority >= 4 ? "紧急" : "高");
        }

        return builder.toString();
    }

    /**
     * 根据消息类型获取图标
     */
    private String getTypeIcon(String type) {
        if (!StringUtils.hasText(type)) {
            return "\u2139\uFE0F"; // ℹ️
        }

        return switch (type.toLowerCase()) {
            case "success" -> "\u2705"; // ✅
            case "warning" -> "\u26A0\uFE0F"; // ⚠️
            case "error" -> "\u274C"; // ❌
            default -> "\u2139\uFE0F"; // ℹ️
        };
    }

    /**
     * 提取飞书返回的状态码
     */
    private int extractStatusCode(JSONObject responseJson) {
        Integer statusCode = responseJson.getInt("StatusCode");
        if (statusCode != null) {
            return statusCode;
        }
        Integer code = responseJson.getInt("code");
        return code != null ? code : -1;
    }
}
