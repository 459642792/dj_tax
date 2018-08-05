package com.yun9.service.tax.core;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayrollItem;
import com.yun9.service.tax.core.dto.BizTaxInstanceCategoryPersonalPayrollItemStateDTO;

import java.util.List;

/**
 * @author yunjie
 * @version 1.0
 * @since 2018-05-30 16:53
 */
public interface TaxInstanceCategoryPersonalPayrollItemFactory {
    /**
     * 校验个税人员数据
     *
     * @return
     */
    BizTaxInstanceCategoryPersonalPayrollItemStateDTO vaild (BizTaxInstanceCategoryPersonalPayrollItem personalItem, long mdCompanyId, long mdInstClientId);

    BizTaxInstanceCategoryPersonalPayrollItem calculate(BizTaxInstanceCategoryPersonalPayrollItem personalItem);

    /**
     * 初始化数据
     *
     * @return
     */
    BizTaxInstanceCategoryPersonalPayrollItem initialize (BizTaxInstanceCategoryPersonalPayrollItem personalItem, long mdInstClientId, long mdCompanyId, long mdAccountCycleId, long md_area_id);

    /**
     * 批量增加(会调用删除以前老数据后新增)
     * @param mdInstClientId
     * @param mdCompanyId
     * @param mdAccountCycleId
     * @param mdAreaId
     * @param instanceCategoryPersonalPayrollId
     * @param itemList
     */
    void bacthCreate(long mdInstClientId,long mdCompanyId,long mdAccountCycleId,long mdAreaId,long instanceCategoryPersonalPayrollId,List<BizTaxInstanceCategoryPersonalPayrollItem> itemList);
}