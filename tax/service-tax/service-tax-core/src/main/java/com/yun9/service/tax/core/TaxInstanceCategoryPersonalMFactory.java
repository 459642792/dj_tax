package com.yun9.service.tax.core;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayroll;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalPayrollItem;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.dto.BizTaxInstanceCategoryPersonalPayrollItemStateDTO;
import com.yun9.service.tax.core.dto.BizTaxPersonalImportDTO;
import com.yun9.service.tax.core.dto.BizTaxPersonalImportSheetDTO;
import com.yun9.service.tax.core.dto.PersonalHistoryPayrollDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 个税工资薪金月报
 *
 * @Author: chenbin
 * @Date: 2018-05-31
 * @Time: 16:02
 * @Description:
 */
public interface TaxInstanceCategoryPersonalMFactory {
    /**
     * 获取申报列表
     *
     * @param instClientId         机构客户id
     * @param mdCompanyId           公司id
     * @param areaId               地区id
     * @param mdAccountCycleIdList 会计期间id
     * @return
     */
    List<PersonalHistoryPayrollDTO> getHistoryList(Long instClientId, long mdCompanyId, long areaId, long[] mdAccountCycleIdList);

    /**
     * 解析excel
     */
    BizTaxPersonalImportDTO parsePersonalExcel(BizTaxPersonalImportSheetDTO bizTaxPersonalImportSheetDTO);

    /**
     * 获取人员清单
     *
     * @param payrollId
     * @return
     */
    BizTaxPersonalImportDTO getHistoryItems(Long payrollId);

    /**
     * 审核状态更改
     */
    void audit(long taxInstanceCategoryPayrollId, long userId, String action);

    void cancleAudit(long taxInstanceCategoryPayrollId,long userId,String action);

    /**
     * 修改申报方式
     *
     * @param instanceCategoryPersonalPayrollId
     * @return
     */
    boolean updateSourceType(long instanceCategoryPersonalPayrollId, BizTaxInstanceCategoryPersonalPayroll.SourceType sourceType, long userId);


    /**
     * 人员清单新增
     * @param bizTaxInstanceCategoryPersonalPayrollItemList
     * @return
     */
    BizTaxInstanceCategoryPersonalPayrollItemStateDTO addPersonalPayrollItem(List<BizTaxInstanceCategoryPersonalPayrollItem> bizTaxInstanceCategoryPersonalPayrollItemList,long categoryId,boolean isDelete);

    /**
     * 修改人员清单
     * @param bizTaxInstanceCategoryPersonalPayrollItem
     * @return
     */
    BizTaxInstanceCategoryPersonalPayrollItemStateDTO updatePersonalPayrollItem(BizTaxInstanceCategoryPersonalPayrollItem bizTaxInstanceCategoryPersonalPayrollItem);

    /**
     * 批量删除人员信息
     * @param ids
     */
    Object deletePersonalPayrollItem(List<Long> ids);

    /**
     * 按条件查询
     * @param page
     * @param limit
     * @param params
     * @return
     */
    Object getPersonalPayrollItem(int page, int limit,Map<String, Object> params);


    /**
     * 人员清单确认
     * @param categoryPersonalPayrollId
     * @return
     */
    Object confirmPersonalInfo(long categoryPersonalPayrollId);



    /**
     * 从税局下载历史人员校验
     * @param taxInstanceCategoryId
     * @return
     */
    Map<String,Object> downItemFromTaxCheck(long taxInstanceCategoryId);
}
