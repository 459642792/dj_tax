package com.yun9.service.tax.core.ft;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.commons.exception.BizException;
import com.yun9.service.tax.core.ft.handler.AuditHandler;
import com.yun9.service.tax.core.task.helper.JsonMap;

/**
 * Created by werewolf on  2018/6/5.
 * 所有税种统一操作业务[根据税种sn操作]
 */
public interface TaxMdCategoryHandler {

    interface AuditCallback {
        void success();

        void exception(BizException ex);
    }

    AuditHandler auditHandler(TaxSn taxSn);


    /**
     * 审核税种实列
     *
     * @param bizTaxInstanceCategoryId
     * @param processBy
     * @param auditCallback
     */
    void audit(long bizTaxInstanceCategoryId, long processBy, AuditCallback auditCallback);

    void audit(BizTaxInstanceCategory bizTaxInstanceCategory, long processBy, AuditCallback auditCallback);

    void getTaxCategoriesTaskToAudit(BizTaxInstanceCategory bizTaxInstanceCategory, JsonMap jsonMap, long processBy, AuditCallback auditCallback);

    /**
     * 取消审核
     *
     * @param bizTaxInstanceCategoryId
     * @param processBy
     */
    void cancelOfAudit(long bizTaxInstanceCategoryId, long processBy);


}
