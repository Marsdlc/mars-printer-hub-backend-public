package cn.mars.marsprinterhub.service.impl;

import cn.mars.marsprinterhub.bambu.model.AmsTray;
import cn.mars.marsprinterhub.mapper.AmsSlotsMapper;
import cn.mars.marsprinterhub.model.entity.AmsSlots;
import cn.mars.marsprinterhub.service.AmsSlotsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 针对表【ams_slots(AMS槽位绑定表)】的数据库操作Service实现
 */
@Service
@Slf4j
public class AmsSlotsServiceImpl extends ServiceImpl<AmsSlotsMapper, AmsSlots> implements AmsSlotsService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindSlotSpool(Long amsId, Integer slotNo, Long spoolId) {
        LambdaUpdateWrapper<AmsSlots> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AmsSlots::getAmsId, amsId)
                .eq(AmsSlots::getSlotNo, slotNo)
                .set(AmsSlots::getUpdateTime, new Date());
        // 0 表示解绑
        if (spoolId == 0) {
            updateWrapper.set(AmsSlots::getSpoolId, null);
        } else {
            updateWrapper.set(AmsSlots::getSpoolId, spoolId);
        }
        return this.update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncAmsSlotsInfo(Long amsId, Long userId, List<AmsTray> tray) {
        List<AmsSlots> amsSlotsList = new ArrayList<>();
        for (AmsTray amsTray : tray) {
            Integer slotNo = Integer.valueOf(amsTray.getId());
            AmsSlots amsSlots = this.getOne(
                    new LambdaQueryWrapper<AmsSlots>()
                            .eq(AmsSlots::getUserId, userId)
                            .eq(AmsSlots::getAmsId, amsId)
                            .eq(AmsSlots::getSlotNo, slotNo)
            );
            if (amsSlots == null) {
                amsSlots = new AmsSlots();
                amsSlots.setAmsId(amsId);
                amsSlots.setUserId(userId);
                amsSlots.setSlotNo(slotNo);
            }
            amsSlotsList.add(amsSlots);
        }
        this.saveOrUpdateBatch(amsSlotsList);
    }

    @Override
    public AmsSlots getBySpoolId(Long spoolId, Long userId) {
        return this.getOne(
                new LambdaQueryWrapper<AmsSlots>()
                        .eq(AmsSlots::getSpoolId, spoolId)
                        .eq(AmsSlots::getUserId, userId)
        );
    }

    @Override
    public List<AmsSlots> getSlotsListByUserId(Long userId) {
        return this.list(
                new LambdaQueryWrapper<AmsSlots>()
                        .eq(AmsSlots::getUserId, userId)
        );
    }

    @Override
    public AmsSlots getByAmsIdAndSlotNo(Long amsId, Integer activeAmsSlotNo) {
        return this.getOne(
                new LambdaQueryWrapper<AmsSlots>()
                        .eq(AmsSlots::getAmsId, amsId)
                        .eq(AmsSlots::getSlotNo, activeAmsSlotNo)
        );
    }

    @Override
    public List<Long> getBindSpoolIds(Long userId) {
        return this.listObjs(
                new LambdaQueryWrapper<AmsSlots>()
                        .eq(AmsSlots::getUserId, userId)
                        .isNotNull(AmsSlots::getSpoolId)
                        .select(AmsSlots::getSpoolId)
        );
    }

    @Override
    public void removeByAmsIds(List<Long> amsIds) {
        this.remove(
                new LambdaQueryWrapper<AmsSlots>()
                        .in(AmsSlots::getAmsId, amsIds)
        );
    }

    @Override
    public void updateToNullBySpoolId(Long spoolId) {
        if (spoolId == null) {
            return;
        }
        this.update(
                new LambdaUpdateWrapper<AmsSlots>()
                        .eq(AmsSlots::getSpoolId, spoolId)
                        .setSql("spool_id = NULL")
        );
    }
}
