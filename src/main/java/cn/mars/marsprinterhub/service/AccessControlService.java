package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 统一处理系统级接口的访问权限校验。
 */
@Service
public class AccessControlService {

    @Value("${bambu.intranet}")
    private boolean isIntranet;

    @Resource
    private UserService userService;

    public void checkAdmin() {
        ThrowUtils.throwIf(!"admin".equals(userService.getCurrentUser().getRole()),
                ErrorCode.NO_AUTH_ERROR, "仅管理员可操作");
    }

    public void checkAdminIntranet() {
        ThrowUtils.throwIf(!isIntranet, ErrorCode.FORBIDDEN_ERROR, "该功能仅内网部署可用");
        checkAdmin();
    }
}
