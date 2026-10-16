package cn.mars.marsprinterhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.mapper.PrinterMapper;
import cn.mars.marsprinterhub.model.dto.printer.PrinterQueryDTO;
import cn.mars.marsprinterhub.model.dto.printer.PrinterSortUpdateDTO;
import cn.mars.marsprinterhub.model.entity.Printer;
import cn.mars.marsprinterhub.model.enums.PrinterModelEnum;
import cn.mars.marsprinterhub.model.enums.PrinterStatusEnum;
import cn.mars.marsprinterhub.model.vo.PrinterOverviewVO;
import cn.mars.marsprinterhub.model.vo.PrinterVO;
import cn.mars.marsprinterhub.service.PrinterService;
import cn.mars.marsprinterhub.service.UserService;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCache;
import cn.mars.marsprinterhub.service.cache.DeviceStatusCacheService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_TASK_PREFIX;
import static cn.mars.marsprinterhub.constant.RedisConstants.DEVICE_USER_PREFIX;

/**
 * 针对表【printers(拓竹打印机表)】的数据库操作Service实现
 */
@Service
@Slf4j
public class PrinterServiceImpl extends ServiceImpl<PrinterMapper, Printer>
        implements PrinterService {

    @Resource
    private UserService userService;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private DeviceStatusCacheService deviceStatusCacheService;

    @Override
    public Printer getById(Long id) {
        Printer printer = this.getBaseMapper().selectById(id);
        ThrowUtils.throwIf(printer == null, ErrorCode.NOT_FOUND_ERROR, "打印机不存在");
        return printer;
    }

    @Override
    public Printer getByDeviceId(String deviceId) {
        return this.getOne(
                new LambdaQueryWrapper<Printer>()
                        .eq(Printer::getDeviceId, deviceId)
        );
    }

    @Override
    public Printer getCurrentUserPrinterByDeviceId(String deviceId) {
        ThrowUtils.throwIf(StrUtil.isBlank(deviceId), ErrorCode.PARAMS_ERROR, "设备 ID 不能为空");
        Printer printer = getByDeviceId(deviceId);
        ThrowUtils.throwIf(printer == null, ErrorCode.NOT_FOUND_ERROR, "打印机不存在");
        ThrowUtils.throwIf(!Objects.equals(userService.getCurrentUserId(), printer.getUserId()),
                ErrorCode.NO_AUTH_ERROR, "无权操作该打印机");
        return printer;
    }

    @Override
    public IPage<Printer> getPage(long current, long pageSize, PrinterQueryDTO queryDTO) {
        // 1. 获取当前用户
        Long currentUserId = userService.getCurrentUser().getId();

        // 2. 构建查询条件
        LambdaQueryWrapper<Printer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Printer::getUserId, currentUserId);

        if (StrUtil.isNotBlank(queryDTO.getName())) {
            queryWrapper.like(Printer::getName, queryDTO.getName());
        }
        if (StrUtil.isNotBlank(queryDTO.getDeviceId())) {
            queryWrapper.eq(Printer::getDeviceId, queryDTO.getDeviceId());
        }
        if (StrUtil.isNotBlank(queryDTO.getModel())) {
            queryWrapper.eq(Printer::getModel, queryDTO.getModel());
        }
        if (StrUtil.isNotBlank(queryDTO.getIpAddress())) {
            queryWrapper.eq(Printer::getIpAddress, queryDTO.getIpAddress());
        }

        queryWrapper.orderByAsc(Printer::getSort).orderByDesc(Printer::getCreateTime);

        // 3. 分页查询
        Page<Printer> page = new Page<>(current, pageSize);
        return this.page(page, queryWrapper);
    }

    @Override
    public List<Printer> getListByUserId(Long userId) {
        return this.list(
                new LambdaQueryWrapper<Printer>()
                        .eq(Printer::getUserId, userId)
                        .orderByAsc(Printer::getSort)
        );
    }

    @Override
    public void createCache() {
        List<Printer> printerList = this.list();
        for (Printer printer : printerList) {
            String key = DEVICE_USER_PREFIX + printer.getDeviceId();
            redisUtils.set(key, printer.getUserId());
        }
    }

    @Override
    public PrinterVO convertToVO(Printer printer) {
        if (printer == null) {
            return null;
        }
        PrinterVO printerVO = new PrinterVO();
        BeanUtil.copyProperties(printer, printerVO);
        PrinterModelEnum printerModelEnum = PrinterModelEnum.getEnumByValue(printer.getModel());
        printerVO.setModel(printerModelEnum != null ? printerModelEnum.getText() : printer.getModel());
        return printerVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSort(List<PrinterSortUpdateDTO> printerSortList) {
        if (printerSortList == null || printerSortList.isEmpty()) {
            return;
        }
        Long currentUserId = userService.getCurrentUserId();
        for (PrinterSortUpdateDTO dto : printerSortList) {
            ThrowUtils.throwIf(dto == null || dto.getId() == null || dto.getSort() == null,
                    ErrorCode.PARAMS_ERROR, "打印机 ID 和排序值不能为空");
            boolean updated = this.update(new LambdaUpdateWrapper<Printer>()
                    .eq(Printer::getId, dto.getId())
                    .eq(Printer::getUserId, currentUserId)
                    .set(Printer::getSort, dto.getSort()));
            ThrowUtils.throwIf(!updated, ErrorCode.NO_AUTH_ERROR, "无权操作该打印机");
        }
    }

    @Override
    public void updateLocation(Long printerId, String location) {
        Printer printer = new Printer();
        printer.setId(printerId);
        printer.setLocation(location);
        this.updateById(printer);
    }

    @Override
    public void updatePowerEntityId(Long printerId, String powerEntityId) {
        this.update(new LambdaUpdateWrapper<Printer>()
                .eq(Printer::getId, printerId)
                .set(Printer::getPowerEntityId, StrUtil.trim(powerEntityId)));
    }

    @Override
    public void updateChamberTempEntityId(Long printerId, String chamberTempEntityId) {
        this.update(new LambdaUpdateWrapper<Printer>()
                .eq(Printer::getId, printerId)
                .set(Printer::getChamberTempEntityId, StrUtil.trim(chamberTempEntityId)));
    }

    @Override
    public void updateCustomCameraRtspUrl(Long printerId, String customCameraRtspUrl) {
        this.update(new LambdaUpdateWrapper<Printer>()
                .eq(Printer::getId, printerId)
                .set(Printer::getCustomCameraRtspUrl, StrUtil.trim(customCameraRtspUrl)));
    }

    @Override
    public List<PrinterOverviewVO> getOverviewList() {
        Long currentUserId = userService.getCurrentUser().getId();
        List<Printer> printerList = this.getListByUserId(currentUserId);

        return printerList.stream()
                .map(printer -> {
                    String deviceId = printer.getDeviceId();
                    DeviceStatusCache deviceStatus = deviceStatusCacheService.getDeviceStatus(deviceId);
                    PrinterModelEnum printerModelEnum = PrinterModelEnum.getEnumByValue(printer.getModel());
                    String modelText = printerModelEnum == null ? printer.getModel() : printerModelEnum.getText();
                    String redisTaskKey = DEVICE_TASK_PREFIX + deviceId;
                    Object taskImage = redisUtils.get(redisTaskKey);

                    return PrinterOverviewVO.builder()
                            .id(printer.getId())
                            .name(printer.getName())
                            .model(modelText)
                            .deviceId(deviceId)
                            .status(deviceStatus == null || deviceStatus.getPrintStatus() == null
                                    ? PrinterStatusEnum.OFFLINE.getValue()
                                    : deviceStatus.getPrintStatus())
                            .location(printer.getLocation())
                            .mcPercent(deviceStatus == null || deviceStatus.getMcPercent() == null
                                    ? 0 : deviceStatus.getMcPercent())
                            .remainingTime(deviceStatus == null || deviceStatus.getRemainingTime() == null
                                    ? 0 : deviceStatus.getRemainingTime())
                            .coverUrl(Objects.toString(taskImage, null))
                            .build();
                })
                .toList();
    }
}
