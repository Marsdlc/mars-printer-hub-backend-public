package cn.mars.marsprinterhub.service;

import cn.mars.marsprinterhub.model.dto.spool.SpoolQueryDTO;
import cn.mars.marsprinterhub.model.entity.Spool;
import cn.mars.marsprinterhub.model.vo.SpoolHomeSummaryVO;
import cn.mars.marsprinterhub.model.vo.SpoolQueryOptionsVO;
import cn.mars.marsprinterhub.model.vo.SpoolSummaryVO;
import cn.mars.marsprinterhub.model.vo.SpoolVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 针对表【spools(耗材料盘表)】的数据库操作Service
 */
public interface SpoolService extends IService<Spool> {

    /**
     * 创建耗材
     *
     * @param spool 耗材信息
     * @return 创建的耗材
     */
    Spool createSpool(Spool spool, int num);

    /**
     * 更新耗材信息
     *
     * @param spool 耗材信息
     * @return 更新后的耗材
     */
    Spool updateSpool(Spool spool);

    /**
     * 根据ID删除耗材
     *
     * @param id 耗材ID
     * @return 是否删除成功
     */
    boolean deleteSpool(Long id);

    /**
     * 根据ID获取耗材详情
     *
     * @param id 耗材ID
     * @return 耗材信息
     */
    Spool getSpoolById(Long id);

    /**
     * 根据ID获取耗材详情（不登录）
     *
     * @param id 耗材ID
     * @return 耗材信息
     */
    Spool getSpoolByIdNotLogin(Long id);

    /**
     * 分页查询耗材列表
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<Spool> getSpoolPage(SpoolQueryDTO queryDTO);

    /**
     * 使用耗材
     *
     * @param spoolId    耗材ID
     * @param usedWeight 使用重量
     * @return 更新后的耗材
     */
    Spool useSpool(Long spoolId, BigDecimal usedWeight, String title);

    /**
     * 校准耗材
     *
     * @param spoolId    耗材ID
     * @param usedWeight 使用重量
     * @return 校准后的耗材
     */
    Spool checkSpool(Long spoolId, BigDecimal usedWeight);

    /**
     * 将Spool实体转换为SpoolVO
     *
     * @param spool 耗材实体
     * @return 耗材VO
     */
    SpoolVO convertToVO(Spool spool);

    /**
     * 将Spool实体转换为SpoolHomeSummaryVO
     *
     * @param spoolVO 耗材实体
     * @return 耗材HomeSummaryVO
     */
    SpoolHomeSummaryVO convertToHomeSummaryVO(SpoolVO spoolVO);

    /**
     * 获取耗材汇总信息
     *
     * @return 汇总信息
     */
    SpoolSummaryVO getSpoolSummary(boolean excludeFinished, String brand, String materialType);

    /**
     * 获取所有耗材列表
     *
     * @return 耗材列表
     */
    List<Spool> getAllSpools();

    /**
     * 获取搜索栏下拉选项
     *
     * @return 下拉选项
     */
    SpoolQueryOptionsVO getOptions();

    /**
     * 获取最近5条耗材列表
     *
     * @return 耗材列表
     */
    List<Spool> getLatest5Spools();

    /**
     * 获取剩余百分比小于10的耗材列表
     *
     * @return 耗材列表
     */
    List<Spool> getSpoolsByRemainingPercentageLessThan10();

    /**
     * 根据ID获取耗材名称
     *
     * @param id 耗材ID
     * @return 耗材名称
     */
    String getSpoolNameById(Long id);

}
