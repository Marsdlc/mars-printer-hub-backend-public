package cn.mars.marsprinterhub.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Sa-Token 登录拦截器（order=1）
        registry.addInterceptor(new SaInterceptor(handler -> {
                    // 直接校验登录，OPTIONS 请求会自动放行
                    StpUtil.checkLogin();
                }))
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns(
                        "/health/check",
                        "/health/version",
                        "/open/**",
                        "/sse/**",
                        "/webjars/**",
                        "/doc.html",
                        "/v3/api-docs/**",
                        "/user/register",
                        "/user/login",
                        "/user/passkey/login/options",
                        "/user/passkey/login/verify",
                        "/user/system-initialized",
                        "/user/setup-admin",
                        "/hls/**",  // 放行 HLS 静态资源
                        "/license/**",  // 兼容旧版前端的 license 接口
                        "/internal/**"  // 内部接口，通过请求头验证
                )
                .order(1);  // 在 Open API 密钥拦截器之前执行
    }
}
