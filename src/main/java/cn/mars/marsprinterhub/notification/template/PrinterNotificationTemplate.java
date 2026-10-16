package cn.mars.marsprinterhub.notification.template;

import cn.mars.marsprinterhub.notification.NotificationFactory;
import cn.mars.marsprinterhub.notification.NotificationMessage;
import cn.mars.marsprinterhub.service.SysConfigService;
import cn.mars.marsprinterhub.utils.BambuConvertUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 3D打印机通知模板
 * 提供常用的打印机相关通知消息模板
 *
 * @author mars
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrinterNotificationTemplate {

    private final NotificationFactory notificationFactory;
    private final SysConfigService sysConfigService;

    /**
     * 构建并发送消息
     */
    /**
     * 构建并发送消息
     */
    private void send(
            String type,
            String title,
            String content,
            int priority,
            String channel
    ) {
        // 检查通知功能是否可用
        if (!isNotificationAvailable()) {
            return;
        }

        NotificationMessage message = NotificationMessage.builder()
                .type(type)
                .title(title)
                .content(content)
                .priority(priority)
                .source("Mars Printer Hub")
                .createTime(LocalDateTime.now())
                .build();

        notificationFactory.sendAsync(message, channel);
    }

    /**
     * 检查通知功能是否可用
     */
    private boolean isNotificationAvailable() {
        try {
            // 这里我们简单地检查工厂是否注入成功
            return notificationFactory != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 打印暂停通知
     */
    public void sendPrinterPausedNotification(String printerName) {
        if (!sysConfigService.isTemplateEnabled(NotificationTemplateType.PRINT_PAUSED)) {
            return;
        }
        String title = "⏸️ 打印暂停";
        String content = """
                打印机：%s

                打印任务已暂停，请及时检查设备状态或继续打印。
                """.formatted(printerName);

        send("warning", title, content, Priority.MEDIUM, null);
    }

    /**
     * 打印完成通知
     */
    public void sendPrinterFinishedNotification(String printerName) {
        if (!sysConfigService.isTemplateEnabled(NotificationTemplateType.PRINT_FINISHED)) {
            return;
        }
        String title = "✅ 打印完成";
        String content = """
                打印机：%s

                打印任务已完成，请及时取件并检查打印结果。
                """.formatted(printerName);

        send("info", title, content, Priority.LOW, null);
    }

    /**
     * ams 槽位自动解绑
     */
    public void sendAmsSlotAutoUnbindWarning(
            String printerName, Integer amsNo, Integer slotNo
    ) {
        if (!sysConfigService.isTemplateEnabled(NotificationTemplateType.AMS_SLOT_UNBIND)) {
            return;
        }
        String title = "⚠️ 槽位解绑";
        String content = """
                打印机：%s
                AMS：%s
                槽位序号： %d
                
                请检查 AMS 槽位，及时进行新耗材绑定！
                """.formatted(printerName, BambuConvertUtils.convertAmsName(amsNo, false), slotNo + 1);

        send("warning", title, content, Priority.MEDIUM, null);
    }

    /**
     * ams 槽位变为空
     */
    public void sendAmsSlotChangeEmptyWarning(
            String printerName, Integer amsNo, Integer slotNo
    ) {
        if (!sysConfigService.isTemplateEnabled(NotificationTemplateType.AMS_SLOT_EMPTY)) {
            return;
        }
        String title = "⚠️ 槽位为空";
        String content = """
                打印机：%s
                AMS：%s
                槽位序号： %d
                
                请检查 AMS 槽位，及时进行新耗材绑定！
                """.formatted(printerName, BambuConvertUtils.convertAmsName(amsNo, false), slotNo + 1);

        send("warning", title, content, Priority.MEDIUM, null);
    }

    /**
     * ams 湿度过高
     */
    public void sendAmsHumidityTooHighWarning(
            String printerName, Integer amsNo
    ) {
        if (!sysConfigService.isTemplateEnabled(NotificationTemplateType.AMS_HUMIDITY_HIGH)) {
            return;
        }
        // 将 AMS 槽位号转为字母 A~F
        String amsNoStr;
        if (amsNo != null && amsNo >= 0 && amsNo < 6) {
            amsNoStr = String.valueOf((char) ('A' + amsNo));
        } else {
            amsNoStr = "未知";
        }

        String title = "⚠️ 湿度过高";
        String content = """
                打印机：%s
                AMS编号：%s
                
                请检查 AMS 内部湿度，必要时更换干燥剂！
                """.formatted(printerName, amsNoStr);

        send("warning", title, content, Priority.MEDIUM, null);
    }

    /**
     * 未绑定耗材
     */
    public void sendFilamentNotBoundWarning(
            String printerName, Integer amsSlot
    ) {
        if (!sysConfigService.isTemplateEnabled(NotificationTemplateType.FILAMENT_NOT_BOUND)) {
            return;
        }
        String title = "📦 未绑定耗材";
        String content = """
                打印机：%s
                AMS：%s
                槽位序号：%d
                
                请检查并绑定正确的耗材！
                """.formatted(printerName, BambuConvertUtils.convertAmsName(BambuConvertUtils.getActiveAmsNo(amsSlot), false), BambuConvertUtils.getActiveAmsSlotNo(amsSlot) + 1);
        send("warning", title, content, Priority.MEDIUM, null);
    }

    /**
     * 耗材未找到
     */
    public void sendFilamentNotFoundWarning(
            String printerName, Long spoolId
    ) {
        if (!sysConfigService.isTemplateEnabled(NotificationTemplateType.FILAMENT_NOT_FOUND)) {
            return;
        }
        String title = "📦 耗材未找到";
        String content = """
                打印机：%s
                耗材id：%d
                
                请检查并绑定正确的耗材！
                """.formatted(printerName, spoolId);
        send("warning", title, content, Priority.MEDIUM, null);
    }

    /**
     * 耗材类型不匹配通知
     */
    public void sendFilamentTypeMismatchWarning(
            String printerName, Integer amsSlot, String requiredType, String actualType,
            String spoolName) {
        if (!sysConfigService.isTemplateEnabled(NotificationTemplateType.FILAMENT_TYPE_MISMATCH)) {
            return;
        }
        String title = "⚠️ 耗材类型不匹配";
        String content = """
                打印机：%s
                AMS：%s
                槽位序号：%d
                
                任务需要：%s
                实际绑定：%s
                耗材名称：%s
                
                请检查并更换正确的耗材类型！
                """.formatted(printerName, BambuConvertUtils.convertAmsName(BambuConvertUtils.getActiveAmsNo(amsSlot), false), BambuConvertUtils.getActiveAmsSlotNo(amsSlot) + 1, requiredType, actualType, spoolName);

        send("warning", title, content, Priority.MEDIUM, null);
    }

    /**
     * 耗材重量不足通知
     */
    public void sendFilamentWeightInsufficientWarning(
            String printerName, int amsSlot, String spoolName,
            double requiredWeight, double availableWeight) {
        if (!sysConfigService.isTemplateEnabled(NotificationTemplateType.FILAMENT_WEIGHT_INSUFFICIENT)) {
            return;
        }
        double shortfall = requiredWeight - availableWeight;
        String title = "📦 耗材重量不足";
        String content = """
                打印机：%s
                AMS：%s
                槽位序号：%d
                耗材名称：%s
                
                需要重量：%.2fg
                可用重量：%.2fg
                缺少重量：%.2fg
                
                请及时补充或更换耗材！
                """.formatted(printerName, BambuConvertUtils.convertAmsName(BambuConvertUtils.getActiveAmsNo(amsSlot), false), BambuConvertUtils.getActiveAmsSlotNo(amsSlot) + 1, spoolName, requiredWeight, availableWeight, shortfall);

        send("warning", title, content, Priority.MEDIUM, null);
    }

    /**
     * MQTT 重连结果通知
     *
     * @param reconnectedCount 成功重连的设备数
     * @param failedCount      重连失败的设备数
     */
    public void sendMqttReconnectNotification(int reconnectedCount, int failedCount) {
        if (!sysConfigService.isTemplateEnabled(NotificationTemplateType.MQTT_RECONNECT)) {
            return;
        }
        
        // 如果没有重连任何设备，不发送通知
        if (reconnectedCount == 0 && failedCount == 0) {
            return;
        }

        String title;
        String content;
        
        if (failedCount == 0) {
            title = "✅ MQTT 重连成功";
            content = """
                    成功重连 %d 台设备
                    
                    所有断开的设备已恢复连接。
                    """.formatted(reconnectedCount);
        } else if (reconnectedCount == 0) {
            title = "❌ MQTT 重连失败";
            content = """
                    %d 台设备重连失败
                    
                    请检查网络连接或设备状态。
                    """.formatted(failedCount);
        } else {
            title = "⚠️ MQTT 重连部分成功";
            content = """
                    成功重连：%d 台设备
                    重连失败：%d 台设备
                    
                    请检查失败设备的状态。
                    """.formatted(reconnectedCount, failedCount);
        }

        send("info", title, content, Priority.LOW, null);
    }

    /**
     * 优先级常量
     */
    public static class Priority {
        public static final int LOW = 1;
        public static final int MEDIUM = 2;
        public static final int HIGH = 3;
    }
}
