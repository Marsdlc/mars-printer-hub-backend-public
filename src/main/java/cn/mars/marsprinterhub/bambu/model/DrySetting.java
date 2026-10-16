package cn.mars.marsprinterhub.bambu.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * AMS烘干设置
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DrySetting {

    @JsonAlias("dry_duration")
    private Integer dryDuration; // 烘干时长

    @JsonAlias("dry_filament")
    private String dryFilament; // 烘干耗材类型

    @JsonAlias("dry_temperature")
    private Integer dryTemperature; // 烘干温度
}
