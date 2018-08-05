package com.yun9.service.tax.core.task.callback.handler;

import com.yun9.biz.tax.ops.BizDeductService;
import com.yun9.service.tax.core.task.annotation.TaskSnMapping;
import com.yun9.service.tax.core.task.callback.TaskCallBackContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@TaskSnMapping(sns = {"SZ0012"})
public class FindCompanyDeductCallBack extends AbstractCallbackHandlerMapping {

    private static final Logger logger = LoggerFactory.getLogger(FindCompanyDeductCallBack.class);

    @Autowired
    BizDeductService bizDeductService;

    @Autowired
    EventService eventService;

    @Override
    public TaskCallBackContext process(TaskCallBackContext context) {
        bizDeductService.completeDeduct(context.getBizTaxInstanceSeq(), context.getBody());
        logger.info("回调逻辑执行完成-------------");
        return context;

    }
}
