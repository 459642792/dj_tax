package com.yun9.service.tax.core.task.callback.handler;

import com.yun9.biz.tax.ops.BizTaxDeclareService;
import com.yun9.service.tax.core.task.annotation.TaskSnMapping;
import com.yun9.service.tax.core.task.callback.TaskCallBackContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-05-22 15:20
 */
@TaskSnMapping(sns = {"SZ0023"})
public class SendPersonalBusinessCallback extends AbstractCallbackHandlerMapping {


    @Autowired
    BizTaxDeclareService bizTaxDeclareService;

    @Autowired
    EventService eventService;


    @Override
    public TaskCallBackContext process(TaskCallBackContext context) {
        bizTaxDeclareService
                .completeDeclare(context.getBizTaxInstanceSeq().getBizTaxInstanceCategoryId(),
                        context.getBizTaxInstanceSeq().getSeq(), context.getBody(), null);
        eventService.handleSingleTaxDeclaredEventAsync(context.getBizTaxInstanceSeq().getBizTaxInstanceCategoryId(), context.getBizTaxInstanceSeq().getCreatedBy());
        return context;
    }
}
