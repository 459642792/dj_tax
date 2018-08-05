package com.yun9.service.tax.core.task.callback.handler;

import com.yun9.biz.tax.BizTaxInstanceCategoryDeductService;
import com.yun9.biz.tax.ops.BizSynScreenShotService;
import com.yun9.service.tax.core.task.annotation.TaskSnMapping;
import com.yun9.service.tax.core.task.callback.TaskCallBackContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-05-28 16:33
 */

@TaskSnMapping(sns = {"SZ0037","SZ0047"})
public class SynScreenShotCallback extends AbstractCallbackHandlerMapping {

    @Autowired
    BizSynScreenShotService bizSynScreenShotService;
    @Autowired
    BizTaxInstanceCategoryDeductService bizTaxInstanceCategoryDeductService;

    @Override
    public TaskCallBackContext process(TaskCallBackContext context) {
        bizSynScreenShotService.completeSynScreenShot(context.getBizTaxInstanceSeq(), context.getBody(), null);
        return context;
    }
}
