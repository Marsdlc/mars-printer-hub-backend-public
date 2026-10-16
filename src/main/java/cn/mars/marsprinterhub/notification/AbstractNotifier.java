package cn.mars.marsprinterhub.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 抽象通知器基类
 * 提供通用的通知器实现逻辑
 *
 * @author mars
 */
@Slf4j
public abstract class AbstractNotifier implements Notifier {

    /**
     * 验证通知消息
     *
     * @param message 消息对象
     * @return 验证结果
     */
    protected boolean validateMessage(NotificationMessage message) {
        if (message == null) {
            log.warn("[{}] 通知消息为空", getType());
            return false;
        }

        if (!StringUtils.hasText(message.getContent())) {
            log.warn("[{}] 通知消息内容为空", getType());
            return false;
        }

        return true;
    }

    /**
     * 预处理消息
     * 子类可重写此方法来进行消息的预处理
     *
     * @param message 原始消息
     * @return 处理后的消息
     */
    protected NotificationMessage preprocessMessage(NotificationMessage message) {
        // 设置默认标题
        if (!StringUtils.hasText(message.getTitle())) {
            message.setTitle("系统通知");
        }

        // 设置默认类型
        if (!StringUtils.hasText(message.getType())) {
            message.setType("info");
        }

        // 设置默认优先级
        if (message.getPriority() == null) {
            message.setPriority(2);
        }

        return message;
    }

    /**
     * 发送前检查
     *
     * @return 检查结果
     */
    protected boolean preCheck() {
        return isAvailable();
    }

    /**
     * 实际发送逻辑
     * 子类必须实现此方法
     *
     * @param message 通知消息
     * @return 发送结果
     */
    protected abstract NotificationResult doSend(NotificationMessage message);

    @Override
    public NotificationResult send(NotificationMessage message) {
        try {
            // 验证消息
            if (!validateMessage(message)) {
                return NotificationResult.failure(getType(), "消息验证失败");
            }

            // 预处理消息
            message = preprocessMessage(message);

            // 发送前检查
            if (!preCheck()) {
                return NotificationResult.failure(getType(), "通知器不可用");
            }

            // 执行发送
            NotificationResult result = doSend(message);

            if (result.isSuccess()) {
                log.info("[{}] 通知发送成功: {}", getType(), message.getTitle());
            } else {
                log.error("[{}] 通知发送失败: {}, 错误: {}",
                        getType(), message.getTitle(), result.getErrorMessage());
            }

            return result;

        } catch (Exception e) {
            log.error("[{}] 通知发送异常: {}", getType(), e.getMessage(), e);
            return NotificationResult.failure(getType(), "发送异常: " + e.getMessage());
        }
    }
}