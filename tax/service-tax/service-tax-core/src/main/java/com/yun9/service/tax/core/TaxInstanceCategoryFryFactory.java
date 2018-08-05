package com.yun9.service.tax.core;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.framework.orm.commons.criteria.Pagination;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface TaxInstanceCategoryFryFactory {

    /**
     * @param accountCycleId
     * @param orgTreeId
     * @param params
     * @return
     * @Description: 统计状态
     * @author leigm
     * @date 2018/5/3
     */
//    HashMap totalByState(long accountCycleId, long orgTreeId, Map<String, Object> params);


    /**
     * @param accountCycleIds
     * @param orgTreeId
     * @param state
     * @param page
     * @param limit
     * @param params
     * @return
     * @Description: 获取分页列表
     * @author leigm
     * @date 2018/5/3
     */
    Pagination<HashMap> pageByState(List<Long> accountCycleIds, long orgTreeId, BizTaxInstanceCategory.State state, int page, int limit, Map<String, Object> params);

}
