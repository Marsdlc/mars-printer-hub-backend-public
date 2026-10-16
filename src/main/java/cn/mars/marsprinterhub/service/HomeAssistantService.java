package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.homeassistant.model.HomeAssistantEntityState;
import cn.mars.marsprinterhub.model.vo.HomeAssistantEntityStateVO;

public interface HomeAssistantService {

    HomeAssistantEntityState getRawEntityState(String entityId);

    HomeAssistantEntityStateVO getEntityState(String entityId);
}
