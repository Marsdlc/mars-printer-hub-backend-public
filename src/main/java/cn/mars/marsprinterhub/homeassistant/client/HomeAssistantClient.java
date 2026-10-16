package cn.mars.marsprinterhub.homeassistant.client;

import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.homeassistant.model.HomeAssistantEntityState;
import cn.mars.marsprinterhub.service.SysConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class HomeAssistantClient {

    private final CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                    .setResponseTimeout(Timeout.ofSeconds(10))
                    .build())
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private SysConfigService sysConfigService;

    public HomeAssistantEntityState getEntityState(String entityId) {
        ThrowUtils.throwIf(StrUtil.isBlank(entityId), ErrorCode.PARAMS_ERROR, "实体 ID 不能为空");

        String baseUrl = sysConfigService.getHomeAssistantBaseUrl();
        String apiKey = sysConfigService.getHomeAssistantApiKey();
        String url = baseUrl + "/api/states/" + entityId;

        HttpGet request = new HttpGet(url);
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

        try {
            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                String responseBody = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());

                if (statusCode < 200 || statusCode >= 300) {
                    log.warn("获取 Home Assistant 实体状态失败: entityId={}, statusCode={}, body={}", entityId, statusCode, responseBody);
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取 Home Assistant 实体状态失败");
                }

                return objectMapper.readValue(responseBody, HomeAssistantEntityState.class);
            });
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("请求 Home Assistant 实体状态异常: entityId={}", entityId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "请求 Home Assistant 失败");
        }
    }
}
