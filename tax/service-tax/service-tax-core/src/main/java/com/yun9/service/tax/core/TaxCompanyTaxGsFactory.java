package com.yun9.service.tax.core;

import com.yun9.framework.orm.commons.criteria.Pagination;

import java.util.HashMap;

/**
 * Created by zhengzb on 2018/6/11.
 */
public interface TaxCompanyTaxGsFactory {

    /**
     * 查询国税信息列表
     * @param orgTreeId
     * @param page
     * @param limit
     * @param params
     * @return
     */
    Pagination list(long orgTreeId, int page, int limit, HashMap<String,Object>params);

    /**
     * 查询公司国税信息
     * @param companyId
     * @return
     */
    HashMap find(long companyId);
}
