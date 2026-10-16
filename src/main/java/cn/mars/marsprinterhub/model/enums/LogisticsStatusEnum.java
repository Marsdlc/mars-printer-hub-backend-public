package cn.mars.marsprinterhub.model.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public enum LogisticsStatusEnum {

    WAIT_ACCEPT("WAIT_ACCEPT", "待揽收"),
    ACCEPT("ACCEPT", "已揽收"),
    TRANSPORT("TRANSPORT", "运输中"),
    DELIVERING("DELIVERING", "派件中"),
    AGENT_SIGN("AGENT_SIGN", "已代签收"),
    SIGN("SIGN", "已签收"),
    FAILED("FAILED", "包裹异常"),
    UNKNOWN("UNKNOWN", "未知状态");

    private final String code;

    private final String text;

    LogisticsStatusEnum(String code, String text) {
        this.code = code;
        this.text = text;
    }

    public static LogisticsStatusEnum getByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return UNKNOWN;
        }
        for (LogisticsStatusEnum statusEnum : values()) {
            if (statusEnum.code.equalsIgnoreCase(code)) {
                return statusEnum;
            }
        }
        return UNKNOWN;
    }
}
