package com.yun9.service.tax.core.event;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.service.tax.core.task.helper.JsonMap;

/**
 * Created by werewolf on  2018/6/12.
 */

public class TaskDownloadComplete extends ServiceTaxEvent {

    private BizTaxInstanceCategory bizTaxInstanceCategory;
    private JsonMap body;

    public BizTaxInstanceCategory getBizTaxInstanceCategory() {
        return bizTaxInstanceCategory;
    }

    public TaskDownloadComplete setBizTaxInstanceCategory(BizTaxInstanceCategory bizTaxInstanceCategory) {
        this.bizTaxInstanceCategory = bizTaxInstanceCategory;
        return this;
    }

    public JsonMap getBody() {
        return body;
    }

    public TaskDownloadComplete setBody(JsonMap body) {
        this.body = body;
        return this;
    }
}
