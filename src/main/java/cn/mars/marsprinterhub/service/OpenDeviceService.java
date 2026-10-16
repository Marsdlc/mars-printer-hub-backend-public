package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.vo.OpenDeviceInfoVO;

public interface OpenDeviceService {

    /**
     * 获取开放接口设备信息
     *
     * @param userId   Open API 调用所属用户
     * @param deviceId 设备唯一标识
     * @return 设备信息
     */
    OpenDeviceInfoVO getDeviceInfo(Long userId, String deviceId);
}
