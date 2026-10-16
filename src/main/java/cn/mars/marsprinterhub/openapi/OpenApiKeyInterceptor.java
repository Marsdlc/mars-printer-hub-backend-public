package cn.mars.marsprinterhub.openapi;

import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.model.entity.UserApiKey;
import cn.mars.marsprinterhub.service.UserApiKeyService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OpenApiKeyInterceptor implements HandlerInterceptor {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";

    @Resource
    private UserApiKeyService userApiKeyService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);
        if (StrUtil.isBlank(apiKey)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "缺少 API Key");
        }

        UserApiKey userApiKey = userApiKeyService.validateApiKey(apiKey);
        request.setAttribute(OpenApiRequestAttributes.OPEN_API_USER_ID, userApiKey.getUserId());
        request.setAttribute(OpenApiRequestAttributes.OPEN_API_KEY_ID, userApiKey.getId());
        userApiKeyService.recordApiKeyUsage(userApiKey.getId(), extractClientIp(request));
        return true;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (StrUtil.isNotBlank(xForwardedFor)) {
            return StrUtil.splitTrim(xForwardedFor, ',').stream().findFirst().orElse(request.getRemoteAddr());
        }
        String xRealIp = request.getHeader(HEADER_X_REAL_IP);
        if (StrUtil.isNotBlank(xRealIp)) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
