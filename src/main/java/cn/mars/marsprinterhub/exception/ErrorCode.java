package cn.mars.marsprinterhub.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    NOT_LICENSE(9999, "缺少授权信息"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    TOO_MANY_REQUESTS(42900, "请求过于频繁"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),
    CONNECTION_FAILED(50100, "连接失败"),
    DISCONNECTION_FAILED(50100, "断开连接失败"),
    SEND_MESSAGE_FAILED(50100, "发送消息失败"),
    SUBSCRIBE_FAILED(50100, "订阅主题失败"),
    UNSUBSCRIBE_FAILED(50100, "取消订阅失败"),
    CLIENT_NOT_FOUND(50100, "客户端不存在"),
    DEVICE_NOT_FOUND(50100, "设备不存在"),
    INVALID_CONNECTION_INFO(50100, "无效的连接信息"),
    AUTHENTICATION_FAILED(50100, "认证失败"),
    TIMEOUT(50100, "操作超时"),
    NETWORK_ERROR(50100, "网络错误"),
    PROTOCOL_ERROR(50100, "协议错误"),
    PARSER_ERROR(50100, "解析错误"),
    UNKNOWN_ERROR(50100, "未知错误");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
