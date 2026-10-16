package cn.mars.marsprinterhub.mapper;

import cn.mars.marsprinterhub.model.entity.SpoolUsedSnapshots;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author mars
 * @description 针对表【spool_used_snapshots(耗材使用快照)】的数据库操作Mapper
 * @createDate 2025-08-27 11:07:08
 * @Entity cn.mars.marsprinterhub.model.entity.SpoolUsedSnapshots
 */
public interface SpoolUsedSnapshotsMapper extends BaseMapper<SpoolUsedSnapshots> {

    /**
     * 批量插入快照 (忽略重复键冲突)
     *
     * @param list 快照列表
     */
    void insertBatchIgnore(@Param("list") List<SpoolUsedSnapshots> list);

}




