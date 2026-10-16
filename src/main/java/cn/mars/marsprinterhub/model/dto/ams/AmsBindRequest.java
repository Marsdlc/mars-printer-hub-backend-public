package cn.mars.marsprinterhub.model.dto.ams;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * AMS 槽位绑定请求
 * 绑定操作会覆盖槽位原有的耗材绑定
 *
 * @author mars
 */
@Data
public class AmsBindRequest {

    /**
     * AMS ID
     */
    @NotNull(message = "AMS ID不能为空")
    private Long amsId;

    /**
     * 槽位编号（0~3）
     */
    @NotNull(message = "槽位编号不能为空")
    @Min(value = 0, message = "槽位编号最小为0")
    private Integer slotNo;

    /**
     * 耗材ID
     */
    @NotNull(message = "耗材ID不能为空")
    private Long spoolId;

    /**
     * 是否同步更新设备信息
     */
    private Boolean syncUpdateDevice;

    /**
     * 槽位信息索引
     */
    private String trayInfoIdx;
}
