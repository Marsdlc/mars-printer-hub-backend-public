package cn.mars.marsprinterhub.notification;

import java.util.concurrent.CompletableFuture;

/**
 * 通知器接口
 * 定义各种通知渠道的统一接口
 * 
 * @author mars
 */
public interface Notifier {
    
    /**
     * 获取通知器类型
     * @return 通知器类型标识（如：email, wechat, dingtalk等）
     */
    String getType();
    
    /**
     * 检查通知器是否可用
     * @return true-可用，false-不可用
     */
    boolean isAvailable();
    
    /**
     * 同步发送通知
     * @param message 通知消息
     * @return 发送结果
     */
    NotificationResult send(NotificationMessage message);
    
    /**
     * 异步发送通知
     * @param message 通知消息
     * @return 异步发送结果
     */
    default CompletableFuture<NotificationResult> sendAsync(NotificationMessage message) {
        return CompletableFuture.supplyAsync(() -> send(message));
    }
    
    /**
     * 测试通知器连接
     * @return 测试结果
     */
    default NotificationResult testConnection() {
        NotificationMessage testMessage = NotificationMessage.builder()
                .title("系统测试")
                .content("这是一条测试消息，用于验证通知渠道是否正常工作")
                .type("info")
                .build();
        return send(testMessage);
    }
}