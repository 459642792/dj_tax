package com.yun9.service.tax.core.report;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.service.tax.core.task.helper.JsonMap;

/**
 * Created by werewolf on  2018/6/1.
 */
public interface ReportFactory {

    String PARAM_REPORT_ITEM_ID = "reportItemId";

    /**
     * 生成报表接口
     *
     * @param bizTaxInstanceCategory 税种实例category
     * @param body                   税务路由返回参数[根据税种判断是否可选]
     */
    void generate(BizTaxInstanceCategory bizTaxInstanceCategory, JsonMap body);
}
