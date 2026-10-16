package cn.mars.marsprinterhub.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知配置 DTO
 *
 * @author mars
 */
@Data
public class NotificationConfigDTO {
    private String channel;
    private String name;
    private Boolean enabled;
    private Integer sort;
    /**
     * 具体配置对象 (BarkConfig, FeishuConfig 等)
     */
    private Object config;

    @Data
    @NoArgsConstructor
    public static class BarkConfig {
        private String serverUrl = "https://api.day.app";
        private String deviceKey;
    }

    @Data
    @NoArgsConstructor
    public static class TelegramConfig {
        private String botToken;
        private String chatId;
        private ProxyConfig proxy = new ProxyConfig();

        @Data
        @NoArgsConstructor
        public static class ProxyConfig {
            private boolean enabled = false;
            private String host;
            private Integer port;
        }
    }

    @Data
    @NoArgsConstructor
    public static class FeishuConfig {
        private String webhookUrl;
        private String secret;
    }

    @Data
    @NoArgsConstructor
    public static class GotifyConfig {
        private String serverUrl;
        private String appToken;
        private Integer priority = 5;
    }
}
