package cn.mars.marsprinterhub.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知发送结果
 * 
 * @author mars
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResult {
    
    /**
     * 是否发送成功
     */
    private boolean success;
    
    /**
     * 消息ID（用于追踪）
     */
    private String messageId;
    
    /**
     * 通知渠道
     */
    private String channel;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
    
    /**
     * 响应数据（原始响应）
     */
    private Object responseData;
    
    /**
     * 创建成功结果
     */
    public static NotificationResult success(String channel, String messageId) {
        return NotificationResult.builder()
                .success(true)
                .channel(channel)
                .messageId(messageId)
                .sendTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static NotificationResult failure(String channel, String errorMessage) {
        return NotificationResult.builder()
                .success(false)
                .channel(channel)
                .errorMessage(errorMessage)
                .sendTime(LocalDateTime.now())
                .build();
    }
}