package cn.mars.marsprinterhub.controller;

import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.model.dto.record.RecordQueryDTO;
import cn.mars.marsprinterhub.model.entity.Spool;
import cn.mars.marsprinterhub.model.entity.SpoolUsedRecord;
import cn.mars.marsprinterhub.model.entity.SpoolUsedSnapshots;
import cn.mars.marsprinterhub.model.vo.SpoolUsedRecordVO;
import cn.mars.marsprinterhub.model.vo.SpoolVO;
import cn.mars.marsprinterhub.service.SpoolService;
import cn.mars.marsprinterhub.service.SpoolUsedRecordService;
import cn.mars.marsprinterhub.service.SpoolUsedSnapshotsService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.mars.marsprinterhub.common.ResultUtils.success;

/**
 * 耗材使用记录控制器
 *
 * @author mars
 */
@RestController
@RequestMapping("/record")
@Tag(name = "Record", description = "耗材使用记录相关接口")
@Slf4j
public class RecordController {

    @Resource
    private SpoolService spoolService;

    @Resource
    private SpoolUsedRecordService spoolUsedRecordService;

    @Resource
    private SpoolUsedSnapshotsService spoolUsedSnapshotsService;

    @Operation(summary = "分页获取耗材使用记录")
    @GetMapping("/list/page")
    public BaseResponse<IPage<SpoolUsedRecordVO>> listSpoolUsedRecordPage(@Valid @ParameterObject RecordQueryDTO queryDTO) {
        IPage<SpoolUsedRecord> recordPage = spoolUsedRecordService.getRecordPage(queryDTO);
        IPage<SpoolUsedRecordVO> recordVOPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        recordVOPage.setRecords(spoolUsedRecordService.convertToVOList(recordPage.getRecords(), false));
        return success(recordVOPage);
    }

    @Operation(summary = "获取耗材使用记录")
    @GetMapping("/list/by-spool/{spoolId}")
    public BaseResponse<List<SpoolUsedRecordVO>> listSpoolUsedRecordBySpoolId(
            @Parameter(description = "耗材ID", required = true)
            @PathVariable Long spoolId) {
        Spool spool = spoolService.getSpoolById(spoolId);
        ThrowUtils.throwIf(spool == null, ErrorCode.NOT_FOUND_ERROR, "耗材不存在");
        SpoolVO spoolVO = spoolService.convertToVO(spool);

        List<SpoolUsedRecord> spoolUsedRecords = spoolUsedRecordService.listSpoolUsedRecordsBySpoolId(spoolId);
        List<SpoolUsedRecordVO> recordVOList = spoolUsedRecordService.convertToVOList(spoolUsedRecords, false);
        recordVOList.forEach(recordVO -> recordVO.setSpoolVO(spoolVO));
        return success(recordVOList);
    }

    @Operation(summary = "根据订单 ID获取耗材使用记录")
    @GetMapping("/list/by-order/{orderId}")
    public BaseResponse<List<SpoolUsedRecordVO>> listSpoolUsedRecordByOrderId(
            @Parameter(description = "耗材 ID", required = true)
            @PathVariable Long orderId) {
        List<SpoolUsedRecord> spoolUsedRecords = spoolUsedRecordService.listSpoolUsedRecordsByOrderId(orderId);
        List<SpoolUsedRecordVO> recordVOList = spoolUsedRecordService.convertToVOList(spoolUsedRecords, true);
        return success(recordVOList);
    }

    @Operation(summary = "获取耗材使用记录快照列表")
    @GetMapping("/list/snapshot/{recordId}")
    public BaseResponse<List<SpoolUsedSnapshots>> listSpoolUsedSnapshot(
            @Parameter(description = "记录id", required = true)
            @PathVariable Long recordId) {
        SpoolUsedRecord spoolUsedRecordId = spoolUsedRecordService.getBySpoolUsedRecordId(recordId);
        return success(spoolUsedSnapshotsService.getSpoolUsedSnapshotsByRecordId(spoolUsedRecordId.getId()));
    }

    @Operation(summary = "获取耗材使用记录详情")
    @GetMapping("/{id}")
    public BaseResponse<SpoolUsedRecordVO> getSpoolUsedRecord(
            @Parameter(description = "耗材使用记录ID", required = true)
            @PathVariable Long id) {
        SpoolUsedRecord spoolUsedRecord = spoolUsedRecordService.getBySpoolUsedRecordId(id);
        return success(spoolUsedRecordService.convertToVO(spoolUsedRecord, false));
    }

    @Operation(summary = "耗材使用记录转移")
    @PutMapping("/transfer/{id}/{newSpoolId}")
    public BaseResponse<Boolean> transferSpoolUsedRecord(
            @Parameter(description = "耗材使用记录ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "新耗材ID", required = true)
            @PathVariable Long newSpoolId) {
        spoolUsedRecordService.transferSpoolUsedRecord(id, newSpoolId);
        return success(true);
    }

    @Operation(summary = "耗材使用记录删除")
    @DeleteMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteSpoolUsedRecord(@Parameter(description = "耗材使用记录ID", required = true)
                                                       @PathVariable Long id) {
        return success(spoolUsedRecordService.deleteSpoolUsedRecord(id));
    }
}
