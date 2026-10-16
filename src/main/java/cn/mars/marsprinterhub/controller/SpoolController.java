package cn.mars.marsprinterhub.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.model.dto.spool.*;
import cn.mars.marsprinterhub.model.entity.AmsSlots;
import cn.mars.marsprinterhub.model.entity.Spool;
import cn.mars.marsprinterhub.model.vo.HomeAssistantEntityStateVO;
import cn.mars.marsprinterhub.model.vo.SpoolQueryOptionsVO;
import cn.mars.marsprinterhub.model.vo.SpoolSummaryVO;
import cn.mars.marsprinterhub.model.vo.SpoolVO;
import cn.mars.marsprinterhub.service.AmsService;
import cn.mars.marsprinterhub.service.SpoolService;
import cn.mars.marsprinterhub.service.SpoolCalibrationService;
import cn.mars.marsprinterhub.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.mars.marsprinterhub.common.ResultUtils.success;

/**
 * 耗材管理控制器
 * 提供耗材的增删改查、导入导出、使用记录等功能
 *
 * @author mars
 */
@RestController
@RequestMapping("/spool")
@Tag(name = "Spool", description = "耗材相关接口")
@Slf4j
public class SpoolController {

    @Resource
    private SpoolService spoolService;

    @Resource
    private AmsService amsService;

    @Resource
    private SpoolCalibrationService spoolCalibrationService;

    @Resource
    private UserService userService;

    @Operation(summary = "创建耗材")
    @PostMapping("/add")
    public BaseResponse<SpoolVO> addSpool(@Valid @RequestBody SpoolCreateDTO createDTO) {
        Spool spool = BeanUtil.copyProperties(createDTO, Spool.class);
        Spool createdSpool = spoolService.createSpool(spool, createDTO.getNum());
        return success(spoolService.convertToVO(createdSpool));
    }

    @Operation(summary = "更新耗材")
    @PutMapping("/update")
    public BaseResponse<SpoolVO> updateSpool(@Valid @RequestBody SpoolUpdateDTO updateDTO) {
        Spool spool = BeanUtil.copyProperties(updateDTO, Spool.class);
        Spool updatedSpool = spoolService.updateSpool(spool);
        return success(spoolService.convertToVO(updatedSpool));
    }

    @Operation(summary = "删除耗材")
    @DeleteMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteSpool(
            @Parameter(description = "耗材ID", required = true)
            @PathVariable Long id) {
        return success(spoolService.deleteSpool(id));
    }

    @Operation(summary = "获取耗材")
    @GetMapping("/{id}")
    public BaseResponse<SpoolVO> getSpool(
            @Parameter(description = "耗材ID", required = true)
            @PathVariable Long id) {
        Spool spool = spoolService.getSpoolById(id);
        return success(spoolService.convertToVO(spool));
    }

    @Operation(summary = "获取搜索栏下拉选项")
    @GetMapping("/options")
    public BaseResponse<SpoolQueryOptionsVO> getOptions() {
        SpoolQueryOptionsVO options = spoolService.getOptions();
        return success(options);
    }

    @Operation(summary = "分页获取耗材列表")
    @GetMapping("/list/page")
    public BaseResponse<IPage<SpoolVO>> listSpools(@Valid @ParameterObject SpoolQueryDTO queryDTO) {
        IPage<Spool> spoolPage = spoolService.getSpoolPage(queryDTO);
        IPage<SpoolVO> spoolVOPage = spoolPage.convert(spoolService::convertToVO);
        return success(spoolVOPage);
    }

    @Operation(summary = "获取所有耗材列表")
    @GetMapping("/list/all")
    public BaseResponse<List<SpoolVO>> getAllSpools() {
        List<Spool> spools = spoolService.getAllSpools();
        List<SpoolVO> spoolVOList = spools.stream()
                .sorted(
                        Comparator
                                // 是否为 0：false 在前，true 在后
                                .comparing(
                                        (Spool s) ->
                                                s.getNetWeight().subtract(s.getUsedWeight())
                                                        .compareTo(BigDecimal.ZERO) == 0
                                )
                                // 剩余重量升序
                                .thenComparing(
                                        s -> s.getNetWeight().subtract(s.getUsedWeight())
                                )
                )
                .map(spoolService::convertToVO)
                .toList();
        return success(spoolVOList);
    }

    @Operation(summary = "获取所有耗材列表（可用的）")
    @GetMapping("/list/available")
    public BaseResponse<List<SpoolVO>> getAllAvailableSpools() {
        List<Spool> spools = spoolService.getAllSpools();
        List<SpoolVO> spoolVOList = spools.stream()
                .filter(spool -> spool.getNetWeight().compareTo(spool.getUsedWeight()) > 0)
                .map(spoolService::convertToVO)
                .toList();
        return success(spoolVOList);
    }

    @Operation(summary = "获取所有耗材列表（可用的，包含指定耗材）")
    @GetMapping("/list/available/with-id")
    public BaseResponse<List<SpoolVO>> getAllAvailableSpoolsWithId(
            @Parameter(description = "需要保留的耗材ID（即使已用完也返回）", required = true)
            @RequestParam Long spoolId) {
        List<Spool> spools = spoolService.getAllSpools();
        List<SpoolVO> spoolVOList = spools.stream()
                .filter(spool -> {
                    if (Objects.equals(spool.getId(), spoolId)) {
                        return true;
                    }
                    BigDecimal netWeight = spool.getNetWeight() == null ? BigDecimal.ZERO : spool.getNetWeight();
                    BigDecimal usedWeight = spool.getUsedWeight() == null ? BigDecimal.ZERO : spool.getUsedWeight();
                    return netWeight.compareTo(usedWeight) > 0;
                })
                .map(spoolService::convertToVO)
                .toList();
        return success(spoolVOList);
    }

    @Operation(summary = "获取所有耗材列表（已绑定）")
    @GetMapping("/list/bound")
    public BaseResponse<List<SpoolVO>> listSpoolsBound() {
        // 获取当前用户绑定的耗材ID
        Set<Long> boundSpoolIds = amsService.getSlotsListByUserId(userService.getCurrentUser().getId()).stream()
                .map(AmsSlots::getSpoolId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 获取所有已绑定的耗材
        List<SpoolVO> spoolVOList = spoolService.getAllSpools().stream()
                .filter(spool -> boundSpoolIds.contains(spool.getId()))
                .map(spoolService::convertToVO)
                .toList();
        return success(spoolVOList);
    }

    @Operation(summary = "获取最近新增的5条耗材")
    @GetMapping("/list/latest5")
    public BaseResponse<List<SpoolVO>> listLatest5Spools() {
        List<Spool> recentSpools = spoolService.getLatest5Spools();
        List<SpoolVO> spoolVOList = recentSpools.stream()
                .map(spoolService::convertToVO)
                .toList();
        return success(spoolVOList);
    }

    @Operation(summary = "使用耗材")
    @PostMapping("/use")
    public BaseResponse<SpoolVO> useSpool(@Valid @RequestBody SpoolUseDTO useDTO) {
        Spool updatedSpool = spoolService.useSpool(useDTO.getSpoolId(), useDTO.getUsedWeight(), "手动使用");
        return success(spoolService.convertToVO(updatedSpool));
    }

    @Operation(summary = "校准耗材")
    @PostMapping("/calibrate")
    public BaseResponse<SpoolVO> calibrateSpool(@Valid @RequestBody SpoolCheckDTO checkDTO) {
        Spool updatedSpool = spoolService.checkSpool(checkDTO.getSpoolId(), checkDTO.getUsedWeight());
        return success(spoolService.convertToVO(updatedSpool));
    }

    @Operation(summary = "读取校准重量", description = "读取系统配置中的 Home Assistant 电子秤实体当前状态，用于耗材校准")
    @GetMapping("/calibrate/weight")
    public BaseResponse<HomeAssistantEntityStateVO> getCalibrateWeight() {
        return success(spoolCalibrationService.getCalibrateWeight());
    }

    @GetMapping("/summary")
    @Operation(summary = "获取耗材汇总信息")
    public BaseResponse<SpoolSummaryVO> getSpoolSummary(
            @Parameter(description = "是否排除已用完的耗材")
            @RequestParam(required = false, defaultValue = "false") Boolean excludeFinished,
            @Parameter(description = "品牌，单个值，精准匹配")
            @RequestParam(required = false) String brand,
            @Parameter(description = "材料类型，单个值，精准匹配")
            @RequestParam(required = false) String materialType) {
        return success(spoolService.getSpoolSummary(excludeFinished, brand, materialType));
    }
}
