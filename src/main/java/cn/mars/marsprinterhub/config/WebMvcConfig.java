package cn.mars.marsprinterhub.config;

import cn.mars.marsprinterhub.openapi.OpenApiKeyInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 统一配置
 * 包含：异步支持、CORS跨域、Open API 密钥拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private OpenApiKeyInterceptor openApiKeyInterceptor;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*"); // 允许所有域
        config.addAllowedMethod("*");        // 允许所有方法(POST,GET,OPTIONS...)
        config.addAllowedHeader("*");        // 允许所有头
        config.setAllowCredentials(true);    // 允许 Cookie
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    /**
     * 配置异步请求支持
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 设置异步请求超时时间为10分钟（600秒）
        configurer.setDefaultTimeout(600000L);
    }

    /**
     * 配置拦截器
     * 注意：拦截器执行顺序由 order 决定，数字越小越先执行
     * 注意：拦截器的路径匹配不包含 context-path
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(openApiKeyInterceptor)
                .addPathPatterns("/open/**")
                .excludePathPatterns("/open/ping")
                .order(2);
    }
}
