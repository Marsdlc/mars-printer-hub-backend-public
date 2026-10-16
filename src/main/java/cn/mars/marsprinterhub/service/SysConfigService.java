package cn.mars.marsprinterhub.service;

import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.config.PasskeyProperties;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.mapper.SysAppConfigMapper;
import cn.mars.marsprinterhub.model.entity.SysAppConfig;
import cn.mars.marsprinterhub.model.enums.DeviceConfigItem;
import cn.mars.marsprinterhub.notification.template.NotificationTemplateType;
import cn.mars.marsprinterhub.utils.CryptoUtils;
import cn.mars.marsprinterhub.utils.MaskUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.net.URI;
import java.util.*;

/**
 * 系统配置服务
 * 管理各种系统配置项，包括通知模板开关、设备配置等
 *
 * @author mars
 */
@Slf4j
@Service
public class SysConfigService extends ServiceImpl<SysAppConfigMapper, SysAppConfig> {

    // ==================== 配置 Key 常量 ====================
    private static final String NOTIFICATION_TEMPLATE_SETTINGS_KEY = "notification_template_settings";
    private static final String DEVICE_SETTINGS_KEY = "device_settings";
    private static final String REMOTE_CONFIG_URLS_KEY = "remote_config_urls";
    private static final String ALIYUN_MARKET_APP_CODE = "aliyun_market_app_code";
    private static final String HOME_ASSISTANT_BASE_URL = "home_assistant_base_url";
    private static final String HOME_ASSISTANT_API_KEY = "home_assistant_api_key";
    private static final String HOME_ASSISTANT_SCALE_ENTITY_ID = "home_assistant_scale_entity_id";
    private static final String PUBLIC_ACCESS_URL = "public_access_url";
    private static final String PASSKEY_RP_ID = "passkey_rp_id";
    private static final String PASSKEY_ORIGINS = "passkey_origins";
    private static final String OPEN_REGISTRATION = "open_registration";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PasskeyProperties passkeyProperties;

    // ==================== 初始化 ====================

    @PostConstruct
    public void initSettings() {
        initNotificationTemplateSettings();
        initDeviceSettings();
    }

    private void initNotificationTemplateSettings() {
        Map<String, Boolean> saved = loadSettingsFromDb(NOTIFICATION_TEMPLATE_SETTINGS_KEY);
        Map<String, Boolean> synced = new LinkedHashMap<>();

        for (NotificationTemplateType type : NotificationTemplateType.values()) {
            synced.put(type.name(), saved.getOrDefault(type.name(), true));
        }

        if (!synced.equals(saved)) {
            log.info("同步通知模板设置: {} -> {}", saved.keySet(), synced.keySet());
            saveSettingsToDb(NOTIFICATION_TEMPLATE_SETTINGS_KEY, synced);
        }
    }

    private void initDeviceSettings() {
        Map<String, Boolean> saved = loadSettingsFromDb(DEVICE_SETTINGS_KEY);
        Map<String, Boolean> synced = new LinkedHashMap<>();

        for (DeviceConfigItem item : DeviceConfigItem.values()) {
            synced.put(item.name(), saved.getOrDefault(item.name(), item.isDefaultValue()));
        }

        if (!synced.equals(saved)) {
            log.info("同步设备配置: {} -> {}", saved.keySet(), synced.keySet());
            saveSettingsToDb(DEVICE_SETTINGS_KEY, synced);
        }
    }

    // ==================== 通知模板配置 ====================

    public List<ConfigItemVO> getAllTemplateConfigs() {
        Map<String, Boolean> savedSettings = loadSettingsFromDb(NOTIFICATION_TEMPLATE_SETTINGS_KEY);
        List<ConfigItemVO> result = new ArrayList<>();

        for (NotificationTemplateType type : NotificationTemplateType.values()) {
            boolean enabled = savedSettings.getOrDefault(type.name(), true);
            result.add(new ConfigItemVO(type.name(), type.getDisplayName(), enabled));
        }

        return result;
    }

    public boolean isTemplateEnabled(NotificationTemplateType type) {
        Map<String, Boolean> settings = loadSettingsFromDb(NOTIFICATION_TEMPLATE_SETTINGS_KEY);
        return settings.getOrDefault(type.name(), true);
    }

    public void updateTemplateSettings(Map<String, Boolean> settings) {
        Map<String, Boolean> existingSettings = loadSettingsFromDb(NOTIFICATION_TEMPLATE_SETTINGS_KEY);
        existingSettings.putAll(settings);

        Map<String, Boolean> validSettings = new LinkedHashMap<>();
        for (NotificationTemplateType type : NotificationTemplateType.values()) {
            if (existingSettings.containsKey(type.name())) {
                validSettings.put(type.name(), existingSettings.get(type.name()));
            }
        }

        saveSettingsToDb(NOTIFICATION_TEMPLATE_SETTINGS_KEY, validSettings);
    }

    // ==================== 设备配置 ====================

    public List<ConfigItemVO> getAllDeviceConfigs() {
        Map<String, Boolean> savedSettings = loadSettingsFromDb(DEVICE_SETTINGS_KEY);
        List<ConfigItemVO> result = new ArrayList<>();

        for (DeviceConfigItem item : DeviceConfigItem.values()) {
            boolean enabled = savedSettings.getOrDefault(item.name(), item.isDefaultValue());
            result.add(new ConfigItemVO(item.name(), item.getDisplayName(), enabled));
        }

        return result;
    }

    public boolean isDeviceConfigEnabled(DeviceConfigItem item) {
        Map<String, Boolean> settings = loadSettingsFromDb(DEVICE_SETTINGS_KEY);
        return settings.getOrDefault(item.name(), item.isDefaultValue());
    }

    public void updateDeviceSettings(Map<String, Boolean> settings) {
        Map<String, Boolean> existingSettings = loadSettingsFromDb(DEVICE_SETTINGS_KEY);
        existingSettings.putAll(settings);

        Map<String, Boolean> validSettings = new LinkedHashMap<>();
        for (DeviceConfigItem item : DeviceConfigItem.values()) {
            if (existingSettings.containsKey(item.name())) {
                validSettings.put(item.name(), existingSettings.get(item.name()));
            }
        }

        saveSettingsToDb(DEVICE_SETTINGS_KEY, validSettings);
    }

    // ==================== 远程配置 URL ====================

    /**
     * 获取远程配置 URL
     * @return Map 包含 colorUrl, filamentsUrl, spoolUrl
     */
    public Map<String, String> getRemoteConfigUrls() {
        return loadFromDb(REMOTE_CONFIG_URLS_KEY, new TypeReference<>() {});
    }

    /**
     * 更新远程配置 URL
     * @param urls Map 包含要更新的 URL（colorUrl, filamentsUrl, spoolUrl）
     */
    public void updateRemoteConfigUrls(Map<String, String> urls) {
        Map<String, String> existingUrls = loadFromDb(REMOTE_CONFIG_URLS_KEY, new TypeReference<>() {});
        existingUrls.putAll(urls);
        saveToDb(REMOTE_CONFIG_URLS_KEY, existingUrls);
    }

    // ==================== 阿里云市场 AppCode 配置 ====================

    public void saveAliyunMarketAppCode(String appCode) {
        if (StrUtil.isNotBlank(appCode)) {
            SysAppConfig config = new SysAppConfig();
            config.setConfigKey(ALIYUN_MARKET_APP_CODE);
            config.setConfigValue(encryptAppCode(appCode));
            boolean saved = this.saveOrUpdate(config);
            ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "保存 AppCode 失败");
        }
    }

    public AliyunMarketAppCodeVO getAliyunMarketAppCodeInfo() {
        SysAppConfig config = this.getById(ALIYUN_MARKET_APP_CODE);
        if (config == null || StrUtil.isBlank(config.getConfigValue())) {
            return new AliyunMarketAppCodeVO(null);
        }

        String rawAppCode = decryptAppCode(config.getConfigValue());
        return new AliyunMarketAppCodeVO(MaskUtils.mask(rawAppCode));
    }

    public String getAliyunMarketAppCode() {
        SysAppConfig config = this.getById(ALIYUN_MARKET_APP_CODE);
        ThrowUtils.throwIf(config == null || StrUtil.isBlank(config.getConfigValue()), ErrorCode.NOT_FOUND_ERROR, "阿里云市场 AppCode 未配置");
        return decryptAppCode(config.getConfigValue());
    }

    // ==================== Home Assistant 配置 ====================

    public void saveHomeAssistantConfig(String baseUrl, String apiKey, String scaleEntityId) {
        ThrowUtils.throwIf(StrUtil.isBlank(baseUrl), ErrorCode.PARAMS_ERROR, "Home Assistant 地址不能为空");

        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        savePlainConfig(HOME_ASSISTANT_BASE_URL, normalizedBaseUrl, "保存 Home Assistant 地址失败");
        if (StrUtil.isNotBlank(apiKey)) {
            saveEncryptedConfig(HOME_ASSISTANT_API_KEY, apiKey, "保存 Home Assistant Key 失败");
        }
        String normalizedScaleEntityId = StrUtil.trim(scaleEntityId);
        if (StrUtil.isBlank(normalizedScaleEntityId)) {
            removeConfig(HOME_ASSISTANT_SCALE_ENTITY_ID, "删除电子秤实体 ID 失败");
        } else {
            savePlainConfig(HOME_ASSISTANT_SCALE_ENTITY_ID, normalizedScaleEntityId, "保存电子秤实体 ID 失败");
        }
    }

    public HomeAssistantConfigVO getHomeAssistantConfigInfo() {
        String baseUrl = getPlainConfigValue(HOME_ASSISTANT_BASE_URL);
        String encryptedApiKey = getPlainConfigValue(HOME_ASSISTANT_API_KEY);
        String scaleEntityId = getPlainConfigValue(HOME_ASSISTANT_SCALE_ENTITY_ID);
        String maskedApiKey = null;

        if (StrUtil.isNotBlank(encryptedApiKey)) {
            String rawApiKey = decryptSecret(encryptedApiKey, "读取 Home Assistant Key 失败", "解密 Home Assistant Key 失败");
            maskedApiKey = MaskUtils.mask(rawApiKey);
        }

        return new HomeAssistantConfigVO(baseUrl, maskedApiKey, scaleEntityId);
    }

    public String getHomeAssistantBaseUrl() {
        String baseUrl = getPlainConfigValue(HOME_ASSISTANT_BASE_URL);
        ThrowUtils.throwIf(StrUtil.isBlank(baseUrl), ErrorCode.NOT_FOUND_ERROR, "Home Assistant 地址未配置");
        return baseUrl;
    }

    public String getHomeAssistantApiKey() {
        String encryptedApiKey = getPlainConfigValue(HOME_ASSISTANT_API_KEY);
        ThrowUtils.throwIf(StrUtil.isBlank(encryptedApiKey), ErrorCode.NOT_FOUND_ERROR, "Home Assistant Key 未配置");
        return decryptSecret(encryptedApiKey, "读取 Home Assistant Key 失败", "解密 Home Assistant Key 失败");
    }

    public String getHomeAssistantScaleEntityId() {
        String scaleEntityId = getPlainConfigValue(HOME_ASSISTANT_SCALE_ENTITY_ID);
        ThrowUtils.throwIf(StrUtil.isBlank(scaleEntityId), ErrorCode.NOT_FOUND_ERROR, "Home Assistant 电子秤实体未配置");
        return scaleEntityId;
    }

    // ==================== 公网访问 URL 配置 ====================

    public void savePublicAccessUrl(String publicAccessUrl) {
        String normalizedPublicAccessUrl = normalizeOptionalBaseUrl(publicAccessUrl);
        if (StrUtil.isBlank(normalizedPublicAccessUrl)) {
            removeConfig(PUBLIC_ACCESS_URL, "删除公网访问 URL 失败");
            return;
        }
        savePlainConfig(PUBLIC_ACCESS_URL, normalizedPublicAccessUrl, "保存公网访问 URL 失败");
    }

    public PublicAccessUrlVO getPublicAccessUrlInfo() {
        return new PublicAccessUrlVO(getPlainConfigValue(PUBLIC_ACCESS_URL));
    }

    public String getPublicAccessUrl() {
        String publicAccessUrl = getPlainConfigValue(PUBLIC_ACCESS_URL);
        ThrowUtils.throwIf(StrUtil.isBlank(publicAccessUrl), ErrorCode.NOT_FOUND_ERROR, "公网访问 URL 未配置");
        return publicAccessUrl;
    }

    // ==================== Passkey 配置 ====================

    public PasskeyConfigVO getPasskeyConfigInfo() {
        return new PasskeyConfigVO(getEffectivePasskeyRpId(), getEffectivePasskeyOrigins());
    }

    public void savePasskeyConfig(String rpId, List<String> origins) {
        String normalizedRpId = normalizeRpId(rpId);
        List<String> normalizedOrigins = normalizeOrigins(origins, normalizedRpId);

        savePlainConfig(PASSKEY_RP_ID, normalizedRpId, "保存 Passkey RP ID 失败");
        saveToDb(PASSKEY_ORIGINS, normalizedOrigins);
    }

    public String getEffectivePasskeyRpId() {
        String saved = getPlainConfigValue(PASSKEY_RP_ID);
        return StrUtil.isNotBlank(saved) ? saved : passkeyProperties.getRpId();
    }

    public Set<String> getEffectivePasskeyOrigins() {
        List<String> saved = loadFromDb(PASSKEY_ORIGINS, new TypeReference<>() {});
        List<String> effective = (saved == null || saved.isEmpty()) ? passkeyProperties.getOrigins() : saved;
        return new LinkedHashSet<>(effective);
    }

    // ==================== 开放注册配置 ====================

    public OpenRegistrationConfigVO getOpenRegistrationConfigInfo() {
        return new OpenRegistrationConfigVO(isOpenRegistrationEnabled());
    }

    public void saveOpenRegistrationConfig(Boolean enabled) {
        ThrowUtils.throwIf(enabled == null, ErrorCode.PARAMS_ERROR, "开放自由注册配置不能为空");
        savePlainConfig(OPEN_REGISTRATION, String.valueOf(enabled), "保存开放自由注册配置失败");
    }

    public boolean isOpenRegistrationEnabled() {
        String value = getPlainConfigValue(OPEN_REGISTRATION);
        if (StrUtil.isBlank(value)) {
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    // ==================== 通用方法 ====================

    private <T> T loadFromDb(String configKey, TypeReference<T> typeReference) {
        SysAppConfig config = this.getById(configKey);
        String emptyJson = resolveEmptyJson(typeReference);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            try {
                return objectMapper.readValue(emptyJson, typeReference);
            } catch (JsonProcessingException e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        }

        try {
            return objectMapper.readValue(config.getConfigValue(), typeReference);
        } catch (JsonProcessingException e) {
            log.warn("无法解析配置 {}: {}", configKey, e.getMessage());
            try {
                return objectMapper.readValue(emptyJson, typeReference);
            } catch (JsonProcessingException ex) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        }
    }

    private <T> String resolveEmptyJson(TypeReference<T> typeReference) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        var javaType = typeFactory.constructType(typeReference);
        if (javaType.isCollectionLikeType() || javaType.isArrayType()) {
            return "[]";
        }
        return "{}";
    }

    private void saveToDb(String configKey, Object settings) {
        try {
            String json = objectMapper.writeValueAsString(settings);
            SysAppConfig config = new SysAppConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(json);
            this.saveOrUpdate(config);
        } catch (JsonProcessingException e) {
            log.error("序列化配置 {} 失败", configKey, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private Map<String, Boolean> loadSettingsFromDb(String configKey) {
        return loadFromDb(configKey, new TypeReference<>() {});
    }

    private void saveSettingsToDb(String configKey, Map<String, Boolean> settings) {
        saveToDb(configKey, settings);
    }

    private String encryptAppCode(String appCode) {
        return CryptoUtils.encryptHex(appCode);
    }

    private String decryptAppCode(String encryptedAppCode) {
        return decryptSecret(encryptedAppCode, "读取 AppCode 失败", "解密阿里云市场 AppCode 失败");
    }

    private void savePlainConfig(String configKey, String value, String errorMessage) {
        SysAppConfig config = new SysAppConfig();
        config.setConfigKey(configKey);
        config.setConfigValue(value);
        boolean saved = this.saveOrUpdate(config);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, errorMessage);
    }

    private void saveEncryptedConfig(String configKey, String value, String errorMessage) {
        savePlainConfig(configKey, CryptoUtils.encryptHex(value), errorMessage);
    }

    private void removeConfig(String configKey, String errorMessage) {
        boolean removed = this.removeById(configKey);
        ThrowUtils.throwIf(!removed && this.getById(configKey) != null, ErrorCode.SYSTEM_ERROR, errorMessage);
    }

    private String getPlainConfigValue(String configKey) {
        SysAppConfig config = this.getById(configKey);
        if (config == null || StrUtil.isBlank(config.getConfigValue())) {
            return null;
        }
        return config.getConfigValue();
    }

    private String decryptSecret(String encryptedValue, String readErrorMessage, String logMessage) {
        try {
            return CryptoUtils.decryptStr(encryptedValue);
        } catch (Exception e) {
            log.error(logMessage, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, readErrorMessage);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = StrUtil.trim(baseUrl);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeOptionalBaseUrl(String baseUrl) {
        if (StrUtil.isBlank(baseUrl)) {
            return null;
        }
        return normalizeBaseUrl(baseUrl);
    }

    private String normalizeRpId(String rpId) {
        String normalized = StrUtil.trim(rpId);
        ThrowUtils.throwIf(StrUtil.isBlank(normalized), ErrorCode.PARAMS_ERROR, "RP ID 不能为空");
        ThrowUtils.throwIf(normalized.contains("://"), ErrorCode.PARAMS_ERROR, "RP ID 不能包含协议");
        ThrowUtils.throwIf(normalized.contains("/"), ErrorCode.PARAMS_ERROR, "RP ID 不能包含路径");
        ThrowUtils.throwIf(normalized.contains(":"), ErrorCode.PARAMS_ERROR, "RP ID 不能包含端口");
        return normalized.toLowerCase(Locale.ROOT);
    }

    private List<String> normalizeOrigins(List<String> origins, String rpId) {
        ThrowUtils.throwIf(origins == null || origins.isEmpty(), ErrorCode.PARAMS_ERROR, "Allowed Origins 不能为空");

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String origin : origins) {
            String trimmed = StrUtil.trim(origin);
            if (StrUtil.isBlank(trimmed)) {
                continue;
            }

            URI uri;
            try {
                uri = URI.create(trimmed);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "Allowed Origins 中包含非法 URL");
            }

            ThrowUtils.throwIf(StrUtil.isBlank(uri.getScheme()) || StrUtil.isBlank(uri.getHost()), ErrorCode.PARAMS_ERROR, "Allowed Origins 必须包含协议和主机");
            ThrowUtils.throwIf(uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath()), ErrorCode.PARAMS_ERROR, "Allowed Origins 不能包含路径");
            ThrowUtils.throwIf(uri.getQuery() != null || uri.getFragment() != null, ErrorCode.PARAMS_ERROR, "Allowed Origins 不能包含查询参数或片段");

            String host = uri.getHost().toLowerCase(Locale.ROOT);
            boolean matchesRpId = host.equals(rpId) || host.endsWith("." + rpId);
            ThrowUtils.throwIf(!matchesRpId, ErrorCode.PARAMS_ERROR, "Allowed Origins 必须与 RP ID 匹配");

            StringBuilder builder = new StringBuilder();
            builder.append(uri.getScheme().toLowerCase(Locale.ROOT)).append("://").append(host);
            if (uri.getPort() != -1) {
                builder.append(":").append(uri.getPort());
            }
            normalized.add(builder.toString());
        }

        ThrowUtils.throwIf(normalized.isEmpty(), ErrorCode.PARAMS_ERROR, "Allowed Origins 不能为空");
        return new ArrayList<>(normalized);
    }

    // ==================== VO ====================

    @Data
    @AllArgsConstructor
    public static class ConfigItemVO {
        private String key;
        private String name;
        private Boolean enabled;
    }

    @Data
    @AllArgsConstructor
    public static class AliyunMarketAppCodeVO {
        private String appCode;
    }

    @Data
    @AllArgsConstructor
    public static class HomeAssistantConfigVO {
        private String baseUrl;
        private String apiKey;
        private String scaleEntityId;
    }

    @Data
    @AllArgsConstructor
    public static class PublicAccessUrlVO {
        private String publicAccessUrl;
    }

    @Data
    @AllArgsConstructor
    public static class PasskeyConfigVO {
        private String rpId;
        private Set<String> origins;
    }

    @Data
    @AllArgsConstructor
    public static class OpenRegistrationConfigVO {
        private Boolean enabled;
    }
}
