package cn.mars.marsprinterhub.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum PrintingAppearanceEnum {

    NORMAL("普通", 1),
    SILK("丝绸", 2),
    METAL("金属", 3),
    MATTE("哑光", 4),
    TRANSPARENT("透明", 5),
    GLOW("荧光", 6),
    COLOR_SHIFT("变色", 7),
    MARBLE("大理石", 8),
    WOOD("木质", 9);

    private final String text;

    private final int value;

    PrintingAppearanceEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static PrintingAppearanceEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PrintingAppearanceEnum spaceTypeEnum : PrintingAppearanceEnum.values()) {
            if (spaceTypeEnum.value == value) {
                return spaceTypeEnum;
            }
        }
        return null;
    }
}