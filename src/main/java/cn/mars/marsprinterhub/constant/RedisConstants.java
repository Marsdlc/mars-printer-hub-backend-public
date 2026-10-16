package cn.mars.marsprinterhub.constant;

public class RedisConstants {

    /**
     * 设备打印过程图片缓存
     */
    public static final String DEVICE_IMAGES_PREFIX = "device:images:";

    /**
     * 设备最新任务封面图缓存
     */
    public static final String DEVICE_TASK_PREFIX = "device:task:";

    /**
     * 设备任务过程中使用 AMS 记录
     */
    public static final String DEVICE_TASK_AMS_PREFIX = "device:task:ams:";

    /**
     * 设备状态缓存
     */
    public static final String DEVICE_STATUS_PREFIX = "device:status:";

    /**
     * 设备最后状态缓存
     */
    public static final String DEVICE_LAST_STATUS_PREFIX = "device:last:";

    /**
     * 设备归属用户缓存
     */
    public static final String DEVICE_USER_PREFIX = "device:user:";

    /**
     * 耗材汇总缓存
     */
    public static final String SPOOL_SUMMARY_PREFIX = "spool:summary:";

    /**
     * 首页汇总缓存
     */
    public static final String HOME_SUMMARY_PREFIX = "home:summary:";

    /**
     * 用户自动连接设备 id 列表
     */
    public static final String USER_MQTT_AUTO_DEVICES_PREFIX = "user:mqtt:auto:devices:";

    /**
     * 用户订单报价草稿缓存
     */
    public static final String USER_ORDER_QUOTE_DRAFT_KEY = "user:order:quote:draft:";

    /**
     * API Key 鉴权缓存
     */
    public static final String USER_API_KEY_AUTH_PREFIX = "user:api:key:auth:";

    /**
     * VIP 激活码限流前缀
     */
    public static final String VIP_ACTIVATE_LIMIT_PREFIX = "vip:activate:limit:";

}
