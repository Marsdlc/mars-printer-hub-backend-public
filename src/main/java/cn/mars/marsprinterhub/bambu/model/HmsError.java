package cn.mars.marsprinterhub.bambu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 打印机 MQTT 上报的 HMS 错误项。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HmsError {

    /**
     * HMS attr 字段，和 code 组合成完整 HMS_xxxx_xxxx_xxxx_xxxx 错误码。
     */
    private Long attr;

    /**
     * HMS code 字段，和 attr 组合成完整 HMS_xxxx_xxxx_xxxx_xxxx 错误码。
     */
    private Long code;
}
