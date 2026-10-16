package cn.mars.marsprinterhub.service.impl;

import cn.mars.marsprinterhub.homeassistant.client.HomeAssistantClient;
import cn.mars.marsprinterhub.homeassistant.model.HomeAssistantEntityState;
import cn.mars.marsprinterhub.model.vo.HomeAssistantEntityStateVO;
import cn.mars.marsprinterhub.service.HomeAssistantService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class HomeAssistantServiceImpl implements HomeAssistantService {

    @Resource
    private HomeAssistantClient homeAssistantClient;

    @Override
    public HomeAssistantEntityState getRawEntityState(String entityId) {
        return homeAssistantClient.getEntityState(entityId);
    }

    @Override
    public HomeAssistantEntityStateVO getEntityState(String entityId) {
        HomeAssistantEntityState raw = getRawEntityState(entityId);
        Object unit = raw.getAttributes() == null ? null : raw.getAttributes().get("unit_of_measurement");
        return new HomeAssistantEntityStateVO(
                raw.getEntityId(),
                normalizeState(raw.getState()),
                unit == null ? null : unit.toString()
        );
    }

    private String normalizeState(String state) {
        if (state == null) {
            return null;
        }
        try {
            return new BigDecimal(state).setScale(2, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException ignored) {
            return state;
        }
    }
}
