package cn.mars.marsprinterhub.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Objects;

@Getter
public enum PrinterStatusEnum {


    FAILED("失败", "FAILED"),
    FINISH("完成", "FINISH"),
    IDLE("空闲", "IDLE"),
    OFFLINE("离线", "OFFLINE"),
    PREPARE("准备中", "PREPARE"),
    RUNNING("打印中", "RUNNING"),
    PAUSE("暂停", "PAUSE"),
    SLICING("切片中", "SLICING"),
    INIT("初始化", "INIT");

    private final String text;

    private final String value;

    PrinterStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static PrinterStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PrinterStatusEnum spaceTypeEnum : PrinterStatusEnum.values()) {
            if (Objects.equals(spaceTypeEnum.value, value)) {
                return spaceTypeEnum;
            }
        }
        return null;
    }
}