package cn.mars.marsprinterhub.model.dto.printer;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrinterUnbindDTO {

    /**
     * 打印机id
     */
    @NotNull(message = "打印机id不能为空")
    private Long printerId;
}
