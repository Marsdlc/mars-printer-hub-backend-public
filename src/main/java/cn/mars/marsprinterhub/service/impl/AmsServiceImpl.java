package cn.mars.marsprinterhub.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.bambu.model.AmsInstance;
import cn.mars.marsprinterhub.bambu.model.AmsStatus;
import cn.mars.marsprinterhub.bambu.model.AmsTray;
import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttMessage;
import cn.mars.marsprinterhub.mapper.AmsMapper;
import cn.mars.marsprinterhub.model.entity.Ams;
import cn.mars.marsprinterhub.model.entity.AmsSlots;
import cn.mars.marsprinterhub.model.enums.PrinterModelEnum;
import cn.mars.marsprinterhub.service.AmsService;
import cn.mars.marsprinterhub.service.AmsSlotsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static cn.mars.marsprinterhub.constant.BambuConstants.TRAY_EXTERNAL;

/**
 * 针对表【ams(AMS设备表)】的数据库操作Service实现
 */
@Service
@Slf4j
public class AmsServiceImpl extends ServiceImpl<AmsMapper, Ams> implements AmsService {

    private static final int AMS_HT_TASK_REF_MIN = 16;
    private static final int AMS_HT_TASK_REF_MAX = 24;
    private static final int AMS_HT_AMS_NO_OFFSET = 112;

    @Resource
    private AmsSlotsService amsSlotsService;

    @Override
    public void syncAmsInfo(Long printerId, Long userId, AmsStatus amsStatus) {
        // 获取所有AMS
        List<AmsInstance> amsList = amsStatus.getAmsList();
        if (CollUtil.isEmpty(amsList)) return;
        for (AmsInstance amsInstance : amsList) {
            Integer amsNo = Integer.valueOf(amsInstance.getId());
            Ams ams = getByPrinterIdAndAmsNo(printerId, amsNo);
            if (ams == null) {
                ams = new Ams();
                ams.setPrinterId(printerId);
                ams.setUserId(userId);
                ams.setAmsNo(amsNo);
            }
            boolean saveOrUpdate = this.saveOrUpdate(ams);
            if (!saveOrUpdate) {
                log.warn("同步AMS信息失败: printerId={}, amsNo={}", printerId, amsNo);
                continue;
            }
            // 同步 ams 槽信息
            amsSlotsService.syncAmsSlotsInfo(ams.getId(), userId, amsInstance.getTray());
        }
    }

    @Override
    public List<AmsSlots> getSlotsListByUserId(Long userId) {
        return amsSlotsService.getSlotsListByUserId(userId);
    }

    @Override
    public void syncAmsVtTray(Long printerId, Long userId, BambuMqttMessage mqttMessage) {
        AmsTray vtTray = mqttMessage.getVtTray();
        if (vtTray == null) {
            return;
        }
        // 根据打印机id和ams编号获取AMS
        Ams ams = getByPrinterIdAndAmsNo(printerId, TRAY_EXTERNAL);
        if (ams == null) {
            ams = new Ams();
            ams.setPrinterId(printerId);
            ams.setUserId(userId);
            ams.setAmsNo(TRAY_EXTERNAL);
        }
        boolean saveOrUpdate = this.saveOrUpdate(ams);
        if (!saveOrUpdate) {
            log.warn("同步外挂信息失败: printerId={}, amsNo={}", printerId, TRAY_EXTERNAL);
            return;
        }
        // 同步 ams 槽信息
        amsSlotsService.syncAmsSlotsInfo(ams.getId(), userId, Collections.singletonList(vtTray));
    }

    @Override
    public void syncAmsVirSlot(Long printerId, Long userId, BambuMqttMessage mqttMessage) {
        List<AmsTray> virSlot = mqttMessage.getVirSlot();
        if (CollUtil.isEmpty(virSlot)) {
            return;
        }
        for (AmsTray amsTray : virSlot) {
            String id = amsTray.getId();
            // 根据打印机id和ams编号获取AMS
            Ams ams = getByPrinterIdAndAmsNo(printerId, Integer.valueOf(id));
            if (ams == null) {
                ams = new Ams();
                ams.setPrinterId(printerId);
                ams.setUserId(userId);
                ams.setAmsNo(Integer.valueOf(id));
            }
            boolean saveOrUpdate = this.saveOrUpdate(ams);
            if (!saveOrUpdate) {
                log.warn("同步外挂信息失败: printerId={}, amsNo={}", printerId, TRAY_EXTERNAL);
                return;
            }
            // 同步 ams 槽信息
            amsSlotsService.syncAmsSlotsInfo(ams.getId(), userId, Collections.singletonList(amsTray));
        }
    }

    @Override
    public Ams getByPrinterIdAndAmsNo(Long id, Integer activeAmsNo) {
        return this.getOne(
                new LambdaQueryWrapper<Ams>()
                        .eq(Ams::getPrinterId, id)
                        .eq(Ams::getAmsNo, activeAmsNo)
        );
    }

    @Override
    public AmsSlots getAmsSlotsByPrinterIdAndTrayNow(Long printerId, String model, Integer trayNow) {
        // 计算AMS索引和槽位ID
        // 这边特殊处理，对老设备，如果是外挂料盘，任务中实际返回的值是255，但是ams中是以254表示外挂料盘
        int activeAmsNo;
        int activeAmsSlotNo;

        // 1. AMS HT（手机发起的任务接口可能返回 16-24，设备同步表中存的是 128-136）
        if (trayNow >= AMS_HT_TASK_REF_MIN && trayNow <= AMS_HT_TASK_REF_MAX) {
            activeAmsNo = trayNow + AMS_HT_AMS_NO_OFFSET;
            activeAmsSlotNo = 0;
        }
        // 2. AMS HT（设备侧原始编码）
        else if (trayNow >= 128 && trayNow <= 136) {
            activeAmsNo = trayNow;
            activeAmsSlotNo = 0;
        }
        // 3. 外挂料盘
        else if (trayNow == 254 || trayNow == 255) {
            PrinterModelEnum printerModelEnum = PrinterModelEnum.getEnumByValue(model);
            boolean isOldModel = printerModelEnum == PrinterModelEnum.P1S
                    || printerModelEnum == PrinterModelEnum.A1
                    || printerModelEnum == PrinterModelEnum.A1MINI;

            if (isOldModel) {
                activeAmsNo = 254;
                activeAmsSlotNo = 254;
            } else {
                activeAmsNo = trayNow;
                activeAmsSlotNo = trayNow;
            }
        }
        // 4. 普通 AMS 槽位
        else {
            activeAmsNo = trayNow >> 2;
            activeAmsSlotNo = trayNow & 0x3;
        }

        // 获取 AMS 信息
        Ams ams = getByPrinterIdAndAmsNo(printerId, activeAmsNo);
        if (ams == null) {
            return null;
        }

        // 获取槽位信息
        return amsSlotsService.getByAmsIdAndSlotNo(ams.getId(), activeAmsSlotNo);
    }

    @Override
    public List<Ams> getListByPrinterId(Long printerId) {
        return this.list(
                new LambdaQueryWrapper<Ams>()
                        .eq(Ams::getPrinterId, printerId)
        );
    }

    @Override
    public Ams getAmsByPrinterIdAndAmsNo(Long id, Integer amsNo) {
        return this.getOne(
                new LambdaQueryWrapper<Ams>()
                        .eq(Ams::getPrinterId, id)
                        .eq(Ams::getAmsNo, amsNo)
        );
    }

    @Override
    public void updateHomeAssistantEntityIds(Long amsId, String temperatureEntityId, String humidityEntityId) {
        update(
                new LambdaUpdateWrapper<Ams>()
                        .eq(Ams::getId, amsId)
                        .set(Ams::getTemperatureEntityId, StrUtil.isBlank(temperatureEntityId) ? null : temperatureEntityId)
                        .set(Ams::getHumidityEntityId, StrUtil.isBlank(humidityEntityId) ? null : humidityEntityId)
        );
    }

    @Override
    public void removeByUserIdAndPrinterId(Long userId, Long printerId) {
        List<Ams> list = list(
                new LambdaQueryWrapper<Ams>()
                        .eq(Ams::getUserId, userId)
                        .eq(Ams::getPrinterId, printerId)
        );
        if (CollUtil.isEmpty(list)) {
            return;
        }

        List<Long> ids = list.stream().map(Ams::getId).toList();
        removeByIds(ids);
        amsSlotsService.removeByAmsIds(ids);
    }

}
