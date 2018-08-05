package com.yun9.service.tax.core;

import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategory;
import com.yun9.biz.tax.enums.TaxOffice;

import java.util.List;

/**
 * Created by zhengzb on 2018/6/13.
 */
public interface TaxMdOfficeCategoryFactory {

    /**
     * 查询所有税局税区支持的税种列表
     * @param companyId
     * @param taxOffice
     * @return
     */
    List list(long companyId, TaxOffice taxOffice);

}
