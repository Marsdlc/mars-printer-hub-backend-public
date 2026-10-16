package cn.mars.marsprinterhub.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

@Getter
public enum PrinterModelEnum {
    X1C("X1C", "BL-P001"),
    P1P("P1P", "C11"),
    P1S("P1S", "C12"),
    X1E("X1E", "C13"),
    A1MINI("A1MINI", "N1"),
    A1("A1", "N2S"),
    A2L("A2L", "N9"),
    X2D("X2D", "N6-V2"),
    P2S("P2S", "N7", "N7-V2"),
    H2C("H2C", "O1C2-V2"),
    H2D("H2D", "O1D", "O1D-V2"),
    H2S("H2S", "O1S");

    private final String text;

    private final String value;

    private final String[] aliases;

    PrinterModelEnum(String text, String value, String... aliases) {
        this.text = text;
        this.value = value;
        this.aliases = aliases;
    }

    /**
     * 根据 value 获取枚举
     */
    public static PrinterModelEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PrinterModelEnum spaceTypeEnum : PrinterModelEnum.values()) {
            if (Objects.equals(spaceTypeEnum.value, value)
                    || Arrays.asList(spaceTypeEnum.aliases).contains(value)) {
                return spaceTypeEnum;
            }
        }
        return null;
    }
}
