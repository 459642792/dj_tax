package com.yun9.service.tax.core;

import com.yun9.biz.tax.domain.entity.BizTaxNoticeInstRecord;

/**
 * 机构申报通知
 */
public interface TaxNoticeInstFactory {

    BizTaxNoticeInstRecord noticeByBizTaxInstanceCategoryId(long bizTaxInstanceCategoryId);
}
