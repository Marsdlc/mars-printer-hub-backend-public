package cn.mars.marsprinterhub.model.vo;

import cn.mars.marsprinterhub.model.enums.BambuRegionEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前用户拓竹区域视图
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBambuRegionVO {

    private BambuRegionEnum bambuRegion;
}
