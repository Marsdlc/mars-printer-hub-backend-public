package cn.mars.marsprinterhub.sse.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 设备更新通知类
 */
@Data
@Builder
public class DeviceUpdateNotification {

    private String deviceId;

    private String status;

    private Long timestamp;
}
