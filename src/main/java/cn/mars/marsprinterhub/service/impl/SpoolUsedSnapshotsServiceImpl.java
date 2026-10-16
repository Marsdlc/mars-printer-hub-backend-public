package cn.mars.marsprinterhub.service.impl;

import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.mapper.SpoolUsedSnapshotsMapper;
import cn.mars.marsprinterhub.model.entity.SpoolUsedSnapshots;
import cn.mars.marsprinterhub.service.SpoolUsedSnapshotsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author mars
 * @description 针对表【spool_used_snapshots(耗材使用快照)】的数据库操作Service实现
 * @createDate 2025-08-27 11:07:08
 */
@Service
public class SpoolUsedSnapshotsServiceImpl extends ServiceImpl<SpoolUsedSnapshotsMapper, SpoolUsedSnapshots>
        implements SpoolUsedSnapshotsService {

    @Resource
    private SpoolUsedSnapshotsMapper spoolUsedSnapshotsMapper;

    @Override
    public List<SpoolUsedSnapshots> getSpoolUsedSnapshotsByRecordId(Long recordId) {
        ThrowUtils.throwIf(recordId == null, ErrorCode.PARAMS_ERROR, "记录ID不能为空");
        return this.list(
                new LambdaQueryWrapper<SpoolUsedSnapshots>()
                        .eq(SpoolUsedSnapshots::getRecordId, recordId)
                        .orderByDesc(SpoolUsedSnapshots::getPercent)
        );
    }

    @Override
    public void insertBatchIgnore(List<SpoolUsedSnapshots> list) {
        spoolUsedSnapshotsMapper.insertBatchIgnore(list);
    }
}




