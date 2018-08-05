package com.yun9.service.tax.core;


import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryYhItem;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.dto.BizTaxYhItemDTO;
import com.yun9.service.tax.core.dto.BizTaxInstanceCategoryYhItemDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: service-tax
 * @description: ${description}
 * @author: leigm
 * @create: 2018-05-19 16:43
 **/
public interface TaxInstanceCategoryYhFactory {


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

    void confirmed(BizTaxYhItemDTO bizTaxYhItemDTO,long userId);
    
    void unconfirmed(long bizTaxInstanceCategoryYhId,long userId);
    BizTaxInstanceCategoryYhItemDTO addYhItem(List<BizTaxInstanceCategoryYhItem> yhItemList);

    BizTaxInstanceCategoryYhItemDTO vaild(BizTaxInstanceCategoryYhItem yhItem);

    Object getYhItem(int page, int limit, Map<String, Object> params);

    Map<String,Object> getVat(long bizTaxInstanceCategoryYhId);

    void batchAudit(List<Long> ids, long userId);

}
