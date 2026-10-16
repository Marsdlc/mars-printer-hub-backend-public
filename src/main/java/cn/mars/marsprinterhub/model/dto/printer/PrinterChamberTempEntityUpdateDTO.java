package cn.mars.marsprinterhub.model.dto.printer;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新打印机仓温实体请求 DTO
 */
@Data
public class PrinterChamberTempEntityUpdateDTO implements Serializable {

    /**
     * 打印机 ID
     */
    @NotNull(message = "打印机 ID 不能为空")
    private Long printerId;

    /**
     * Home Assistant 仓温实体 ID
     * 允许为空，表示取消绑定
     */
    private String chamberTempEntityId;
}
