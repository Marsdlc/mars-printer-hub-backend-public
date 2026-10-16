package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.entity.SpoolUsedSnapshots;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author mars
 * @description 针对表【spool_used_snapshots(耗材使用快照)】的数据库操作Service
 * @createDate 2025-08-27 11:07:08
 */
public interface SpoolUsedSnapshotsService extends IService<SpoolUsedSnapshots> {

    /**
     * 根据记录ID获取耗材使用快照
     *
     * @param recordId 记录ID
     * @return 耗材使用快照
     */
    List<SpoolUsedSnapshots> getSpoolUsedSnapshotsByRecordId(Long recordId);

    /**
     * 批量插入数据，忽略已存在的数据
     *
     * @param list 待插入的数据列表
     */
    void insertBatchIgnore(List<SpoolUsedSnapshots> list);
}
