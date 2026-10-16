package cn.mars.marsprinterhub.model.dto.bambu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * 拓竹云任务列表响应DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BambuTaskListResponseDTO {
    /**
     * 总数
     */
    private Integer total;
    
    /**
     * 任务列表
     */
    private List<BambuTaskDTO> hits;
}
