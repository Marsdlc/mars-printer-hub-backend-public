package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.bambu.model.AmsTray;
import cn.mars.marsprinterhub.model.entity.AmsSlots;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 针对表【ams_slots(AMS槽位绑定表)】的数据库操作Service
 */
public interface AmsSlotsService extends IService<AmsSlots> {


    /**
     * 绑定槽位和耗材
     *
     * @param amsId   AMS ID
     * @param slotNo  槽位编号
     * @param spoolId 耗材ID
     * @return 是否成功
     */
    boolean bindSlotSpool(Long amsId, Integer slotNo, Long spoolId);

    /**
     * 同步槽位信息
     *
     * @param amsId  AMS ID
     * @param userId 用户ID
     * @param tray   槽位列表
     */
    void syncAmsSlotsInfo(Long amsId, Long userId, List<AmsTray> tray);

    /**
     * 根据耗材ID获取槽位信息
     *
     * @param spoolId 耗材ID
     * @param userId  用户ID
     * @return 槽位信息
     */
    AmsSlots getBySpoolId(Long spoolId, Long userId);

    /**
     * 根据用户ID获取槽位信息
     *
     * @param userId 用户ID
     * @return 槽位列表
     */
    List<AmsSlots> getSlotsListByUserId(Long userId);

    /**
     * 根据AmsId和槽位编号获取槽位信息
     */
    AmsSlots getByAmsIdAndSlotNo(Long amsId, Integer activeAmsSlotNo);

    /**
     * 获取所有已绑定的耗材ID
     */
    List<Long> getBindSpoolIds(Long userId);

    /**
     * 根据AMS ID列表删除槽位信息
     */
    void removeByAmsIds(List<Long> amsIds);

    /**
     * 根据耗材ID更新为空
     *
     * @param spoolId 耗材ID
     */
    void updateToNullBySpoolId(Long spoolId);
}
