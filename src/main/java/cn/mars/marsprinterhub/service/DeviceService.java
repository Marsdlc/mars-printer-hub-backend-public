package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.dto.ams.AmsBindRequest;
import cn.mars.marsprinterhub.model.dto.ams.AmsEntityUpdateDTO;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.vo.DeviceCompleteVO;
import cn.mars.marsprinterhub.model.vo.DeviceSimpleVO;
import cn.mars.marsprinterhub.model.vo.DeviceStreamVO;
import cn.mars.marsprinterhub.model.vo.HomeAssistantEntityStateVO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

public interface DeviceService {

    /**
     * 从拓竹API同步打印机信息
     */
    boolean syncPrintersFromBambu();

    /**
     * 获取设备列表
     */
    List<Printer> getDeviceList();

    /**
     * 设备转换成VO
     */
    DeviceSimpleVO convertToVO(Printer printer);

    /**
     * 获取设备完整信息
     */
    DeviceCompleteVO getDeviceCompleteInfo(Long printerId);

    /**
     * 绑定槽位和耗材
     */
    boolean bindSlotSpool(AmsBindRequest request);

    /**
     * 切换主打印腔灯
     */
    void switchChamberLight(String deviceId);

    /**
     * 切换主打印腔灯2
     */
    void switchChamberLight2(String deviceId);

    /**
     * 获取设备流内容
     */
    ResponseEntity<StreamingResponseBody> getStreamContent(String deviceId);

    /**
     * 获取设备统一流信息
     */
    DeviceStreamVO getStreamInfo(String deviceId);

    /**
     * 断开设备统一流
     *
     * @param deviceId 设备ID
     * @param type     auto / hls / mjpeg / all
     */
    boolean disconnectStream(String deviceId, String type);

    /**
     * 获取 Home Assistant 实体完整状态
     */
    HomeAssistantEntityStateVO getHomeAssistantEntityState(String entityId);

    /**
     * 根据打印机 ID 获取功率信息
     */
    HomeAssistantEntityStateVO getPrinterPower(Long printerId);

    /**
     * 更新 AMS 温湿度实体绑定
     */
    boolean updateAmsEntities(AmsEntityUpdateDTO request);
}
