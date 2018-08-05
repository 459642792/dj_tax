package com.yun9.service.tax.core;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryDeduct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public interface TaxInstanceCategoryDeductFactory {
    /**
     * 获取列表
     *
     * @param instanceCategoryId 税种ID
     * @param params             参数集合
     * @return
     */
    List<HashMap> listByInstanceCategoryIdAndParams(long instanceCategoryId, Map<String, Object> params);

    /**
     * 撤销二维码
     *
     * @param id 扣款凭证id
     */
    void cancel(long id, long processBy);
}
