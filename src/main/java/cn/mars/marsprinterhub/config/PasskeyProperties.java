package cn.mars.marsprinterhub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "passkey")
public class PasskeyProperties {

    /**
     * Relying Party ID，例如 localhost / example.com。
     */
    private String rpId = "localhost";

    /**
     * Relying Party 展示名。
     */
    private String rpName = "Mars Printer Hub";

    /**
     * 允许的前端 origin 列表。
     */
    private List<String> origins = new ArrayList<>(List.of("http://localhost:5173", "http://localhost:8123"));

    /**
     * challenge 过期时间，秒。
     */
    private long challengeTtlSeconds = 300;

    /**
     * 浏览器 options 中的超时时间，毫秒。
     */
    private long timeoutMilliseconds = 60000;
}
