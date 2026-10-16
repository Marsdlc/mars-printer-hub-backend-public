package cn.mars.marsprinterhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.mars.marsprinterhub.constant.RedisConstants;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.mapper.UserApiKeyMapper;
import cn.mars.marsprinterhub.model.dto.user.UserApiKeyCreateDTO;
import cn.mars.marsprinterhub.model.dto.user.UserApiKeyUpdateDTO;
import cn.mars.marsprinterhub.model.entity.UserApiKey;
import cn.mars.marsprinterhub.model.vo.UserApiKeyCreateVO;
import cn.mars.marsprinterhub.model.vo.UserApiKeyVO;
import cn.mars.marsprinterhub.service.UserApiKeyService;
import cn.mars.marsprinterhub.service.UserService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserApiKeyServiceImpl extends ServiceImpl<UserApiKeyMapper, UserApiKey>
        implements UserApiKeyService {

    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;
    private static final String API_KEY_PREFIX = "mph_";
    private static final int DISPLAY_PREFIX_LENGTH = 12;
    private static final long API_KEY_AUTH_CACHE_TTL_SECONDS = 6 * 3600;

    @Resource
    private UserService userService;

    @Resource
    private RedisUtils redisUtils;

    @Override
    public UserApiKeyVO getCurrentUserApiKey() {
        UserApiKey entity = getCurrentManageableApiKey(userService.getCurrentUserId());
        return entity == null ? null : toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserApiKeyCreateVO createCurrentUserApiKey(UserApiKeyCreateDTO createDTO) {
        if (createDTO == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long userId = userService.getCurrentUserId();
        UserApiKey existing = getCurrentManageableApiKey(userId);
        if (existing != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前用户已存在可管理的 API Key");
        }

        String apiKey = generateApiKey();
        UserApiKey entity = new UserApiKey();
        entity.setUserId(userId);
        entity.setName(StrUtil.trim(createDTO.getName()));
        entity.setKeyPrefix(buildDisplayPrefix(apiKey));
        entity.setSecretHash(SecureUtil.sha256(apiKey));
        entity.setStatus(STATUS_ENABLED);
        entity.setCreateUserId(userId);

        boolean saved = save(entity);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "创建 API Key 失败");
        cacheApiKey(entity);

        UserApiKeyCreateVO vo = BeanUtil.copyProperties(entity, UserApiKeyCreateVO.class);
        vo.setApiKey(apiKey);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentUserApiKey(UserApiKeyUpdateDTO updateDTO) {
        if (updateDTO == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long userId = userService.getCurrentUserId();
        UserApiKey entity = getCurrentManageableApiKey(userId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "当前用户不存在可管理的 API Key");
        }

        boolean updated = update(new LambdaUpdateWrapper<UserApiKey>()
                .eq(UserApiKey::getId, entity.getId())
                .set(UserApiKey::getName, StrUtil.trim(updateDTO.getName())));
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "更新 API Key 失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableCurrentUserApiKey() {
        updateCurrentUserApiKeyStatus(STATUS_ENABLED, "启用 API Key 失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableCurrentUserApiKey() {
        updateCurrentUserApiKeyStatus(STATUS_DISABLED, "禁用 API Key 失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserApiKeyCreateVO resetCurrentUserApiKey() {
        Long userId = userService.getCurrentUserId();
        UserApiKey entity = getCurrentManageableApiKey(userId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "当前用户不存在可重置的 API Key");
        }
        evictApiKeyCache(entity.getSecretHash());
        String apiKey = generateApiKey();
        String secretHash = SecureUtil.sha256(apiKey);
        String keyPrefix = buildDisplayPrefix(apiKey);
        boolean updated = update(new LambdaUpdateWrapper<UserApiKey>()
                .eq(UserApiKey::getId, entity.getId())
                .set(UserApiKey::getSecretHash, secretHash)
                .set(UserApiKey::getKeyPrefix, keyPrefix)
                .set(UserApiKey::getStatus, STATUS_ENABLED));
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "重置 API Key 失败");

        entity.setSecretHash(null);
        entity.setKeyPrefix(keyPrefix);
        entity.setStatus(STATUS_ENABLED);
        UserApiKey cacheEntity = getCurrentManageableApiKey(userId);
        cacheApiKey(cacheEntity);
        UserApiKeyCreateVO vo = BeanUtil.copyProperties(entity, UserApiKeyCreateVO.class);
        vo.setApiKey(apiKey);
        return vo;
    }

    @Override
    public UserApiKey validateApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "缺少 API Key");
        }
        String secretHash = SecureUtil.sha256(apiKey);
        UserApiKey entity = getCachedApiKey(secretHash);
        if (entity == null) {
            entity = getOne(new LambdaQueryWrapper<UserApiKey>()
                    .eq(UserApiKey::getSecretHash, secretHash)
                    .last("limit 1"));
            if (entity != null) {
                cacheApiKey(entity);
            }
        }
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "API Key 无效");
        }
        if (!STATUS_ENABLEDEquals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "API Key 不可用");
        }
        return entity;
    }

    @Override
    public void recordApiKeyUsage(Long apiKeyId, String ipAddress) {
        if (apiKeyId == null) {
            return;
        }
        update(new LambdaUpdateWrapper<UserApiKey>()
                .eq(UserApiKey::getId, apiKeyId)
                .set(UserApiKey::getLastUsedTime, new java.util.Date())
                .set(StrUtil.isNotBlank(ipAddress), UserApiKey::getLastUsedIp, StrUtil.trim(ipAddress)));
    }

    private void updateCurrentUserApiKeyStatus(int status, String errorMessage) {
        Long userId = userService.getCurrentUserId();
        UserApiKey entity = getCurrentManageableApiKey(userId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "当前用户不存在可管理的 API Key");
        }
        evictApiKeyCache(entity.getSecretHash());
        boolean updated = update(new LambdaUpdateWrapper<UserApiKey>()
                .eq(UserApiKey::getId, entity.getId())
                .set(UserApiKey::getStatus, status));
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, errorMessage);
    }

    private UserApiKey getCurrentManageableApiKey(Long userId) {
        return getOne(new LambdaQueryWrapper<UserApiKey>()
                .eq(UserApiKey::getUserId, userId)
                .last("limit 1"));
    }

    private UserApiKeyVO toVO(UserApiKey entity) {
        return BeanUtil.copyProperties(entity, UserApiKeyVO.class);
    }

    private String generateApiKey() {
        return API_KEY_PREFIX + IdUtil.fastSimpleUUID() + IdUtil.fastSimpleUUID();
    }

    private String buildDisplayPrefix(String apiKey) {
        return StrUtil.sub(apiKey, 0, Math.min(apiKey.length(), DISPLAY_PREFIX_LENGTH));
    }

    private boolean STATUS_ENABLEDEquals(Integer status) {
        return status != null && status == STATUS_ENABLED;
    }

    private UserApiKey getCachedApiKey(String secretHash) {
        Object cached = redisUtils.get(buildApiKeyAuthCacheKey(secretHash));
        if (cached instanceof UserApiKey userApiKey) {
            return userApiKey;
        }
        return null;
    }

    private void cacheApiKey(UserApiKey entity) {
        if (entity == null || StrUtil.isBlank(entity.getSecretHash())) {
            return;
        }
        redisUtils.set(buildApiKeyAuthCacheKey(entity.getSecretHash()), entity, API_KEY_AUTH_CACHE_TTL_SECONDS);
    }

    private void evictApiKeyCache(String secretHash) {
        if (StrUtil.isBlank(secretHash)) {
            return;
        }
        redisUtils.delete(buildApiKeyAuthCacheKey(secretHash));
    }

    private String buildApiKeyAuthCacheKey(String secretHash) {
        return RedisConstants.USER_API_KEY_AUTH_PREFIX + secretHash;
    }
}
