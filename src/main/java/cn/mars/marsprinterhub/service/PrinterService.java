package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.dto.printer.PrinterQueryDTO;
import cn.mars.marsprinterhub.model.dto.printer.PrinterSortUpdateDTO;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.vo.PrinterOverviewVO;
import cn.mars.marsprinterhub.model.vo.PrinterVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 针对表【printers(拓竹打印机表)】的数据库操作Service
 */
public interface PrinterService extends IService<Printer> {

    /**
     * 根据ID获取打印机信息
     */
    Printer getById(Long id);

    Printer getByDeviceId(String deviceId);

    /**
     * 根据设备 ID 获取当前用户拥有的打印机
     */
    Printer getCurrentUserPrinterByDeviceId(String deviceId);

    /**
     * 分页查询打印机列表
     *
     * @param current  当前页
     * @param pageSize 每页大小
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<Printer> getPage(long current, long pageSize, PrinterQueryDTO queryDTO);

    /**
     * 根据用户ID获取打印机列表
     *
     * @param userId 用户ID
     * @return 打印机列表
     */
    List<Printer> getListByUserId(Long userId);

    void createCache();

    /**
     * 转换为VO对象
     *
     * @param printer 打印机实体
     * @return VO对象
     */
    PrinterVO convertToVO(Printer printer);

    /**
     * 批量更新排序
     *
     * @param printerSortList 排序列表
     */
    void updateSort(List<PrinterSortUpdateDTO> printerSortList);

    /**
     * 更新打印机位置
     *
     * @param printerId 打印机 ID
     * @param location  位置（格式：房间-货架-层，如1-2-3）
     */
    void updateLocation(Long printerId, String location);

    /**
     * 更新打印机功耗实体ID
     *
     * @param printerId      打印机 ID
     * @param powerEntityId  Home Assistant 功耗实体 ID
     */
    void updatePowerEntityId(Long printerId, String powerEntityId);

    /**
     * 更新打印机仓温实体ID
     *
     * @param printerId             打印机 ID
     * @param chamberTempEntityId   Home Assistant 仓温实体 ID
     */
    void updateChamberTempEntityId(Long printerId, String chamberTempEntityId);

    /**
     * 更新打印机自定义摄像头RTSP路径
     *
     * @param printerId            打印机 ID
     * @param customCameraRtspUrl  自定义摄像头RTSP路径
     */
    void updateCustomCameraRtspUrl(Long printerId, String customCameraRtspUrl);

    /**
     * 获取打印机概览列表
     *
     * @return 概览列表
     */
    List<PrinterOverviewVO> getOverviewList();

}
