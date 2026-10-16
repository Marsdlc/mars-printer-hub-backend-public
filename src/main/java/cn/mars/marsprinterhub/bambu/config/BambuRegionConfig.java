package cn.mars.marsprinterhub.bambu.config;

import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.model.enums.BambuRegionEnum;
import lombok.Data;
import org.springframework.context.annotation.Configuration;

/**
 * 拓竹区域配置
 */
@Data
@Configuration
public class BambuRegionConfig {

    public static final String REGION_CHINA = "China";
    public static final String REGION_OTHER = "Other";
    public static final String CHINA_CLOUD_BASE_URL = "https://api.bambulab.cn";
    public static final String OTHER_CLOUD_BASE_URL = "https://api.bambulab.com";
    public static final String CHINA_MQTT_HOST = "cn.mqtt.bambulab.com";
    public static final String OTHER_MQTT_HOST = "us.mqtt.bambulab.com";

    public boolean isChinaRegion(String region) {
        return StrUtil.equalsIgnoreCase(REGION_CHINA, region);
    }

    public boolean isChinaRegion(BambuRegionEnum region) {
        return region == null || region == BambuRegionEnum.CHINA;
    }

    public String getCloudBaseUrl(String region) {
        return isChinaRegion(region) ? CHINA_CLOUD_BASE_URL : OTHER_CLOUD_BASE_URL;
    }

    public String getCloudBaseUrl(BambuRegionEnum region) {
        return isChinaRegion(region) ? CHINA_CLOUD_BASE_URL : OTHER_CLOUD_BASE_URL;
    }

    public String getCloudMqttHost(String region) {
        return isChinaRegion(region) ? CHINA_MQTT_HOST : OTHER_MQTT_HOST;
    }

    public String getCloudMqttHost(BambuRegionEnum region) {
        return isChinaRegion(region) ? CHINA_MQTT_HOST : OTHER_MQTT_HOST;
    }
}
