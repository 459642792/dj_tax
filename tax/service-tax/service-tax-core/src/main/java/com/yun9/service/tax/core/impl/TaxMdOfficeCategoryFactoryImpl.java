package com.yun9.service.tax.core.impl;

import com.yun9.biz.md.BizMdCompanyService;
import com.yun9.biz.md.domain.entity.BizMdCompany;
import com.yun9.biz.tax.BizTaxMdOfficeCategoryService;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategory;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.service.tax.core.TaxMdOfficeCategoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by zhengzb on 2018/6/13.
 */
@Component
public class TaxMdOfficeCategoryFactoryImpl implements TaxMdOfficeCategoryFactory {
    @Autowired
    BizMdCompanyService bizMdCompanyService;
    @Autowired
    BizTaxMdOfficeCategoryService bizTaxMdOfficeCategoryService;
    @Override
    public List list(long companyId, TaxOffice taxOffice) {
        BizMdCompany bizMdCompany = bizMdCompanyService.findById(companyId);
        if(null == bizMdCompany){
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException,"公司不存在");
        }
        List<BizTaxMdOfficeCategory> bizTaxMdOfficeCategories = bizTaxMdOfficeCategoryService.findByTaxOfficeAndAreaId(taxOffice,bizMdCompany.getTaxAreaId());
        if(CollectionUtils.isEmpty(bizTaxMdOfficeCategories)){
            return new ArrayList<>();
        }
        return new ArrayList(){{
            bizTaxMdOfficeCategories.forEach(v->{
                add(new HashMap(){{
                    put("id",v.getId());
                    put("name",v.getRemark());
                }});
            });
        }};
    }
}
