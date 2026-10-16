package cn.mars.marsprinterhub.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Objects;

@Getter
public enum OrderStatusEnum {
    
    PROCESSING("进行中", 1),
    COMPLETED("已完成", 2),
    CANCELLED("已取消", 3),
    IN_STOCK("库存中", 4);

    private final String text;

    private final Integer value;

    OrderStatusEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static OrderStatusEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (OrderStatusEnum spaceTypeEnum : OrderStatusEnum.values()) {
            if (Objects.equals(spaceTypeEnum.value, value)) {
                return spaceTypeEnum;
            }
        }
        return null;
    }
}