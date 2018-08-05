package com.yun9.service.tax.core;


import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryBit;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.dto.BizTaxBitImportDTO;
import com.yun9.service.tax.core.dto.BizTaxBitImportSheetDTO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: service-tax
 * @description: ${description}
 * @author: leigm
 * @create: 2018-05-19 16:43
 **/
public interface TaxInstanceCategoryBitFactory {


    /**
     * 根据状态获取分页查询
     *
     * @param accountCycleIds 会计区间ID
     * @param orgTreeId       组织ID
     * @param state           状态
     * @param page            页数
     * @param limit           每页条数
     * @param params          过滤参数
     * @return
     */
    Pagination<HashMap> pageByState(List<Long> accountCycleIds, long orgTreeId, BizTaxInstanceCategory.State state, int page, int limit, Map<String, Object> params);

    /**
     * 下载excel模板
     *
     * @param request
     * @param response
     * @param categoryIds
     * @param mdAccountCycleId
     */
    void downloadExcel(HttpServletRequest request, HttpServletResponse response, List<Long> categoryIds, long mdAccountCycleId);


    /**
     * 解析excel
     */
    BizTaxBitImportDTO parseBitExcel(BizTaxBitImportSheetDTO importSheetDTO);

    void profitAccounting(BizTaxInstanceCategoryBit bizTaxInstanceCategoryBit,long userID,String remark)throws IllegalAccessException;

    void cancelAudit(long id,long userID,String remark);

    BigDecimal getSaleAmountByVat(long id, long userID);

    int getEmployeeNumberByPersonal(long id, long userID);

    void saveProfit(BizTaxInstanceCategoryBit bizTaxInstanceCategoryBit,long userID,String remark)throws IllegalAccessException;

    void batchAudit(List<Long> ids,long userID);
}
