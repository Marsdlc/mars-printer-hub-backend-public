package cn.mars.marsprinterhub.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum TaskStatusEnum {

    SUCCESS("成功", 2),
    CANCEL("已取消", 3),
    PRINTING("正在打印", 4);

    private final String text;

    private final int value;

    TaskStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static TaskStatusEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (TaskStatusEnum spaceTypeEnum : TaskStatusEnum.values()) {
            if (spaceTypeEnum.value == value) {
                return spaceTypeEnum;
            }
        }
        return null;
    }
}