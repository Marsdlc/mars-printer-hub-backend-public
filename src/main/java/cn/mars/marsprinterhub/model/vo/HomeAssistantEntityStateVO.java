package cn.mars.marsprinterhub.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeAssistantEntityStateVO {

    /**
     * Home Assistant 实体 ID
     */
    private String entityId;

    /**
     * 实体当前状态
     */
    private String state;

    /**
     * 单位
     */
    private String unit;
}
