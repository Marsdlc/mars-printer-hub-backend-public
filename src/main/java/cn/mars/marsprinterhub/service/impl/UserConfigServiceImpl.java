package cn.mars.marsprinterhub.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.mapper.UserConfigMapper;
import cn.mars.marsprinterhub.model.dto.user.UserConfigDTO;
import cn.mars.marsprinterhub.model.entity.UserConfig;
import cn.mars.marsprinterhub.service.UserConfigService;
import cn.mars.marsprinterhub.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class UserConfigServiceImpl extends ServiceImpl<UserConfigMapper, UserConfig>
        implements UserConfigService {

    @Resource
    private UserService userService;

    @Resource
    private ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserConfigDTO getCurrentUserConfig() {
        return getUserConfigByUserId(userService.getCurrentUserId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserConfigDTO getUserConfigByUserId(Long userId) {
        UserConfig entity = getByUserId(userId);
        if (entity == null) {
            UserConfigDTO defaultConfig = new UserConfigDTO();
            applyDefaults(defaultConfig);
            insertConfigIgnoreDuplicate(userId, defaultConfig);
            return defaultConfig;
        }

        if (StrUtil.isBlank(entity.getConfigJson())) {
            UserConfigDTO defaultConfig = new UserConfigDTO();
            applyDefaults(defaultConfig);
            updateEntityConfig(entity, defaultConfig);
            return defaultConfig;
        }

        try {
            UserConfigDTO config = objectMapper.readValue(entity.getConfigJson(), UserConfigDTO.class);
            boolean changed = applyDefaults(config);
            if (changed) {
                updateEntityConfig(entity, config);
            }
            return config;
        } catch (JsonProcessingException e) {
            log.warn("解析用户配置失败，userId={}", userId, e);
            UserConfigDTO defaultConfig = new UserConfigDTO();
            applyDefaults(defaultConfig);
            updateEntityConfig(entity, defaultConfig);
            return defaultConfig;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCurrentUserConfig(UserConfigDTO config) {
        if (config == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "配置不能为空");
        }
        Long userId = userService.getCurrentUserId();
        String configJson;
        try {
            configJson = objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "配置序列化失败");
        }

        UserConfig entity = getByUserId(userId);
        if (entity == null) {
            entity = new UserConfig();
            entity.setUserId(userId);
            entity.setConfigJson(configJson);
            save(entity);
            return;
        }

        entity.setConfigJson(configJson);
        updateById(entity);
    }

    private UserConfig getByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<UserConfig>()
                .eq(UserConfig::getUserId, userId)
                .last("limit 1"));
    }

    private boolean applyDefaults(UserConfigDTO config) {
        boolean changed = false;
        if (config.getPowerWatt() == null) {
            config.setPowerWatt(BigDecimal.valueOf(300));
            changed = true;
        }
        if (config.getElectricityPricePerKwh() == null) {
            config.setElectricityPricePerKwh(BigDecimal.valueOf(0.5));
            changed = true;
        }
        if (config.getDepreciationPerHour() == null) {
            config.setDepreciationPerHour(BigDecimal.ZERO);
            changed = true;
        }
        if (config.getLaborCostPerHour() == null) {
            config.setLaborCostPerHour(BigDecimal.ZERO);
            changed = true;
        }
        if (config.getPageSize() == null) {
            config.setPageSize(10L);
            changed = true;
        }
        if (StrUtil.isBlank(config.getSpoolDefaultStatus())) {
            config.setSpoolDefaultStatus("1");
            changed = true;
        }
        if (StrUtil.isBlank(config.getSpoolRecordDefaultStatus())) {
            config.setSpoolRecordDefaultStatus("all");
            changed = true;
        }
        if (StrUtil.isBlank(config.getOrderDefaultStatus())) {
            config.setOrderDefaultStatus("1");
            changed = true;
        }
        if (StrUtil.isBlank(config.getDesktopTheme())) {
            config.setDesktopTheme("system");
            changed = true;
        }
        if (StrUtil.isBlank(config.getMobileTheme())) {
            config.setMobileTheme("system");
            changed = true;
        }
        if (StrUtil.isBlank(config.getFont())) {
            config.setFont("system");
            changed = true;
        }
        if (StrUtil.isBlank(config.getDesktopLayout())) {
            config.setDesktopLayout("centered");
            changed = true;
        }
        if (config.getWebBackgroundUrl() == null) {
            config.setWebBackgroundUrl("");
            changed = true;
        }
        if (config.getMobileBackgroundUrl() == null) {
            config.setMobileBackgroundUrl("");
            changed = true;
        }
        if (StrUtil.isBlank(config.getCornerStyle())) {
            config.setCornerStyle("rounded");
            changed = true;
        }
        return changed;
    }

    private void insertConfigIgnoreDuplicate(Long userId, UserConfigDTO config) {
        String configJson = toJson(config);
        UserConfig entity = new UserConfig();
        entity.setUserId(userId);
        entity.setConfigJson(configJson);
        try {
            save(entity);
        } catch (DuplicateKeyException e) {
            log.info("用户配置已存在，跳过初始化插入，userId={}", userId);
        }
    }

    private void updateEntityConfig(UserConfig entity, UserConfigDTO config) {
        entity.setConfigJson(toJson(config));
        updateById(entity);
    }

    private String toJson(UserConfigDTO config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "配置序列化失败");
        }
    }
}
