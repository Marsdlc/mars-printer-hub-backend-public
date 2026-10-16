package cn.mars.marsprinterhub.service.impl;

import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.mapper.VipActivationCodeMapper;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.model.entity.VipActivationCode;
import cn.mars.marsprinterhub.service.UserService;
import cn.mars.marsprinterhub.service.VipActivationCodeService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

/**
 * VIP 激活码服务实现
 */
@Service
public class VipActivationCodeServiceImpl extends ServiceImpl<VipActivationCodeMapper, VipActivationCode>
        implements VipActivationCodeService {

    @Resource
    private UserService userService;

    @Resource
    private RedisUtils redisUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int activateVip(String code) {
        boolean lifetimeActivation = LIFETIME_ACTIVATION_CODE.equals(code);
        Long userId = userService.getCurrentUserId();
        Date now = new Date();
        int durationDays = 0;

        // 内置激活码可重复使用，无需查询或消耗数据库中的激活码。
        if (!lifetimeActivation) {
            VipActivationCode activationCode = this.getOne(
                    new LambdaQueryWrapper<VipActivationCode>()
                            .eq(VipActivationCode::getCode, code)
                            .eq(VipActivationCode::getIsUsed, false)
            );
            if (activationCode == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "激活码无效或已被使用");
            }

            // 使用条件更新原子抢占激活码，避免并发重复消费
            boolean claimed = this.update(
                    new LambdaUpdateWrapper<VipActivationCode>()
                            .eq(VipActivationCode::getId, activationCode.getId())
                            .eq(VipActivationCode::getIsUsed, false)
                            .set(VipActivationCode::getIsUsed, true)
                            .set(VipActivationCode::getUsedBy, userId)
                            .set(VipActivationCode::getUsedTime, now)
            );
            if (!claimed) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "激活码无效或已被使用");
            }
            durationDays = activationCode.getDurationDays();
        }

        // 锁定用户行，避免并发激活不同激活码时覆盖 VIP 到期时间
        User currentUser = userService.getOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getId, userId)
                        .last("FOR UPDATE")
        );
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // MySQL DATETIME 最大年份为 9999，重复激活或续期均不超过此上限。
        Date maxVipExpireTime = Date.from(LocalDateTime.of(9999, 12, 31, 23, 59, 59)
                .atZone(ZoneId.systemDefault()).toInstant());
        Date newVipExpireTime;
        if (lifetimeActivation) {
            newVipExpireTime = maxVipExpireTime;
            durationDays = Math.toIntExact(ChronoUnit.DAYS.between(now.toInstant(), maxVipExpireTime.toInstant()));
        } else {
            Date currentVipExpireTime = currentUser.getVipExpireTime();
            Date baseTime = (currentVipExpireTime != null && currentVipExpireTime.after(now))
                    ? currentVipExpireTime : now;

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(baseTime);
            calendar.add(Calendar.DAY_OF_MONTH, durationDays);
            newVipExpireTime = calendar.getTime().after(maxVipExpireTime) ? maxVipExpireTime : calendar.getTime();
        }

        // 更新用户 VIP 过期时间
        boolean userUpdated = userService.update(
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getVipExpireTime, newVipExpireTime)
        );
        if (!userUpdated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新 VIP 到期时间失败");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisUtils.delete("user:" + userId);
            }
        });

        return durationDays;
    }

    @Override
    public String generateCode(int days) {
        // 生成 8 位随机激活码
        String code = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        VipActivationCode activationCode = new VipActivationCode();
        activationCode.setCode(code);
        activationCode.setDurationDays(days);
        activationCode.setIsUsed(false);
        this.save(activationCode);

        return code;
    }
}
