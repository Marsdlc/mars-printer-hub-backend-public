package cn.mars.marsprinterhub.event.events;

import cn.mars.marsprinterhub.event.core.Event;
import cn.mars.marsprinterhub.model.enums.BambuRegionEnum;
import lombok.Getter;

/**
 * 用户拓竹区域变更事件
 */
@Getter
public class UserBambuRegionChangedEvent extends Event {

    private final Long userId;

    private final BambuRegionEnum oldRegion;

    private final BambuRegionEnum newRegion;

    public UserBambuRegionChangedEvent(Long userId, BambuRegionEnum oldRegion, BambuRegionEnum newRegion) {
        super("USER_BAMBU_REGION_CHANGED", "UserService");
        this.userId = userId;
        this.oldRegion = oldRegion;
        this.newRegion = newRegion;
    }
}
