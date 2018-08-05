package com.yun9.service.tax.core.v2.handler.impl;

import com.yun9.biz.tax.BizTaxMdOfficeCategoryService;
import com.yun9.biz.tax.BizTaxPropertiesService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.properties.BizTaxProperties;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.exception.ServiceTaxException.Codes;
import com.yun9.service.tax.core.v2.handler.TaxRouterTaxCategoryHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-05-28 13:02
 */
@Component
public class TaxRouterTaxCategoryHelperImpl implements TaxRouterTaxCategoryHelper {

    @Autowired
    BizTaxMdOfficeCategoryService bizTaxMdOfficeCategoryService;
    @Autowired
    BizTaxPropertiesService bizTaxPropertiesService;


    @Override
    public String findTaxCodeByBizTaxMdOfficeCategoryId(long bizTaxMdOfficeCategoryId) {
        return Optional.ofNullable(bizTaxMdOfficeCategoryService.findById(bizTaxMdOfficeCategoryId))
                .orElseThrow(
                        () -> ServiceTaxException.build(Codes.tax_router_config_not_found, "taxCode相关"))
                .getCode();
    }


    @Override
    public boolean isInDeclareRangeTime(BizTaxInstanceCategory bizTaxInstanceCategory) {
        BizTaxProperties bizTaxProperties = bizTaxPropertiesService.findByKey(BizTaxProperties.OUT_RANGE_TIME_DELCARE);

        boolean condition1 = true;
        if (bizTaxProperties != null) {
            condition1 = Objects.equals(bizTaxProperties.getValue(), "Y");
        }
        return condition1 && bizTaxInstanceCategory.inDeclareRangeTime();
    }
}
