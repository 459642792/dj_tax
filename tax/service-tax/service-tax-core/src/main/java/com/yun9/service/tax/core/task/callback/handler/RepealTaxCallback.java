package com.yun9.service.tax.core.task.callback.handler;

import com.yun9.biz.tax.ops.BizRepealTaxService;
import com.yun9.service.tax.core.task.annotation.TaskSnMapping;
import com.yun9.service.tax.core.task.callback.TaskCallBackContext;
import com.yun9.service.tax.core.task.callback.TaskCallBackHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-05-23 12:00
 */
@TaskSnMapping(sns = {"SZ0036","SZ0046"})
public class RepealTaxCallback implements TaskCallBackHandler {

    @Autowired
    BizRepealTaxService bizRepealTaxService;

    @Override
    public TaskCallBackContext process(TaskCallBackContext context) {

        bizRepealTaxService.completeRepeal(context.getBizTaxInstanceSeq(), context.getBody(), null);
        return context;
    }
}
