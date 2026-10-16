package cn.mars.marsprinterhub.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 耗材使用快照
 * @TableName spool_used_snapshots
 */
@TableName(value ="spool_used_snapshots")
@Data
public class SpoolUsedSnapshots {
    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联打印记录 ID
     */
    private Long recordId;

    /**
     * 进度百分比
     */
    private Integer percent;

    /**
     * 快照图片 URL
     */
    private String imageUrl;

    /**
     * 创建时间
     */
    private Date createTime;
}