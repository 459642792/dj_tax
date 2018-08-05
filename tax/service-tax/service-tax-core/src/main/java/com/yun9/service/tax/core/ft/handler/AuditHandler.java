package com.yun9.service.tax.core.ft.handler;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;

/**
 * Created by werewolf on  2018/6/5.
 */
public interface AuditHandler {

    boolean isNeedAudit(BizTaxInstanceCategory bizTaxInstanceCategory, BizTaxMdCategory bizTaxMdCategory);

    /**
     * 审核税种实列
     *
     * @param bizTaxInstanceCategory
     */
    void audit(BizTaxInstanceCategory bizTaxInstanceCategory);


    /**
     * 取消审核
     *
     * @param bizTaxInstanceCategory
     */
    void cancelOfAudit(BizTaxInstanceCategory bizTaxInstanceCategory);
}
