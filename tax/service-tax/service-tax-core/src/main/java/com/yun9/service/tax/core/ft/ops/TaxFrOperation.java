package com.yun9.service.tax.core.ft.ops;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.service.tax.core.ft.TaxCategoryMapping;
import com.yun9.service.tax.core.ft.handler.AuditHandler;

/**
 * Created by werewolf on  2018/6/5.
 */
@TaxCategoryMapping(sn = {TaxSn.y_fr})
public class TaxFrOperation implements AuditHandler {

    @Override
    public boolean isNeedAudit(BizTaxInstanceCategory bizTaxInstanceCategory, BizTaxMdCategory bizTaxMdCategory) {
        return true;
    }

    @Override
    public void audit(BizTaxInstanceCategory bizTaxInstanceCategory) {
        return;//nothing
    }

    @Override
    public void cancelOfAudit(BizTaxInstanceCategory bizTaxInstanceCategory) {

    }


}
