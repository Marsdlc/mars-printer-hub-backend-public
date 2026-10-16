package cn.mars.marsprinterhub.bambu.client;

import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.bambu.config.BambuRegionConfig;
import cn.mars.marsprinterhub.bambu.dto.BambuAuthDTO;
import cn.mars.marsprinterhub.bambu.dto.BambuDeviceDTO;
import cn.mars.marsprinterhub.constant.BambuConstants;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.model.dto.bambu.BambuTaskDTO;
import cn.mars.marsprinterhub.model.dto.bambu.BambuTaskListResponseDTO;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.model.enums.BambuRegionEnum;
import cn.mars.marsprinterhub.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static cn.mars.marsprinterhub.exception.ErrorCode.*;

/**
 * 拓竹云HTTP客户端
 * 根据当前用户拓竹区域调用对应 Bambu Cloud API
 */
@Slf4j
@Component
public class BambuCloudClient {

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Resource
    private UserService userService;
    @Resource
    private BambuRegionConfig bambuRegionConfig;

    public BambuCloudClient() {
        // 配置HTTP客户端
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(BambuConstants.CONNECT_TIMEOUT)) // 连接请求超时
                .setResponseTimeout(Timeout.ofMilliseconds(BambuConstants.REQUEST_TIMEOUT))         // 响应超时
                .build();

        // 构建 HttpClient
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        // JSON 解析器
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 用户登录
     */
    public BambuAuthDTO.LoginResponse login(String email, String password) {
        try {
            String url = getBaseUrl() + BambuConstants.Endpoints.LOGIN;
            BambuAuthDTO.LoginRequest request = new BambuAuthDTO.LoginRequest(email, password);

            BambuAuthDTO.LoginResponse response = post(url, request, BambuAuthDTO.LoginResponse.class);

            // 只有当获取到有效token时才保存
            if (response != null && StrUtil.isNotBlank(response.getAccessToken()) && !"verifyCode".equals(response.getLoginType())) {
                valid(email, response);
            }

            return response;
        } catch (Exception e) {
            log.error("拓竹云登录失败", e);
            throw new BusinessException(AUTHENTICATION_FAILED);
        }
    }

    /**
     * 发送短信验证码（中国专用）
     */
    public void sendSmsCode(String phone) {
        try {
            String url = getBaseUrl() + BambuConstants.Endpoints.SMS_CODE;
            BambuAuthDTO.SmsCodeRequest request = new BambuAuthDTO.SmsCodeRequest(phone, "login");

            post(url, request, BambuAuthDTO.CommonResponse.class);
        } catch (Exception e) {
            log.error("发送短信验证码失败", e);
            throw new BusinessException(NETWORK_ERROR);
        }
    }

    /**
     * 发送邮箱验证码
     */
    public void sendEmailCode(String email) {
        try {
            String url = getBaseUrl() + BambuConstants.Endpoints.EMAIL_CODE;
            BambuAuthDTO.VerificationCodeRequest request = new BambuAuthDTO.VerificationCodeRequest(email, "codeLogin");

            post(url, request, BambuAuthDTO.CommonResponse.class);
        } catch (Exception e) {
            log.error("发送邮箱验证码失败", e);
            throw new BusinessException(NETWORK_ERROR);
        }
    }

    /**
     * 使用验证码登录
     */
    public BambuAuthDTO.LoginResponse loginWithCode(String account, String code) {
        try {
            String url = getBaseUrl() + BambuConstants.Endpoints.LOGIN;
            BambuAuthDTO.CodeLoginRequest request = new BambuAuthDTO.CodeLoginRequest(account, code);

            BambuAuthDTO.LoginResponse response = post(url, request, BambuAuthDTO.LoginResponse.class);

            if (response != null && StrUtil.isNotBlank(response.getAccessToken())) {
                valid(account, response);
            }

            return response;
        } catch (Exception e) {
            log.error("拓竹云验证码登录失败", e);
            throw new BusinessException(AUTHENTICATION_FAILED);
        }
    }

    /**
     * 验证登录信息
     *
     * @param account  登录账号
     * @param response 登录响应
     */
    private void valid(String account, BambuAuthDTO.LoginResponse response) {
        String bambuUid = getCurrentUserBambuUid(response.getAccessToken());
        saveBambuTokenToCurrentUser(response.getAccessToken(), account, bambuUid, response.getExpiresIn());
    }

    /**
     * 获取设备列表
     */
    public BambuDeviceDTO.DeviceListResponse getDeviceList() {
        String token = getCurrentUserBambuToken();
        if (StrUtil.isBlank(token)) {
            throw new BusinessException(AUTHENTICATION_FAILED);
        }

        try {
            String url = getBaseUrl() + BambuConstants.Endpoints.DEVICE_BIND;
            return getWithAuth(url, null, BambuDeviceDTO.DeviceListResponse.class);
        } catch (Exception e) {
            log.error("获取设备列表失败", e);
            throw new BusinessException(NETWORK_ERROR);
        }
    }

    /**
     * 获取任务列表
     *
     * @param user       用户上下文，为空时使用当前登录用户
     * @param token      认证 token，为空时使用当前用户 token
     * @param offset     偏移量
     * @param limit      每页数量
     * @param startTime  开始时间戳（秒），0表示不限制
     * @param endTime    结束时间戳（秒），0表示不限制
     * @param statusList 状态列表
     * @param query      搜索关键字
     * @return 任务列表响应
     */
    public BambuTaskListResponseDTO getTaskList(User user, String token, Integer offset, Integer limit, Long startTime, Long endTime, String statusList, String query) {
        try {
            // 构建带分页参数的 URL
            StringBuilder urlBuilder = new StringBuilder(getBaseUrl(user) + BambuConstants.Endpoints.TASKS);
            urlBuilder.append("?offset=").append(offset != null ? offset : 0);
            urlBuilder.append("&limit=").append(limit != null ? limit : 20);
            urlBuilder.append("&startTime=").append(startTime != null ? startTime : 0);
            urlBuilder.append("&endTime=").append(endTime != null ? endTime : 0);
            if (StrUtil.isNotBlank(statusList)) {
                urlBuilder.append("&statusList=").append(statusList);
            }
            if (StrUtil.isNotBlank(query)) {
                urlBuilder.append("&query=").append(java.net.URLEncoder.encode(query, StandardCharsets.UTF_8));
            }
            
            String url = urlBuilder.toString();
            if (StrUtil.isBlank(token)) {
                return getWithAuth(url, null, BambuTaskListResponseDTO.class);
            } else {
                return getWithAuth(url, token, BambuTaskListResponseDTO.class);
            }
        } catch (IOException e) {
            log.error("获取任务列表失败", e);
            throw new BusinessException(UNKNOWN_ERROR);
        }
    }

    /**
     * 获取指定设备的最新任务
     */
    public BambuTaskDTO getLatestTaskForPrinter(User user, String deviceId, String token) {
        if (StrUtil.isBlank(deviceId)) {
            throw new IllegalArgumentException("设备ID不能为空");
        }

        // 获取最近的任务列表（使用默认分页参数）
        BambuTaskListResponseDTO taskList = getTaskList(user, token, 0, 20, 0L, 0L, null, null);
        if (taskList == null || taskList.getHits() == null || taskList.getHits().isEmpty()) {
            return null;
        }

        // 筛选指定设备的任务，并按开始时间排序获取最新的
        return taskList.getHits().stream()
                .filter(task -> deviceId.equals(task.getDeviceId()))
                .max((t1, t2) -> {
                    // 按开始时间排序，最新的在前
                    if (t1.getStartTime() == null && t2.getStartTime() == null) {
                        return 0;
                    }
                    if (t1.getStartTime() == null) {
                        return -1;
                    }
                    if (t2.getStartTime() == null) {
                        return 1;
                    }
                    return t1.getStartTime().compareTo(t2.getStartTime());
                })
                .orElse(null);
    }

    /**
     * 根据任务 ID 获取任务详情
     */
    public BambuTaskDTO getTaskById(User user, Long taskId, String token) {
        if (taskId == null) {
            return null;
        }

        BambuTaskListResponseDTO taskList = getTaskList(user, token, 0, 100, 0L, 0L, null, null);
        if (taskList == null || taskList.getHits() == null || taskList.getHits().isEmpty()) {
            return null;
        }

        return taskList.getHits().stream()
                .filter(task -> taskId.equals(task.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 执行GET请求（带认证）
     */
    private <T> T getWithAuth(String url, String token, Class<T> responseType) throws IOException {
        if (token == null) {
            token = getCurrentUserBambuToken();
        }
        if (StrUtil.isBlank(token)) {
            throw new BusinessException(AUTHENTICATION_FAILED);
        }

        HttpGet httpGet = new HttpGet(url);

        // 设置请求头
        httpGet.setHeader(BambuConstants.Headers.USER_AGENT, BambuConstants.Headers.DEFAULT_USER_AGENT);
        httpGet.setHeader(BambuConstants.Headers.AUTHORIZATION, "Bearer " + token);

        // 使用新的HttpClientResponseHandler方式
        HttpClientResponseHandler<T> responseHandler = response -> handleResponse(response, responseType);

        return httpClient.execute(httpGet, responseHandler);
    }

    /**
     * 执行POST请求
     */
    private <T> T post(String url, Object requestBody, Class<T> responseType) throws IOException {
        HttpPost httpPost = new HttpPost(url);

        // 设置请求头
        httpPost.setHeader(BambuConstants.Headers.USER_AGENT, BambuConstants.Headers.DEFAULT_USER_AGENT);
        httpPost.setHeader(BambuConstants.Headers.CONTENT_TYPE, BambuConstants.Headers.APPLICATION_JSON);

        // 设置请求体
        if (requestBody != null) {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            httpPost.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        }

        // 使用新的HttpClientResponseHandler方式
        HttpClientResponseHandler<T> responseHandler = response -> handleResponse(response, responseType);

        return httpClient.execute(httpPost, responseHandler);
    }

    /**
     * 处理HTTP响应
     */
    private <T> T handleResponse(ClassicHttpResponse response, Class<T> responseType) throws IOException {
        int statusCode = response.getCode();
        HttpEntity entity = response.getEntity();

        if (entity == null) {
            throw new BusinessException(SYSTEM_ERROR);
        }

        String responseBody;
        try {
            responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        } catch (ParseException e) {
            log.error("解析响应体失败", e);
            throw new BusinessException(PARSER_ERROR);
        }

        // 检查HTTP状态码
        if (statusCode >= 400) {
            log.error("API请求失败，状态码: {}, 响应: {}", statusCode, responseBody);

            // 检查特定错误
            if (responseBody.contains("cloudflare") || responseBody.contains("Cloudflare")) {
                throw new BusinessException(NETWORK_ERROR);
            }

            throw new BusinessException(SYSTEM_ERROR);
        }

        if (StrUtil.isBlank(responseBody)) {
            return null;
        }

        // 解析响应
        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (Exception e) {
            log.error("解析响应失败: {}", responseBody, e);
            throw new BusinessException(PARSER_ERROR);
        }
    }

    /**
     * 从JWT token中提取用户名
     */
    private String extractUsernameFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.error("Token不是标准JWT格式");
                return null;
            }

            // 解码JWT payload
            StringBuilder payload = new StringBuilder(parts[1]);
            // 补齐Base64填充
            while (payload.length() % 4 != 0) {
                payload.append("=");
            }

            byte[] decodedBytes = Base64.getDecoder().decode(payload.toString());
            String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);

            // 解析JSON获取用户名
            Map<String, Object> claims = objectMapper.readValue(decodedPayload, new TypeReference<>() {
            });
            return (String) claims.get("username");
        } catch (Exception e) {
            log.error("无法从token中提取用户名: {}", e.getMessage());
            return null;
        }
    }

    public String getCurrentUserBambuUid(String token) {
        try {
            String url = getBaseUrl() + BambuConstants.Endpoints.MY_PREFERENCE;
            HttpGet httpGet = new HttpGet(url);

            httpGet.setHeader("Authorization", "Bearer " + token);
            httpGet.setHeader(BambuConstants.Headers.USER_AGENT, BambuConstants.Headers.DEFAULT_USER_AGENT);

            HttpClientResponseHandler<String> responseHandler = response -> {
                if (response.getCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);

                        // 解析响应获取user_id
                        Map<String, Object> responseData = objectMapper.readValue(responseBody, new TypeReference<>() {
                        });
                        Object uidObj = responseData.get("uid");
                        return uidObj != null ? String.valueOf(uidObj) : null;
                    }
                }
                return null;
            };
            return httpClient.execute(httpGet, responseHandler);
        } catch (Exception e) {
            log.error("无法获取Bambu UID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 脱敏用户名
     */
    private String maskUsername(String username) {
        if (StrUtil.isBlank(username) || username.length() <= 7) {
            return username;
        }
        return username.substring(0, 7) + "*****";
    }

    // Getters

    public String getUsername() {
        return getCurrentUserBambuUsername();
    }

    public String getUserId() {
        return getCurrentUserBambuUserId();
    }

    public boolean isAuthenticated() {
        String token = getCurrentUserBambuToken();
        return StrUtil.isNotBlank(token) && !isTokenExpired();
    }

    /**
     * 获取当前用户的拓竹云token
     */
    private String getCurrentUserBambuToken() {
        try {
            User currentUser = userService.getCurrentUser();
            return currentUser != null ? currentUser.getBambuToken() : null;
        } catch (Exception e) {
            log.warn("获取当前用户拓竹云token失败", e);
            return null;
        }
    }

    /**
     * 获取当前用户的拓竹云用户名
     */
    private String getCurrentUserBambuUsername() {
        try {
            User currentUser = userService.getCurrentUser();
            return currentUser != null ? currentUser.getBambuUsername() : null;
        } catch (Exception e) {
            log.warn("获取当前用户拓竹云用户名失败", e);
            return null;
        }
    }

    /**
     * 获取当前用户的拓竹云用户ID
     */
    private String getCurrentUserBambuUserId() {
        try {
            User currentUser = userService.getCurrentUser();
            return currentUser != null ? currentUser.getBambuUserId() : null;
        } catch (Exception e) {
            log.warn("获取当前用户拓竹云用户ID失败", e);
            return null;
        }
    }

    /**
     * 检查token是否过期
     */
    private boolean isTokenExpired() {
        try {
            User currentUser = userService.getCurrentUser();
            if (currentUser == null || currentUser.getBambuTokenExpireTime() == null) {
                return true;
            }
            return currentUser.getBambuTokenExpireTime().before(new Date());
        } catch (Exception e) {
            log.warn("检查token过期状态失败", e);
            return true;
        }
    }

    /**
     * 保存拓竹云token到当前用户
     */
    private void saveBambuTokenToCurrentUser(String token, String bambuUsername, String bambuUserId, Long expiresIn) {
        try {
            User currentUser = userService.getCurrentUser();
            if (currentUser != null) {
                currentUser.setBambuToken(token);
                currentUser.setBambuUsername(bambuUsername); // 存储登录账号（手机号/邮箱）
                currentUser.setBambuUserId(bambuUserId); // 存储纯数字用户ID

                if (StrUtil.isBlank(bambuUserId)) {
                    log.warn("未能获取到有效的用户ID");
                }

                // 计算过期时间（expiresIn是秒数）
                Date expireTime;
                if (expiresIn != null && expiresIn > 0) {
                    expireTime = new Date(System.currentTimeMillis() + expiresIn * 1000L);
                } else {
                    // 默认30天过期
                    expireTime = new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000);
                }
                currentUser.setBambuTokenExpireTime(expireTime);

                userService.updateById(currentUser);
            }
        } catch (Exception e) {
            log.error("保存拓竹云token失败", e);
        }
    }

    private String getBaseUrl() {
        return getBaseUrl(null);
    }

    private String getBaseUrl(User user) {
        return bambuRegionConfig.getCloudBaseUrl(resolveBambuRegion(user));
    }

    private BambuRegionEnum resolveBambuRegion(User user) {
        try {
            User currentUser = user != null ? user : userService.getCurrentUser();
            if (currentUser == null) {
                return BambuRegionEnum.CHINA;
            }
            BambuRegionEnum regionEnum = BambuRegionEnum.fromValue(currentUser.getBambuRegion());
            return regionEnum == null ? BambuRegionEnum.CHINA : regionEnum;
        } catch (Exception e) {
            log.warn("获取拓竹区域失败，使用默认 China", e);
            return BambuRegionEnum.CHINA;
        }
    }
}
