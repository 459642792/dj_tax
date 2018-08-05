package com.yun9.service.tax.core.event;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.service.tax.core.task.helper.JsonMap;

/**
 * Created by werewolf on  2018/6/13.
 */

public class TaxInstanceCategoryAuditBeforeEvent extends ServiceTaxEvent {

    private BizTaxInstanceCategory bizTaxInstanceCategory;

    private long processBy;

    private JsonMap body;

    public BizTaxInstanceCategory getBizTaxInstanceCategory() {
        return bizTaxInstanceCategory;
    }

    public TaxInstanceCategoryAuditBeforeEvent setBizTaxInstanceCategory(BizTaxInstanceCategory bizTaxInstanceCategory) {
        this.bizTaxInstanceCategory = bizTaxInstanceCategory;
        return this;
    }

    public long getProcessBy() {
        return processBy;
    }

    public TaxInstanceCategoryAuditBeforeEvent setProcessBy(long processBy) {
        this.processBy = processBy;
        return this;
    }

    public JsonMap getBody() {
        return body;
    }

    public TaxInstanceCategoryAuditBeforeEvent setBody(JsonMap body) {
        this.body = body;
        return this;
    }
}
