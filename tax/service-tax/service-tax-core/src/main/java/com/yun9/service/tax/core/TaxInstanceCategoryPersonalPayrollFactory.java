package com.yun9.service.tax.core;


import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.framework.orm.commons.criteria.Pagination;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: service-tax
 * @description: ${description}
 * @author: leigm
 * @create: 2018-05-19 16:43
 **/
public interface TaxInstanceCategoryPersonalPayrollFactory {


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
     * 修改收入所属期id
     *
     * @param id             个税工资薪金ID
     * @param accountCycleId 会计区间id
     * @param processBy      操作人
     */
    void changeIncomeAccountCycle(long id, long accountCycleId, long processBy);
}
