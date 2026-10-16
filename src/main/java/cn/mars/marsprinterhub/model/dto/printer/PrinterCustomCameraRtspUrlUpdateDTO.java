package cn.mars.marsprinterhub.model.dto.printer;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新打印机自定义摄像头RTSP路径请求 DTO
 */
@Data
public class PrinterCustomCameraRtspUrlUpdateDTO implements Serializable {

    /**
     * 打印机 ID
     */
    @NotNull(message = "打印机 ID 不能为空")
    private Long printerId;

    /**
     * 自定义摄像头RTSP路径
     * 允许为空，表示取消绑定
     */
    private String customCameraRtspUrl;
}
