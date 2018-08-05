package com.yun9.service.tax.core;

import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.enums.TaxSn;

import java.util.HashMap;
import java.util.List;

/**
 * Created by zhengzb on 2018/6/13.
 */
public interface TaxCompanyCategoryFactory {
    /**
     * 绑定公司税种信息
     * @param companyId
     * @param taxOfficeCategoryId
     */
    void create(long companyId,long taxOfficeCategoryId);

    /**
     * 查询公司绑定税种列表
     * @param companyId
     * @param taxOffice
     * @return
     */
    Object list(long companyId, TaxOffice taxOffice);

    /**
     * 统计包含税种的客户数量
     * @param orgTreeId
     * @param params
     * @return
     */
    Object countContainTaxSns(long orgTreeId, HashMap<String,Object> params);


}
