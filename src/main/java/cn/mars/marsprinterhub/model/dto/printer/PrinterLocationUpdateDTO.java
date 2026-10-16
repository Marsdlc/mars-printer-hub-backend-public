package cn.mars.marsprinterhub.model.dto.printer;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新打印机位置请求DTO
 */
@Data
public class PrinterLocationUpdateDTO implements Serializable {

    /**
     * 打印机ID
     */
    @NotNull(message = "打印机 ID 不能为空")
    private Long printerId;

    /**
     * 打印机位置
     * 允许为空
     */
    private String location;
}
