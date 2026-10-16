package cn.mars.marsprinterhub.utils;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class FilamentUtils {

    private Map<String, FilamentInfo> filamentMap;
    private final ObjectMapper objectMapper;

    public FilamentUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        loadFilamentData();
    }

    private void loadFilamentData() {
        try {
            String json = ResourceUtil.readUtf8Str("static/filaments.json");
            filamentMap = objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("加载耗材数据失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "耗材数据预设加载失败: " + e.getMessage());
        }
    }

    /**
     * 根据对象名称获取完整耗材信息
     *
     * @param key 对象名称，如 "GFA00"
     * @return FilamentInfo 对象，如果不存在返回 null
     */
    public FilamentInfo getFilamentInfo(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        return filamentMap.get(key);
    }

    @Data
    public static class FilamentInfo {
        private String name;
        private String filament_type;
        private int nozzle_temperature_range_high;
        private int nozzle_temperature_range_low;
    }
}