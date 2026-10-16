package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.dto.record.RecordQueryDTO;
import cn.mars.marsprinterhub.model.entity.Spool;
import cn.mars.marsprinterhub.model.entity.SpoolUsedRecord;
import cn.mars.marsprinterhub.model.vo.SpoolUsedRecordHomeSummaryVO;
import cn.mars.marsprinterhub.model.vo.SpoolUsedRecordVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 针对表【spool_used_records(耗材使用记录)】的数据库操作Service
 */
public interface SpoolUsedRecordService extends IService<SpoolUsedRecord> {

    /**
     * 获取耗材使用记录分页
     *
     * @param queryDTO 查询参数
     * @return 使用记录分页
     */
    IPage<SpoolUsedRecord> getRecordPage(RecordQueryDTO queryDTO);

    /**
     * 获取耗材使用记录
     *
     * @param id 使用记录 id
     * @return 使用记录
     */
    SpoolUsedRecord getBySpoolUsedRecordId(Long id);

    /**
     * 获取耗材使用记录
     *
     * @param spoolId 耗材 id
     * @return 使用记录
     */
    List<SpoolUsedRecord> listSpoolUsedRecordsBySpoolId(Long spoolId);

    /**
     * 获取耗材使用记录
     *
     * @param orderId 订单 id
     * @return 使用记录
     */
    List<SpoolUsedRecord> listSpoolUsedRecordsByOrderId(Long orderId);

    /**
     * 获取最近 10 条耗材使用记录
     *
     * @return 使用记录
     */
    List<SpoolUsedRecord> getLatest10Records();

    /**
     * 耗材转移
     *
     * @param id         使用记录 id
     * @param newSpoolId 新的耗材 id
     */
    void transferSpoolUsedRecord(Long id, Long newSpoolId);

    /**
     * 删除耗材使用记录
     *
     * @param spoolId 耗材id
     */
    void removeBySpoolId(Long spoolId);

    /**
     * 转换为 VO
     *
     * @param spoolUsedRecord 使用记录
     * @return 使用记录VO
     */
    SpoolUsedRecordVO convertToVO(SpoolUsedRecord spoolUsedRecord, boolean getSpoolVO);

    /**
     * 批量转换为 VO
     *
     * @param spoolUsedRecords 使用记录列表
     * @return 使用记录VO列表
     */
    List<SpoolUsedRecordVO> convertToVOList(List<SpoolUsedRecord> spoolUsedRecords, boolean getSpoolVO);

    /**
     * 转换为首页使用记录VO
     *
     * @param spoolUsedRecord 使用记录
     * @return 使用记录VO
     */
    SpoolUsedRecordHomeSummaryVO convertToHomeSummaryVO(SpoolUsedRecord spoolUsedRecord);

    /**
     * 获取本周使用记录
     *
     * @return 使用记录
     */
    List<SpoolUsedRecord> getWeekUsedRecords();

    /**
     * 绑定订单使用记录
     *
     * @param orderId  订单 id
     * @param recordId 使用记录 id
     */
    void bindOrderRecord(Long orderId, Long recordId);


    /**
     * 解绑订单使用记录
     *
     * @param orderId  订单 id
     * @param recordId 使用记录 id
     */
    void unbindOrderRecord(Long orderId, Long recordId);

    /**
     * 删除耗材使用记录
     *
     * @param id 使用记录 id
     * @return 是否成功
     */
    boolean deleteSpoolUsedRecord(Long id);

    /**
     * 去除重复记录
     *
     * @param spoolUsedRecords 使用记录
     * @return 去除重复记录
     */
    List<SpoolUsedRecord> distinctByTime(List<SpoolUsedRecord> spoolUsedRecords);

    /**
     * 根据订单 id 更新使用记录
     *
     * @param orderId 订单 id
     */
    void updateByOrderId(Long orderId);

    /**
     * 根据任务ID和AMS引用获取记录
     *
     * @param taskId 任务ID
     * @param amsRef AMS引用
     * @return 记录
     */
    SpoolUsedRecord getByTaskIdAndAmsRef(Long taskId, Integer amsRef);

    /**
     * 计算并填充记录成本
     *
     * @param record 使用记录
     * @return 成本
     */
    BigDecimal fillCost(SpoolUsedRecord record);

    /**
     * 创建一条简单的耗材使用记录
     */
    void createSimpleRecord(Spool spool, BigDecimal usedWeight, String title, Date startTime, Date endTime);
}
