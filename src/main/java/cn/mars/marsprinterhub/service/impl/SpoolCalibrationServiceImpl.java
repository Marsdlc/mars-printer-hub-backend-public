package cn.mars.marsprinterhub.service.impl;

import cn.mars.marsprinterhub.model.vo.HomeAssistantEntityStateVO;
import cn.mars.marsprinterhub.service.HomeAssistantService;
import cn.mars.marsprinterhub.service.SpoolCalibrationService;
import cn.mars.marsprinterhub.service.SysConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class SpoolCalibrationServiceImpl implements SpoolCalibrationService {

    @Resource
    private HomeAssistantService homeAssistantService;

    @Resource
    private SysConfigService sysConfigService;

    @Override
    public HomeAssistantEntityStateVO getCalibrateWeight() {
        return homeAssistantService.getEntityState(sysConfigService.getHomeAssistantScaleEntityId());
    }
}
