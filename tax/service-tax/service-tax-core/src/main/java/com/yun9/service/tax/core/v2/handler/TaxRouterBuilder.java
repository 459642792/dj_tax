package com.yun9.service.tax.core.v2.handler;

import com.yun9.service.tax.core.taxrouter.request.TaxRouterRequest;
import com.yun9.service.tax.core.v2.OperationContext;

public interface TaxRouterBuilder {

    TaxRouterRequest build(OperationContext context);

}
