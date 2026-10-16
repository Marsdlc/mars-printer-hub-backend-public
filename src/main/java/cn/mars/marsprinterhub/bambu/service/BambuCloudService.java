package cn.mars.marsprinterhub.bambu.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.bambu.config.BambuRegionConfig;
import cn.mars.marsprinterhub.bambu.client.BambuCloudClient;
import cn.mars.marsprinterhub.bambu.dto.BambuAuthDTO;
import cn.mars.marsprinterhub.bambu.dto.BambuDeviceDTO;
import cn.mars.marsprinterhub.event.core.EventPublisher;
import cn.mars.marsprinterhub.event.events.DeviceConnectionEvent;
import cn.mars.marsprinterhub.event.events.PrinterUnbindEvent;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.model.dto.bambu.BambuTaskDTO;
import cn.mars.marsprinterhub.model.dto.bambu.BambuTaskListResponseDTO;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.model.enums.BambuRegionEnum;
import cn.mars.marsprinterhub.model.enums.PrinterStatusEnum;
import cn.mars.marsprinterhub.model.enums.TaskStatusEnum;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.service.UserService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static cn.mars.marsprinterhub.exception.ErrorCode.NETWORK_ERROR;
import static cn.mars.marsprinterhub.exception.ErrorCode.SYSTEM_ERROR;

/**
 * 拓竹云服务
 * 提供拓竹云账户和设备管理的高级业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BambuCloudService {

    @Resource
    private BambuCloudClient bambuCloudClient;

    @Resource
    private EventPublisher eventPublisher;

    @Resource
    private UserService userService;

    @Resource
    private PrinterService printerService;

    @Resource
    private BambuRegionConfig bambuRegionConfig;

    /**
     * 用户登录
     *
     * @param email    邮箱
     * @param password 密码
     * @return 登录是否成功
     */
    public boolean login(String email, String password) {
        if (StrUtil.hasBlank(email, password)) {
            throw new IllegalArgumentException("邮箱和密码不能为空");
        }

        try {
            BambuAuthDTO.LoginResponse response = bambuCloudClient.login(email, password);

            if (response == null) {
                log.warn("登录响应为空");
                return false;
            }

            // 检查是否需要验证码登录
            if ("verifyCode".equals(response.getLoginType())) {
                log.info("需要验证码登录");
                throw new BusinessException(SYSTEM_ERROR, "需要验证码登录");
            }

            // 检查是否有错误信息
            if (StrUtil.isNotBlank(response.getError())) {
                log.warn("登录失败: {}", response.getError());

                // 检查是否需要验证码
                if (response.getError().contains("verification") || response.getError().contains("code")) {
                    throw new BusinessException(SYSTEM_ERROR, "需要验证码登录");
                }

                // 检查是否需要2FA
                if (response.getError().contains("2fa") || response.getError().contains("two-factor")) {
                    throw new BusinessException(SYSTEM_ERROR, "需要2FA登录");
                }

                throw new BusinessException(SYSTEM_ERROR, response.getError());
            }

            return StrUtil.isNotBlank(response.getAccessToken());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("登录过程中发生异常", e);
            throw new BusinessException(SYSTEM_ERROR);
        }
    }

    /**
     * 发送短信验证码（中国专用）
     *
     * @param phone 手机号码
     */
    public void sendSmsVerificationCode(String phone) {
        if (StrUtil.isBlank(phone)) {
            throw new IllegalArgumentException("手机号码不能为空");
        }

        try {
            bambuCloudClient.sendSmsCode(phone);
        } catch (Exception e) {
            log.error("发送短信验证码时发生异常", e);
            throw new BusinessException(NETWORK_ERROR);
        }
    }

    /**
     * 发送邮箱验证码（海外区域）
     *
     * @param email 邮箱
     */
    public void sendEmailVerificationCode(String email) {
        if (StrUtil.isBlank(email)) {
            throw new IllegalArgumentException("邮箱不能为空");
        }

        try {
            bambuCloudClient.sendEmailCode(email);
        } catch (Exception e) {
            log.error("发送邮箱验证码时发生异常", e);
            throw new BusinessException(NETWORK_ERROR);
        }
    }

    /**
     * 使用验证码登录
     *
     * @param account 账号（邮箱或手机号）
     * @param code    验证码
     * @return 登录是否成功
     */
    public boolean loginWithVerificationCode(String account, String code) {
        if (StrUtil.isBlank(account)) {
            throw new IllegalArgumentException("账号不能为空");
        }
        if (StrUtil.isBlank(code)) {
            throw new IllegalArgumentException("验证码不能为空");
        }

        try {
            BambuAuthDTO.LoginResponse response = bambuCloudClient.loginWithCode(account, code);

            if (response == null) {
                log.warn("验证码登录响应为空");
                return false;
            }

            if (StrUtil.isNotBlank(response.getError())) {
                log.warn("验证码登录失败: {}", response.getError());

                if (response.getError().contains("expired")) {
                    throw new BusinessException(SYSTEM_ERROR, "验证码已过期");
                }

                if (response.getError().contains("incorrect") || response.getError().contains("invalid")) {
                    throw new BusinessException(SYSTEM_ERROR, "验证码错误");
                }

                throw new BusinessException(SYSTEM_ERROR, response.getError());
            }

            return StrUtil.isNotBlank(response.getAccessToken());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("验证码登录过程中发生异常", e);
            throw new BusinessException(SYSTEM_ERROR);
        }
    }

    /**
     * 获取用户的所有打印机设备
     *
     * @return 设备列表
     */
    public List<BambuDeviceDTO> getUserDevices() {
        if (!bambuCloudClient.isAuthenticated()) {
            throw new BusinessException(SYSTEM_ERROR, "用户未登录");
        }

        try {
            BambuDeviceDTO.DeviceListResponse response = bambuCloudClient.getDeviceList();

            if (response == null) {
                log.warn("获取设备列表响应为空");
                return List.of();
            }

            if (StrUtil.isNotBlank(response.getError())) {
                log.warn("获取设备列表失败: {}", response.getError());
                throw new BusinessException(SYSTEM_ERROR, response.getError());
            }

            List<BambuDeviceDTO> devices = response.getDevices();
            if (CollUtil.isEmpty(devices)) {
                log.info("用户暂无绑定的打印机设备");
                return List.of();
            }

            log.info("成功获取到 {} 台打印机设备", devices.size());
            // 打印每个设备的详细信息
            for (BambuDeviceDTO device : devices) {
                log.info("Device: ID={}, Name={}, Online={}, PrintStatus={}",
                        device.getDeviceId(), device.getName(), device.getOnline(), device.getPrintStatus());
            }
            return devices;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取设备列表时发生异常", e);
            throw new BusinessException(SYSTEM_ERROR);
        }
    }

    /**
     * 根据设备ID查找设备
     *
     * @param deviceId 设备ID
     * @return 设备信息
     */
    public BambuDeviceDTO getDeviceById(String deviceId) {
        if (StrUtil.isBlank(deviceId)) {
            return null;
        }

        List<BambuDeviceDTO> devices = getUserDevices();
        return devices.stream()
                .filter(device -> deviceId.equals(device.getDeviceId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取在线设备列表
     *
     * @return 在线设备列表
     */
    public List<BambuDeviceDTO> getOnlineDevices() {
        List<BambuDeviceDTO> devices = getUserDevices();
        return devices.stream()
                .filter(device -> Boolean.TRUE.equals(device.getOnline()))
                .toList();
    }

    /**
     * 获取指定设备的访问码
     *
     * @param deviceId 设备ID
     * @return 设备访问码，如果设备不存在则返回null
     */
    public String getDeviceAccessCode(String deviceId) {
        if (StrUtil.isBlank(deviceId)) {
            throw new IllegalArgumentException("设备ID不能为空");
        }

        try {
            BambuDeviceDTO device = getDeviceById(deviceId);
            if (device != null) {
                return device.getDeviceAccessCode();
            } else {
                log.warn("设备不存在: {}", deviceId);
                return null;
            }
        } catch (Exception e) {
            log.error("获取设备访问码失败: deviceId={}", deviceId, e);
            throw new BusinessException(SYSTEM_ERROR);
        }
    }

    /**
     * 获取正在打印的设备列表
     *
     * @return 正在打印的设备列表
     */
    public List<BambuDeviceDTO> getPrintingDevices() {
        List<BambuDeviceDTO> devices = getUserDevices();
        return devices.stream()
                .filter(device -> PrinterStatusEnum.RUNNING.getValue().equalsIgnoreCase(device.getPrintStatus()) ||
                        TaskStatusEnum.PRINTING.getText().equalsIgnoreCase(device.getPrintStatus()))
                .toList();
    }

    /**
     * 获取当前登录用户名
     *
     * @return 用户名
     */
    public String getCurrentUsername() {
        return bambuCloudClient.getUsername();
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    public String getCurrentUserId() {
        return bambuCloudClient.getUserId();
    }

    /**
     * 检查是否已登录
     *
     * @return 是否已登录
     */
    public boolean isLoggedIn() {
        return bambuCloudClient.isAuthenticated();
    }

    public boolean isCurrentUserChinaRegion() {
        User currentUser = userService.getCurrentUser();
        BambuRegionEnum regionEnum = BambuRegionEnum.fromValue(currentUser.getBambuRegion());
        return bambuRegionConfig.isChinaRegion(regionEnum);
    }

    /**
     * 获取任务列表
     *
     * @param offset     偏移量
     * @param limit      每页数量
     * @param startTime  开始时间戳（秒），0表示不限制
     * @param endTime    结束时间戳（秒），0表示不限制
     * @param statusList 状态列表
     * @param query      搜索关键字
     * @return 任务列表响应
     */
    public BambuTaskListResponseDTO getTaskList(Integer offset, Integer limit, Long startTime, Long endTime, String statusList, String query) {
        try {
            User currentUser = userService.getCurrentUser();
            return bambuCloudClient.getTaskList(currentUser, null, offset, limit, startTime, endTime, statusList, query);
        } catch (Exception e) {
            log.error("获取任务列表失败", e);
            throw new BusinessException(SYSTEM_ERROR);
        }
    }

    /**
     * 获取指定设备的最新任务
     */
    public BambuTaskDTO getLatestTaskForPrinter(String deviceId, String token) {
        if (StrUtil.isBlank(deviceId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备 ID 不能为空");
        }

        try {
            User currentUser = userService.getCurrentUser();
            return bambuCloudClient.getLatestTaskForPrinter(currentUser, deviceId, token);
        } catch (Exception e) {
            log.error("获取设备最新任务失败, deviceId: {}", deviceId, e);
            throw new BusinessException(SYSTEM_ERROR);
        }
    }

    public BambuTaskDTO getLatestTaskForPrinter(User user, String deviceId, String token) {
        if (StrUtil.isBlank(deviceId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备 ID 不能为空");
        }

        try {
            return bambuCloudClient.getLatestTaskForPrinter(user, deviceId, token);
        } catch (Exception e) {
            log.error("获取设备最新任务失败, deviceId: {}", deviceId, e);
            throw new BusinessException(SYSTEM_ERROR);
        }
    }

    /**
     * 根据任务 ID 获取任务详情
     */
    public BambuTaskDTO getTaskById(Long taskId, String token) {
        if (taskId == null) {
            return null;
        }

        try {
            User currentUser = userService.getCurrentUser();
            return bambuCloudClient.getTaskById(currentUser, taskId, token);
        } catch (Exception e) {
            log.error("获取任务详情失败, taskId: {}", taskId, e);
            throw new BusinessException(SYSTEM_ERROR);
        }
    }

    public BambuTaskDTO getTaskById(User user, Long taskId, String token) {
        if (taskId == null) {
            return null;
        }

        try {
            return bambuCloudClient.getTaskById(user, taskId, token);
        } catch (Exception e) {
            log.error("获取任务详情失败, taskId: {}", taskId, e);
            throw new BusinessException(SYSTEM_ERROR);
        }
    }

    /**
     * 脱敏手机号
     */
    private String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 解绑账户
     */
    public boolean unbind() {
        return cleanupCurrentUserBambuBinding("用户解绑账户");
    }

    public boolean cleanupCurrentUserBambuBinding(String reason) {
        Long userId = userService.getCurrentUserId();
        List<Printer> printers = printerService.getListByUserId(userId);

        printers.forEach(printer -> {
            if (printer == null || StrUtil.isBlank(printer.getDeviceId())) {
                return;
            }
            DeviceConnectionEvent disconnectEvent = new DeviceConnectionEvent(
                    printer.getDeviceId(),
                    DeviceConnectionEvent.ConnectionStatus.DISCONNECTED,
                    DeviceConnectionEvent.ConnectionType.UNKNOWN,
                    reason
            );
            eventPublisher.publish(disconnectEvent);
        });

        boolean unbound = userService.unbindBambuAccount();
        printers.forEach(printer -> {
            if (printer == null || printer.getId() == null) {
                return;
            }
            eventPublisher.publish(new PrinterUnbindEvent(printer.getId()));
        });
        return unbound;
    }

    public String getCurrentBambuUserUid() {
        return bambuCloudClient.getCurrentUserBambuUid(userService.getCurrentUser().getBambuToken());
    }
}
