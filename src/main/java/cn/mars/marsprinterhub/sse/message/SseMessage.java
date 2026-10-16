package cn.mars.marsprinterhub.sse.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 消息实体
 * 简化的单向通信消息格式。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseMessage<T> {

    private String type;

    private String action;

    private T data;

    private Long timestamp;

    public static SseMessage<String> createSystemMessage(String action, String data) {
        return SseMessage.<String>builder()
                .type("system")
                .action(action)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> SseMessage<T> createNotificationMessage(String action, T data) {
        return SseMessage.<T>builder()
                .type("notification")
                .action(action)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
