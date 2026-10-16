package cn.mars.marsprinterhub.model.enums;

import lombok.Getter;

@Getter
public enum LedNodeEnum {

    CHAMBER_LIGHT("chamber_light"),
    CHAMBER_LIGHT2("chamber_light2"),
    WORK_LIGHT("work_light");

    private final String value;

    LedNodeEnum(String value) {
        this.value = value;
    }
    
}