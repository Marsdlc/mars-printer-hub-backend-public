package cn.mars.marsprinterhub.controller;

import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.model.dto.user.AdminResetPasswordDTO;
import cn.mars.marsprinterhub.model.dto.user.AdminBanUserDTO;
import cn.mars.marsprinterhub.model.dto.user.UserBambuRegionUpdateDTO;
import cn.mars.marsprinterhub.model.dto.user.UserChangePasswordDTO;
import cn.mars.marsprinterhub.model.dto.user.UserLoginDTO;
import cn.mars.marsprinterhub.model.dto.user.UserPasskeyLoginFinishDTO;
import cn.mars.marsprinterhub.model.dto.user.UserPasskeyLoginStartDTO;
import cn.mars.marsprinterhub.model.dto.user.UserPasskeyRegisterFinishDTO;
import cn.mars.marsprinterhub.model.dto.user.UserPasskeyRegisterStartDTO;
import cn.mars.marsprinterhub.model.dto.user.UserRegisterDTO;
import cn.mars.marsprinterhub.model.dto.user.UserUpdateDTO;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.model.vo.PasskeyAssertionStartVO;
import cn.mars.marsprinterhub.model.vo.PasskeyRegisterResultVO;
import cn.mars.marsprinterhub.model.vo.PasskeyRegistrationStartVO;
import cn.mars.marsprinterhub.model.vo.UserBambuRegionVO;
import cn.mars.marsprinterhub.model.vo.UserPasskeyCredentialVO;
import cn.mars.marsprinterhub.model.vo.UserListItemVO;
import cn.mars.marsprinterhub.model.vo.UserVO;
import cn.mars.marsprinterhub.service.AccessControlService;
import cn.mars.marsprinterhub.service.PasskeyService;
import cn.mars.marsprinterhub.service.UserService;
import cn.mars.marsprinterhub.service.VipActivationCodeService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.mars.marsprinterhub.common.ResultUtils.success;
import static cn.mars.marsprinterhub.constant.RedisConstants.VIP_ACTIVATE_LIMIT_PREFIX;

@RestController
@RequestMapping("/user")
@Tag(name = "User", description = "用户相关接口")
public class UserController {

    private static final int MAX_ACTIVATE_ATTEMPTS = 5;
    private static final long ACTIVATE_LIMIT_SECONDS = 600; // 10 分钟
    @Resource
    private UserService userService;
    @Resource
    private PasskeyService passkeyService;
    @Resource
    private VipActivationCodeService vipActivationCodeService;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private AccessControlService accessControlService;

    @Operation(summary = "检查系统是否已初始化")
    @GetMapping("/system-initialized")
    public BaseResponse<Boolean> checkSystemInitialized() {
        boolean initialized = userService.isSystemInitialized();
        return success(initialized);
    }

    @Operation(summary = "初始化管理员账号")
    @PostMapping("/setup-admin")
    public BaseResponse<String> setupAdmin(@RequestBody @Valid UserRegisterDTO registerDTO) {
        String token = userService.setupAdmin(registerDTO);
        return success(token);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public BaseResponse<UserVO> register(@RequestBody @Valid UserRegisterDTO registerDTO) {
        User user = userService.register(registerDTO);
        UserVO userVO = userService.convertToUserVO(user);
        return success(userVO);
    }

    @Operation(
            summary = "用户登录",
            operationId = "userLogin"
    )
    @PostMapping("/login")
    public BaseResponse<String> login(@RequestBody @Valid UserLoginDTO loginDTO) {
        String token = userService.login(loginDTO);
        return success(token);
    }

    @Operation(summary = "开始 Passkey 登录")
    @PostMapping("/passkey/login/options")
    public BaseResponse<PasskeyAssertionStartVO> startPasskeyLogin(
            @RequestBody(required = false) @Valid UserPasskeyLoginStartDTO dto) {
        return success(passkeyService.startAssertion(dto));
    }

    @Operation(summary = "完成 Passkey 登录")
    @PostMapping("/passkey/login/verify")
    public BaseResponse<String> finishPasskeyLogin(
            @RequestParam String requestId,
            @RequestBody @Valid UserPasskeyLoginFinishDTO dto) {
        return success(passkeyService.finishAssertion(requestId, dto));
    }

    @Operation(summary = "用户退出")
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout() {
        userService.logout();
        return success(true);
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/current")
    public BaseResponse<UserVO> getCurrentUser() {
        User user = userService.getCurrentUser();
        UserVO userVO = userService.convertToUserVO(user);
        return success(userVO);
    }

    @Operation(summary = "更新当前用户拓竹区域")
    @PutMapping("/bambu-region")
    public BaseResponse<UserBambuRegionVO> updateCurrentUserBambuRegion(
            @RequestBody @Valid UserBambuRegionUpdateDTO updateDTO) {
        return success(userService.updateCurrentUserBambuRegion(updateDTO));
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/update")
    public BaseResponse<UserVO> updateUserInfo(@RequestBody @Valid UserUpdateDTO updateDTO) {
        User user = userService.updateUserInfo(updateDTO);
        UserVO userVO = userService.convertToUserVO(user);
        return success(userVO);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/change-password")
    public BaseResponse<Boolean> changePassword(@RequestBody @Valid UserChangePasswordDTO changePasswordDTO) {
        userService.changePassword(changePasswordDTO);
        return success(true);
    }

    @Operation(summary = "开始注册 Passkey")
    @PostMapping("/passkey/register/options")
    public BaseResponse<PasskeyRegistrationStartVO> startPasskeyRegistration(
            @RequestBody(required = false) @Valid UserPasskeyRegisterStartDTO dto) {
        return success(passkeyService.startRegistration(dto));
    }

    @Operation(summary = "完成注册 Passkey")
    @PostMapping("/passkey/register/verify")
    public BaseResponse<PasskeyRegisterResultVO> finishPasskeyRegistration(
            @RequestParam String requestId,
            @RequestBody @Valid UserPasskeyRegisterFinishDTO dto) {
        return success(passkeyService.finishRegistration(requestId, dto));
    }

    @Operation(summary = "获取当前用户 Passkey 列表")
    @GetMapping("/passkey/list")
    public BaseResponse<List<UserPasskeyCredentialVO>> listPasskeys() {
        return success(passkeyService.listCurrentUserCredentials());
    }

    @Operation(summary = "删除当前用户 Passkey")
    @DeleteMapping("/passkey/{credentialId}")
    public BaseResponse<Boolean> deletePasskey(@PathVariable Long credentialId) {
        passkeyService.deleteCredential(credentialId);
        return success(true);
    }

    @Operation(summary = "激活 VIP")
    @PostMapping("/activate-vip")
    public BaseResponse<String> activateVip(@RequestParam String code) {
        // 限流检查
        Long userId = userService.getCurrentUserId();
        String limitKey = VIP_ACTIVATE_LIMIT_PREFIX + userId;
        Long attempts = redisUtils.increment(limitKey, 1);
        if (attempts == 1) {
            redisUtils.expire(limitKey, ACTIVATE_LIMIT_SECONDS);
        }
        if (attempts > MAX_ACTIVATE_ATTEMPTS) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "操作过于频繁，请 10 分钟后再试");
        }

        // 激活 VIP
        int days = vipActivationCodeService.activateVip(code);
        if (VipActivationCodeService.LIFETIME_ACTIVATION_CODE.equals(code)) {
            return success("激活成功，VIP 有效期至 9999-12-31 23:59:59");
        }
        return success("激活成功，VIP 有效期增加 " + days + " 天");
    }

    @Operation(summary = "获取用户列表（管理员）")
    @GetMapping("/admin/list")
    public BaseResponse<List<UserListItemVO>> listUsers() {
        accessControlService.checkAdminIntranet();
        List<UserListItemVO> list = userService.listUsers();
        return success(list);
    }

    @Operation(summary = "重置用户密码（管理员）")
    @PutMapping("/admin/reset-password")
    public BaseResponse<Boolean> adminResetPassword(@RequestBody @Valid AdminResetPasswordDTO dto) {
        accessControlService.checkAdminIntranet();
        userService.adminResetPassword(dto);
        return success(true);
    }

    @Operation(summary = "封禁用户（管理员）")
    @PutMapping("/admin/ban")
    public BaseResponse<Boolean> adminBanUser(@RequestBody @Valid AdminBanUserDTO dto) {
        accessControlService.checkAdminIntranet();
        userService.adminBanUser(dto);
        return success(true);
    }

    @Operation(summary = "解封用户（管理员）")
    @PutMapping("/admin/unban")
    public BaseResponse<Boolean> adminUnbanUser(@RequestParam Long userId) {
        accessControlService.checkAdminIntranet();
        userService.adminUnbanUser(userId);
        return success(true);
    }

}
