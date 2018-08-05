package com.yun9.service.tax.core.task.callback.handler;

import com.yun9.biz.tax.BizTaxInstanceCategoryDeductService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.ops.BizDeductService;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.exception.ServiceTaxException.Codes;
import com.yun9.service.tax.core.task.annotation.TaskSnMapping;
import com.yun9.service.tax.core.task.callback.TaskCallBackContext;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-05-28 15:10
 */
@TaskSnMapping(sns = {"SZ0038","SZ0048"})
public class SendDeductCallback extends AbstractCallbackHandlerMapping {

    @Autowired
    BizDeductService bizDeductService;
    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;
    @Autowired
    BizTaxInstanceCategoryDeductService bizTaxInstanceCategoryDeductService;

    @Override
    public TaskCallBackContext process(TaskCallBackContext context) {
        Optional
            .ofNullable(bizTaxInstanceCategoryService
                .findById(context.getBizTaxInstanceSeq().getBizTaxInstanceCategoryId()))
            .orElseThrow(
                () -> ServiceTaxException.build(Codes.IllegalArgumentException, "税种申报实例未找到"));

        bizDeductService.completeDeduct(context.getBizTaxInstanceSeq(), context.getBody(),null);
        return context;
    }
}
