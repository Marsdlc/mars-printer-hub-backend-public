package cn.mars.marsprinterhub.model.enums;

import cn.hutool.core.util.ObjUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.Objects;

@Getter
public enum OrderQuantityOperationEnum {

    INBOUND("入库", 1),
    OUTBOUND("出库", -1);

    private final String text;

    private final Integer deltaFactor;

    OrderQuantityOperationEnum(String text, Integer deltaFactor) {
        this.text = text;
        this.deltaFactor = deltaFactor;
    }

    @JsonCreator
    public static OrderQuantityOperationEnum of(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (OrderQuantityOperationEnum operationEnum : OrderQuantityOperationEnum.values()) {
            if (Objects.equals(operationEnum.name(), value.toUpperCase())) {
                return operationEnum;
            }
        }
        return null;
    }
}
