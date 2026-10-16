package cn.mars.marsprinterhub.model.dto.bambu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * AMS详细映射DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmsDetailMappingDTO {
    /**
     * AMS槽位索引
     */
    private Integer ams;
    
    /**
     * 源颜色
     */
    private String sourceColor;
    
    /**
     * 目标颜色
     */
    private String targetColor;
    
    /**
     * 耗材ID
     */
    private String filamentId;
    
    /**
     * 耗材类型
     */
    private String filamentType;
    
    /**
     * 目标耗材类型
     */
    private String targetFilamentType;
    
    /**
     * 重量(克)
     */
    private Double weight;
}