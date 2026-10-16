package cn.mars.marsprinterhub.service.impl;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.mars.marsprinterhub.event.core.EventPublisher;
import cn.mars.marsprinterhub.event.events.UserBambuRegionChangedEvent;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.mapper.UserMapper;
import cn.mars.marsprinterhub.model.dto.user.AdminBanUserDTO;
import cn.mars.marsprinterhub.model.dto.user.AdminResetPasswordDTO;
import cn.mars.marsprinterhub.model.dto.user.UserBambuRegionUpdateDTO;
import cn.mars.marsprinterhub.model.dto.user.UserChangePasswordDTO;
import cn.mars.marsprinterhub.model.dto.user.UserLoginDTO;
import cn.mars.marsprinterhub.model.dto.user.UserRegisterDTO;
import cn.mars.marsprinterhub.model.dto.user.UserUpdateDTO;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.model.enums.BambuRegionEnum;
import cn.mars.marsprinterhub.model.vo.UserBambuRegionVO;
import cn.mars.marsprinterhub.model.vo.UserListItemVO;
import cn.mars.marsprinterhub.model.vo.UserVO;
import cn.mars.marsprinterhub.service.SysConfigService;
import cn.mars.marsprinterhub.service.UserService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_USER_PREFIX;

/**
 * 针对表【user(用户表)】的数据库操作Service实现
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Resource
    private RedisUtils redisUtils;
    @Resource
    private EventPublisher eventPublisher;
    @Resource
    private SysConfigService sysConfigService;

    @Override
    public User register(UserRegisterDTO registerDTO) {
        ThrowUtils.throwIf(!sysConfigService.isOpenRegistrationEnabled(), ErrorCode.FORBIDDEN_ERROR, "当前未开放自由注册，如需直接使用可前往线上服务：https://hcgl.top");

        String username = registerDTO.getUsername();
        String password = registerDTO.getPassword();

        // 1. 校验用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(User::getUsername, username);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
        }

        // 2. 创建用户
        User user = new User();
        BeanUtil.copyProperties(registerDTO, user);
        user.setRole("user");
        // 密码加密
        user.setPassword(BCrypt.hashpw(password));
        // 设置默认昵称
        if (user.getNickname() == null) {
            user.setNickname(username);
        }
        user.setBambuRegion(normalizeBambuRegion(user.getBambuRegion()));
        // 保存用户
        boolean save = this.save(user);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "创建用户失败");
        return user;
    }

    @Override
    public String login(UserLoginDTO loginDTO) {
        // 1. 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = this.getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }

        // 2. 校验密码
        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        if (isUserBanned(user)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被封禁，无法登录");
        }

        return loginByUserId(user.getId());
    }

    @Override
    public void logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }

    @Override
    public User getCurrentUser() {
        // 获取当前登录用户ID
        Long userId = getLoginUserIdOrThrow();
        String key = "user:" + userId;
        if (redisUtils.hasKey(key)) {
            Object cachedUser = redisUtils.get(key);
            if (cachedUser instanceof User) {
                return (User) cachedUser;
            }
        }
        // 查询用户信息
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户不存在");
        }
        if (isUserBanned(user)) {
            StpUtil.logout(userId);
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被封禁");
        }
        user.setPassword(null);
        redisUtils.set(key, user, 24 * 3600);
        return user;
    }

    @Override
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username)));
    }

    @Override
    public String loginByUserId(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        if (isUserBanned(user)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被封禁，无法登录");
        }

        StpUtil.login(userId);
        updateLastLoginTime(userId);
        return StpUtil.getTokenValue();
    }

    @Override
    public boolean isCurrentUserVip() {
        User currentUser = getCurrentUser();
        return currentUser.isVip();
    }

    @Override
    public UserBambuRegionVO updateCurrentUserBambuRegion(UserBambuRegionUpdateDTO updateDTO) {
        ThrowUtils.throwIf(updateDTO == null, ErrorCode.PARAMS_ERROR, "拓竹区域不能为空");
        Long userId = getLoginUserIdOrThrow();
        User currentUser = this.getById(userId);
        ThrowUtils.throwIf(currentUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户不存在");
        BambuRegionEnum bambuRegion = normalizeBambuRegion(updateDTO.getBambuRegion());
        BambuRegionEnum currentRegion = getNormalizedBambuRegionEnum(currentUser.getBambuRegion());

        if (currentRegion == bambuRegion) {
            return new UserBambuRegionVO(bambuRegion);
        }

        boolean updated = this.update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getBambuRegion, bambuRegion.getValue()));
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "更新拓竹区域失败");

        eventPublisher.publish(new UserBambuRegionChangedEvent(userId, currentRegion, bambuRegion));
        String key = "user:" + userId;
        redisUtils.delete(key);
        return new UserBambuRegionVO(bambuRegion);
    }

    @Override
    public User updateUserInfo(UserUpdateDTO updateDTO) {
        Long userId = getLoginUserIdOrThrow();
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户不存在");
        }
        String email = StrUtil.isBlank(updateDTO.getEmail()) ? null : updateDTO.getEmail();
        String phone = StrUtil.isBlank(updateDTO.getPhone()) ? null : updateDTO.getPhone();
        String nickname = StrUtil.isBlank(updateDTO.getNickname()) ? null : updateDTO.getNickname();
        String avatar = StrUtil.isBlank(updateDTO.getAvatar()) ? null : updateDTO.getAvatar();

        boolean update = this.update(
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getNickname, nickname)
                        .set(User::getAvatar, avatar)
                        .set(User::getEmail, email)
                        .set(User::getPhone, phone)
        );
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "更新用户信息失败");
        user.setNickname(nickname);
        user.setAvatar(avatar);
        user.setEmail(email);
        user.setPhone(phone);
        String key = "user:" + userId;
        redisUtils.set(key, user, 24 * 3600);
        return user;
    }

    @Override
    public void changePassword(UserChangePasswordDTO changePasswordDTO) {
        String oldPassword = changePasswordDTO.getOldPassword();
        String newPassword = changePasswordDTO.getNewPassword();

        // 1. 获取当前用户
        Long userId = getLoginUserIdOrThrow();
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户不存在");
        }

        // 2. 验证旧密码
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "旧密码错误");
        }

        // 3. 更新密码
        user.setPassword(BCrypt.hashpw(newPassword));
        boolean update = this.updateById(user);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "修改密码失败");

        // 4. 清除缓存
        String key = "user:" + userId;
        redisUtils.delete(key);

        // 5. 强制退出登录，要求重新登录
        StpUtil.logout();
    }

    @Override
    public boolean unbindBambuAccount() {
        User currentUser = getCurrentUser();
        boolean updated = this.update(
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, currentUser.getId())
                        .set(User::getBambuUserId, null)
                        .set(User::getBambuUsername, null)
                        .set(User::getBambuToken, null)
                        .set(User::getBambuTokenExpireTime, null)
        );
        if (updated) {
            redisUtils.delete("user:" + currentUser.getId());
        }
        return updated;
    }

    @Override
    public UserVO convertToUserVO(User user) {
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        userVO.setBambuRegion(getNormalizedBambuRegionEnum(user.getBambuRegion()));
        userVO.setIsIntranet(isIntranet);
        userVO.setIsVip(user.isVip());
        userVO.setIsBanned(isUserBanned(user));
        return userVO;
    }

    @Override
    public User getVipUserByDeviceId(String deviceId) {
        Long userId = redisUtils.getLong(DEVICE_USER_PREFIX + deviceId);
        if (userId == null) return null;
        User user = this.getById(userId);
        if (user == null || !user.isVip()) {
            return null;
        }
        return user;
    }

    @Override
    public boolean isSystemInitialized() {
        // 非内网模式（线上部署）始终视为已初始化
        if (!isIntranet) {
            return true;
        }
        return this.count() > 0;
    }

    @Override
    public String setupAdmin(UserRegisterDTO registerDTO) {
        // 仅内网模式（独立部署）允许初始化管理员
        if (!isIntranet) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "当前部署模式不支持此操作");
        }
        // 1. 校验：是否已有用户
        if (this.count() > 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "系统已初始化，无法重复创建管理员");
        }
        // 2. 创建管理员用户
        User user = new User();
        BeanUtil.copyProperties(registerDTO, user);
        user.setRole("admin");
        user.setPassword(BCrypt.hashpw(registerDTO.getPassword()));
        if (user.getNickname() == null) {
            user.setNickname(registerDTO.getUsername());
        }
        user.setBambuRegion(normalizeBambuRegion(user.getBambuRegion()));
        boolean save = this.save(user);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "创建管理员失败");
        // 3. 自动登录
        return loginByUserId(user.getId());
    }

    @Override
    public List<UserListItemVO> listUsers() {
        List<User> users = this.list();
        return users.stream().map(user -> {
            UserListItemVO vo = new UserListItemVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setCreateTime(user.getCreateTime());
            vo.setLastLoginTime(user.getLastLoginTime());
            vo.setIsBanned(isUserBanned(user));
            vo.setBanTime(user.getBanTime());
            vo.setBanReason(user.getBanReason());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void adminResetPassword(AdminResetPasswordDTO dto) {
        // 1. 查找目标用户
        User user = this.getById(dto.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }

        // 2. 更新密码
        user.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        boolean update = this.updateById(user);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "重置密码失败");

        // 3. 清除目标用户缓存
        String key = "user:" + dto.getUserId();
        redisUtils.delete(key);

        // 4. 强制目标用户退出登录
        StpUtil.kickout(dto.getUserId());
    }

    @Override
    public void adminBanUser(AdminBanUserDTO dto) {
        User user = this.getById(dto.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        if ("admin".equals(user.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "管理员账号不允许封禁");
        }
        Long currentUserId = getLoginUserIdOrThrow();
        if (currentUserId.equals(dto.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "不能封禁自己");
        }

        boolean updated = this.update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, dto.getUserId())
                .set(User::getIsBanned, 1)
                .set(User::getBanReason, dto.getReason())
                .set(User::getBanTime, new Date()));
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "封禁用户失败");

        String key = "user:" + dto.getUserId();
        redisUtils.delete(key);
        StpUtil.kickout(dto.getUserId());
    }

    @Override
    public void adminUnbanUser(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }

        boolean updated = this.update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getIsBanned, 0)
                .set(User::getBanReason, null)
                .set(User::getBanTime, null));
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "解封用户失败");

        String key = "user:" + userId;
        redisUtils.delete(key);
    }

    private Long getLoginUserIdOrThrow() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (NotLoginException e) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
    }

    private void updateLastLoginTime(Long userId) {
        Date now = new Date();
        try {
            this.update(new LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .set(User::getLastLoginTime, now));
            String key = "user:" + userId;
            redisUtils.delete(key);
        } catch (Exception e) {
            log.warn("更新用户最后登录时间失败: userId={}", userId, e);
        }
    }

    private boolean isUserBanned(User user) {
        return user != null && Integer.valueOf(1).equals(user.getIsBanned());
    }

    private String normalizeBambuRegion(String bambuRegion) {
        return getNormalizedBambuRegionEnum(bambuRegion).getValue();
    }

    private BambuRegionEnum normalizeBambuRegion(BambuRegionEnum bambuRegion) {
        return bambuRegion == null ? BambuRegionEnum.CHINA : bambuRegion;
    }

    private BambuRegionEnum getNormalizedBambuRegionEnum(String bambuRegion) {
        BambuRegionEnum regionEnum = BambuRegionEnum.fromValue(bambuRegion);
        return regionEnum == null ? BambuRegionEnum.CHINA : regionEnum;
    }
}
