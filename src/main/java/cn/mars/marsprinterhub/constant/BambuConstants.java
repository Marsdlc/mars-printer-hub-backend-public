package cn.mars.marsprinterhub.constant;

/**
 * 拓竹云服务常量
 * 专门针对中国大陆地区的API端点
 */
public class BambuConstants {

    /**
     * 拓竹云API基础URL（中国区域）
     */
    public static final String BASE_URL = "https://api.bambulab.cn";

    /**
     * API端点
     */
    public static class Endpoints {
        /**
         * 用户登录
         */
        public static final String LOGIN = "/v1/user-service/user/login";

        /**
         * 2FA登录
         */
        public static final String TFA_LOGIN = "/api/sign-in/tfa";

        /**
         * 发送邮箱验证码
         */
        public static final String EMAIL_CODE = "/v1/user-service/user/sendemail/code";

        /**
         * 发送短信验证码（中国专用）
         */
        public static final String SMS_CODE = "/v1/user-service/user/sendsmscode";

        /**
         * 获取设备列表
         */
        public static final String DEVICE_BIND = "/v1/iot-service/api/user/bind";

        /**
         * 获取切片设置
         */
        public static final String SLICER_SETTINGS = "/v1/iot-service/api/slicer/setting?version=1.10.0.89";

        /**
         * 获取任务列表
         */
        public static final String TASKS = "/v1/user-service/my/tasks";

        /**
         * 获取项目列表
         */
        public static final String PROJECTS = "/v1/iot-service/api/user/project";

        public static final String MY_PREFERENCE = "/v1/design-user-service/my/preference";
    }

    /**
     * HTTP请求头
     */
    public static class Headers {
        public static final String USER_AGENT = "User-Agent";
        public static final String AUTHORIZATION = "Authorization";
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String APPLICATION_JSON = "application/json";

        /**
         * 默认User-Agent
         */
        public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    }

    /**
     * 请求超时时间（毫秒）
     */
    public static final int REQUEST_TIMEOUT = 10000;

    /**
     * 连接超时时间（毫秒）
     */
    public static final int CONNECT_TIMEOUT = 5000;

    /**
     * 无活动
     */
    public static final Integer TRAY_NONE = 255;

    /**
     * 外挂耗材
     */
    public static final Integer TRAY_EXTERNAL = 254;
}