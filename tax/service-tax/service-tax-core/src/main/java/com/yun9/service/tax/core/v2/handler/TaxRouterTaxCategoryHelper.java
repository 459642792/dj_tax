package com.yun9.service.tax.core.v2.handler;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-05-28 13:00
 */

public interface TaxRouterTaxCategoryHelper {

    /**
     *通过 bizTaxInstanceCategoryId找到对应税务路由对应的taxCode
     * @param bizTaxMdOfficeCategoryId
     * @return
     */
    String findTaxCodeByBizTaxMdOfficeCategoryId(long bizTaxMdOfficeCategoryId);

    /**
     *  是否在申报期
     * @param bizTaxInstanceCategory
     * @return
     */
    boolean isInDeclareRangeTime(BizTaxInstanceCategory bizTaxInstanceCategory);

}
