package com.yun9.service.tax.core.task.callback.handler;

import com.yun9.biz.tax.ops.BizTaxDeclareService;
import com.yun9.service.tax.core.task.annotation.TaskSnMapping;
import com.yun9.service.tax.core.task.callback.TaskCallBackContext;
import com.yun9.service.tax.core.task.callback.TaskCallBackHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by werewolf on  2018/4/23. 财务报表申报回调 SZ_SEND_GFRQ("SZ0020", "财务报表-一般企业-季报"),
 * SZ_SEND_SFRQ("SZ0021", "财务报表-小企业-季报"),
 */


@TaskSnMapping(sns = {"SZ0035", "SZ0049"})
public class SendFrCallback extends AbstractCallbackHandlerMapping implements TaskCallBackHandler {

    @Autowired
    BizTaxDeclareService bizTaxDeclareService;

    @Override
    public TaskCallBackContext process(TaskCallBackContext context) {
        bizTaxDeclareService
            .completeDeclare(context.getBizTaxInstanceSeq().getBizTaxInstanceCategoryId(),
                context.getBizTaxInstanceSeq().getSeq(), context.getBody(), null);
        return context;

    }


}
