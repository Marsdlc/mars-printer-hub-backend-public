package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.vo.HomeAssistantEntityStateVO;

public interface SpoolCalibrationService {

    /**
     * 获取默认电子秤当前状态
     */
    HomeAssistantEntityStateVO getCalibrateWeight();
}
