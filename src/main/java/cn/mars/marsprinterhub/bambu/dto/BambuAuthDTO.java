package cn.mars.marsprinterhub.bambu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 拓竹云认证相关DTO
 */
public class BambuAuthDTO {
    
    /**
     * 登录请求DTO
     */
    @Data
    public static class LoginRequest {
        private String account;
        private String password;
        
        public LoginRequest(String account, String password) {
            this.account = account;
            this.password = password;
        }
    }
    
    /**
     * 登录响应DTO
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoginResponse {
        private String message;
        private String code;
        private String error;
        
        @JsonProperty("accessToken")
        private String accessToken;
        
        @JsonProperty("refreshToken")
        private String refreshToken;
        
        @JsonProperty("expiresIn")
        private Long expiresIn;
        
        @JsonProperty("refreshExpiresIn")
        private Long refreshExpiresIn;
        
        @JsonProperty("tfaKey")
        private String tfaKey;
        
        @JsonProperty("accessMethod")
        private String accessMethod;
        
        @JsonProperty("loginType")
        private String loginType;
    }
    
    /**
     * 验证码请求DTO
     */
    @Data
    public static class VerificationCodeRequest {
        private String email;
        private String type;
        
        public VerificationCodeRequest(String email, String type) {
            this.email = email;
            this.type = type;
        }
    }
    
    /**
     * 短信验证码请求DTO
     */
    @Data
    public static class SmsCodeRequest {
        private String phone;
        private String type;
        
        public SmsCodeRequest(String phone, String type) {
            this.phone = phone;
            this.type = type;
        }
    }
    
    /**
     * 验证码登录请求DTO
     */
    @Data
    public static class CodeLoginRequest {
        private String account;
        private String code;
        
        public CodeLoginRequest(String account, String code) {
            this.account = account;
            this.code = code;
        }
    }
    
    /**
     * 2FA验证码请求DTO
     */
    @Data
    public static class TfaCodeRequest {
        private String code;
        
        public TfaCodeRequest(String code) {
            this.code = code;
        }
    }
    
    /**
     * 通用响应DTO
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommonResponse {
        private String message;
        private String code;
        private String error;
        private Boolean success;
    }
}
