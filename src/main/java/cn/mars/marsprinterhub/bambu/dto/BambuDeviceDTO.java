package cn.mars.marsprinterhub.bambu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 拓竹设备信息DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BambuDeviceDTO {
    
    /**
     * 设备ID
     */
    @JsonProperty("dev_id")
    private String deviceId;
    
    /**
     * 设备名称
     */
    private String name;
    
    /**
     * 是否在线
     */
    private Boolean online;
    
    /**
     * 打印状态
     */
    @JsonProperty("print_status")
    private String printStatus;
    
    /**
     * 设备型号名称
     */
    @JsonProperty("dev_model_name")
    private String deviceModelName;
    
    /**
     * 设备产品名称
     */
    @JsonProperty("dev_product_name")
    private String deviceProductName;
    
    /**
     * 设备访问码
     */
    @JsonProperty("dev_access_code")
    private String deviceAccessCode;
    
    /**
     * 喷嘴直径
     */
    @JsonProperty("nozzle_diameter")
    private Double nozzleDiameter;
    
    /**
     * 设备结构类型
     */
    @JsonProperty("dev_structure")
    private String deviceStructure;
    
    /**
     * 设备列表响应DTO
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeviceListResponse {
        private String message;
        private String code;
        private String error;
        private List<BambuDeviceDTO> devices;
    }
}
