package cn.mars.marsprinterhub.model.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 拓竹云区域枚举
 */
@Getter
public enum BambuRegionEnum {

    CHINA("China", "中国大陆"),
    OTHER("Other", "海外");

    private final String value;

    private final String text;

    BambuRegionEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static BambuRegionEnum fromValue(String value) {
        if (StrUtil.isBlank(value)) {
            return CHINA;
        }
        for (BambuRegionEnum regionEnum : values()) {
            if (StrUtil.equalsIgnoreCase(regionEnum.value, value)) {
                return regionEnum;
            }
        }
        return null;
    }
}
