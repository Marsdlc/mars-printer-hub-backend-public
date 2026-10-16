package cn.mars.marsprinterhub.model.dto.bambu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * 拓竹云任务DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BambuTaskDTO {
    /**
     * 任务ID
     */
    private Long id;
    
    /**
     * 设计ID
     */
    private Long designId;
    
    /**
     * 设计标题
     */
    private String designTitle;
    
    /**
     * 实例ID
     */
    private Long instanceId;
    
    /**
     * 模型ID
     */
    private String modelId;
    
    /**
     * 任务标题
     */
    private String title;
    
    /**
     * 任务图片URL
     */
    private String cover;
    
    /**
     * 任务状态
     */
    private Integer status;
    
    /**
     * 反馈状态
     */
    private Integer feedbackStatus;
    
    /**
     * 开始时间
     */
    private String startTime;
    
    /**
     * 结束时间
     */
    private String endTime;
    
    /**
     * 重量(克)
     */
    private Double weight;
    
    /**
     * 长度(毫米)
     */
    private Integer length;
    
    /**
     * 耗时(秒)
     */
    private Integer costTime;
    
    /**
     * 配置文件ID
     */
    private Long profileId;
    
    /**
     * 打印板索引
     */
    private Integer plateIndex;
    
    /**
     * 打印板名称
     */
    private String plateName;
    
    /**
     * 设备ID
     */
    private String deviceId;
    
    /**
     * AMS详细映射
     */
    private List<AmsDetailMappingDTO> amsDetailMapping;
    
    /**
     * 打印模式
     */
    private String mode;
    
    /**
     * 是否公开配置文件
     */
    private Boolean isPublicProfile;
    
    /**
     * 是否可打印
     */
    private Boolean isPrintable;
    
    /**
     * 设备型号
     */
    private String deviceModel;
    
    /**
     * 设备名称
     */
    private String deviceName;
    
    /**
     * 打印床类型
     */
    private String bedType;
}