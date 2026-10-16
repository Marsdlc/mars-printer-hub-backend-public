package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

/**
 * HMS 错误信息视图对象。
 */
@Data
public class HmsErrorVO {

    /**
     * 标准 HMS 错误码，如 HMS_0300_0100_0001_0007。
     */
    private String code;

    /**
     * 中文错误描述。
     */
    private String message;

    /**
     * 严重级别：fatal/serious/common/info/unknown。
     */
    private String severity;

    /**
     * 模块：mainboard/xcam/ams/toolhead/mc/unknown。
     */
    private String module;
}
