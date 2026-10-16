package cn.mars.marsprinterhub.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.mars.marsprinterhub.model.entity.User;
import cn.mars.marsprinterhub.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义权限验证接口扩展
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private UserService userService;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 本项目暂不涉及权限功能，只做角色功能
        return new ArrayList<>();
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> list = new ArrayList<>();
        try {
            // 根据用户ID获取用户信息
            User user = userService.getById(Long.valueOf(loginId.toString()));
            if (user != null && user.getRole() != null) {
                list.add(user.getRole());
            }
        } catch (Exception e) {
            // 如果获取失败，返回空列表
        }
        return list;
    }
}