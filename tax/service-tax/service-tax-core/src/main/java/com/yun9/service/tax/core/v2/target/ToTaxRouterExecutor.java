package com.yun9.service.tax.core.v2.target;

import com.yun9.service.tax.core.taxrouter.request.TaxRouterRequest;
import com.yun9.service.tax.core.v2.ITargetExecutor;
import com.yun9.service.tax.core.v2.OperationContext;
import com.yun9.service.tax.core.v2.handler.TaxRouterBuilder;
import org.springframework.stereotype.Component;

/**
 * 税务路由操作
 */
@Component("target_tax_router")
public class ToTaxRouterExecutor extends AbstractExecutor implements ITargetExecutor {


    @Override
    public Object execute(OperationContext context) {

//        TaxRouterRequest taxRouterRequest = taxRouterBuilder.build(context);
//
//       Object object =  post(taxRouterRequest);
//
        return null;
    }
}
