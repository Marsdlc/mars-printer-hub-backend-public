package cn.mars.marsprinterhub.model.dto.ams;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新 AMS 温湿度实体请求 DTO
 */
@Data
public class AmsEntityUpdateDTO implements Serializable {

    /**
     * AMS ID
     */
    @NotNull(message = "AMS ID不能为空")
    private Long amsId;

    /**
     * Home Assistant 温度实体 ID
     * 允许为空，表示取消绑定
     */
    private String temperatureEntityId;

    /**
     * Home Assistant 湿度实体 ID
     * 允许为空，表示取消绑定
     */
    private String humidityEntityId;
}
