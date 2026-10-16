package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.dto.user.AdminResetPasswordDTO;
import cn.mars.marsprinterhub.model.dto.user.AdminBanUserDTO;
import cn.mars.marsprinterhub.model.dto.user.UserBambuRegionUpdateDTO;
import cn.mars.marsprinterhub.model.dto.user.UserChangePasswordDTO;
import cn.mars.marsprinterhub.model.dto.user.UserLoginDTO;
import cn.mars.marsprinterhub.model.dto.user.UserRegisterDTO;
import cn.mars.marsprinterhub.model.dto.user.UserUpdateDTO;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.model.vo.UserBambuRegionVO;
import cn.mars.marsprinterhub.model.vo.UserListItemVO;
import cn.mars.marsprinterhub.model.vo.UserVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Optional;

/**
 * 针对表【user(用户表)】的数据库操作Service
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param registerDTO 注册信息
     * @return 用户信息
     */
    User register(UserRegisterDTO registerDTO);

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return token
     */
    String login(UserLoginDTO loginDTO);

    /**
     * 用户退出
     */
    void logout();

    /**
     * 获取当前登录用户
     *
     * @return 用户信息
     */
    User getCurrentUser();

    /**
     * 获取当前登录用户id
     *
     * @return 用户id
     */
    Long getCurrentUserId();

    Optional<User> findByUsername(String username);

    String loginByUserId(Long userId);

    boolean isCurrentUserVip();

    /**
     * 更新当前用户拓竹区域
     *
     * @param updateDTO 区域更新信息
     * @return 当前用户拓竹区域
     */
    UserBambuRegionVO updateCurrentUserBambuRegion(UserBambuRegionUpdateDTO updateDTO);

    /**
     * 更新用户信息
     *
     * @param updateDTO 更新信息
     * @return 用户信息
     */
    User updateUserInfo(UserUpdateDTO updateDTO);

    /**
     * 修改密码
     *
     * @param changePasswordDTO 修改密码信息
     */
    void changePassword(UserChangePasswordDTO changePasswordDTO);

    /**
     * 解绑拓竹云账户
     *
     * @return 是否成功
     */
    boolean unbindBambuAccount();

    /**
     * 将用户信息转换为用户视图对象
     *
     * @param user 用户信息
     * @return 用户视图对象
     */
    UserVO convertToUserVO(User user);

    /**
     * 根据设备id获取 vip 用户
     *
     * @param deviceId 设备 id
     * @return vip 用户
     */
    User getVipUserByDeviceId(String deviceId);

    /**
     * 检查系统是否已初始化（是否存在至少一个用户）
     *
     * @return true 表示已初始化
     */
    boolean isSystemInitialized();

    /**
     * 初始化管理员账号（仅在无用户时可调用）
     *
     * @param registerDTO 注册信息
     * @return token
     */
    String setupAdmin(UserRegisterDTO registerDTO);

    /**
     * 获取用户列表（管理员用）
     *
     * @return 用户列表
     */
    List<UserListItemVO> listUsers();

    /**
     * 管理员重置用户密码
     *
     * @param dto 重置密码信息
     */
    void adminResetPassword(AdminResetPasswordDTO dto);

    /**
     * 管理员封禁用户
     *
     * @param dto 封禁信息
     */
    void adminBanUser(AdminBanUserDTO dto);

    /**
     * 管理员解封用户
     *
     * @param userId 用户ID
     */
    void adminUnbanUser(Long userId);
}
