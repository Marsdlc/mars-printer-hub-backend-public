package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.bambu.model.AmsStatus;
import cn.mars.marsprinterhub.bambu.mqtt.BambuMqttMessage;
import cn.mars.marsprinterhub.model.entity.Ams;
import cn.mars.marsprinterhub.model.entity.AmsSlots;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 针对表【ams(AMS设备表)】的数据库操作Service
 */
public interface AmsService extends IService<Ams> {


    /**
     * 同步AMS信息
     *
     * @param printerId 打印机ID
     * @param userId    用户ID
     * @param amsStatus AMS状态
     */
    void syncAmsInfo(Long printerId, Long userId, AmsStatus amsStatus);

    /**
     * 根据用户ID获取槽位列表
     *
     * @return 槽位列表
     */
    List<AmsSlots> getSlotsListByUserId(Long userId);

    /**
     * 同步外挂槽位
     */
    void syncAmsVtTray(Long printerId, Long userId, BambuMqttMessage mqttMessage);

    /**
     * 同步虚拟槽位
     */
    void syncAmsVirSlot(Long printerId, Long userId, BambuMqttMessage mqttMessage);

    /**
     * 根据打印机ID和AMS编号获取AMS信息
     */
    Ams getByPrinterIdAndAmsNo(Long id, Integer activeAmsNo);

    /**
     * 根据打印机ID和槽位获取槽位信息
     */
    AmsSlots getAmsSlotsByPrinterIdAndTrayNow(Long printerId, String model, Integer trayNow);

    /**
     * 根据打印机ID获取AMS列表
     */
    List<Ams> getListByPrinterId(Long printerId);

    Ams getAmsByPrinterIdAndAmsNo(Long id, Integer amsNo);

    /**
     * 更新 AMS 的 Home Assistant 温湿度实体ID
     */
    void updateHomeAssistantEntityIds(Long amsId, String temperatureEntityId, String humidityEntityId);

    void removeByUserIdAndPrinterId(Long userId, Long printerId);
}
