package cn.mars.marsprinterhub.constant;

/**
 * 通用常量类
 * 定义系统级别的通用常量
 */
public class CommonConstants {

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE_NUM = 1;

    /**
     * 默认每页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大每页大小
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 成功状态码
     */
    public static final int SUCCESS_CODE = 0;

    /**
     * 失败状态码
     */
    public static final int ERROR_CODE = -1;

    /**
     * UTF-8 编码
     */
    public static final String UTF8 = "UTF-8";

    /**
     * 日期格式
     */
    public static class DateFormat {
        public static final String YYYY_MM_DD = "yyyy-MM-dd";
        public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
        public static final String YYYY_MM_DD_HH_MM_SS_SSS = "yyyy-MM-dd HH:mm:ss.SSS";
    }

    /**
     * HTTP 请求头
     */
    public static class Headers {
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String AUTHORIZATION = "Authorization";
        public static final String USER_AGENT = "User-Agent";
        public static final String ACCEPT = "Accept";
    }

    /**
     * Content-Type 常量
     */
    public static class ContentType {
        public static final String APPLICATION_JSON = "application/json";
        public static final String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
        public static final String TEXT_PLAIN = "text/plain";
        public static final String TEXT_HTML = "text/html";
        public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    }

    /**
     * 用户角色
     */
    public static class Role {
        public static final String USER = "user";
        public static final String ADMIN = "admin";
    }

    private CommonConstants() {
        // 私有构造函数，防止实例化
    }
}
