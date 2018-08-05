package com.yun9.service.tax.core;


import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.dto.BillDTO;
import com.yun9.service.tax.core.dto.BizTaxBusinessSheetDTO;
import com.yun9.service.tax.core.dto.BizTaxVatImportSheetDTO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: service-tax
 * @description: ${description}
 * @author: leigm
 * @create: 2018-05-19 16:43
 **/
public interface TaxInstanceCategoryPersonalBusinessFactory {


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
     * 解析excel
     *
     * @param bizTaxVatImportSheetDTO 参数
     */
    Object parseVatExcel(BizTaxVatImportSheetDTO bizTaxVatImportSheetDTO);

    /**
     * 下载excel
     */
    void downloadExcel(HttpServletRequest request, HttpServletResponse response, List<Long> categoryBussinessIds);
    void save(BizTaxBusinessSheetDTO bizTaxBusinessSheetDTO, long id, int state, long userId);

}
