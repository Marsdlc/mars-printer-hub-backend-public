package cn.mars.marsprinterhub.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知消息模型
 * 统一定义各种通知渠道的消息格式
 * 
 * @author mars
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    
    /**
     * 消息标题
     */
    private String title;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型（info, warning, error, success等）
     */
    private String type;
    
    /**
     * 消息优先级（1-低，2-普通，3-高，4-紧急）
     */
    private Integer priority;
    
    /**
     * 额外参数（用于各通知渠道的特殊配置）
     */
    private Map<String, Object> extras;
    
    /**
     * 消息创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 消息来源
     */
    private String source;
    
    /**
     * 消息模板ID（可选）
     */
    private String templateId;
    
    /**
     * 模板参数（可选）
     */
    private Map<String, Object> templateParams;
    
    /**
     * 创建信息消息
     */
    public static NotificationMessage info(String title, String content) {
        return NotificationMessage.builder()
                .title(title)
                .content(content)
                .type("info")
                .priority(2)
                .createTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建警告消息
     */
    public static NotificationMessage warning(String title, String content) {
        return NotificationMessage.builder()
                .title(title)
                .content(content)
                .type("warning")
                .priority(3)
                .createTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建错误消息
     */
    public static NotificationMessage error(String title, String content) {
        return NotificationMessage.builder()
                .title(title)
                .content(content)
                .type("error")
                .priority(4)
                .createTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建成功消息
     */
    public static NotificationMessage success(String title, String content) {
        return NotificationMessage.builder()
                .title(title)
                .content(content)
                .type("success")
                .priority(2)
                .createTime(LocalDateTime.now())
                .build();
    }
}