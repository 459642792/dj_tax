package com.yun9.service.tax.core.v2.handler;

import com.yun9.service.tax.core.taxrouter.request.TaxRouterRequest;
import com.yun9.service.tax.core.v2.OperationContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-05-25 16:27
 */

public abstract class AbstractTaxRouterBuilder implements TaxRouterBuilder {

    @Autowired
    private TaxRouterLoginHandler taxRouterLoginHandler;


    protected abstract Map<String, Object> buildParams(OperationContext context);

    protected TaxRouterRequest defaultAccountAndParams(OperationContext context) {
        return this.build(context);
    }

    @Override
    public TaxRouterRequest build(OperationContext context) {
        return new TaxRouterRequest() {{
            setLoginInfo(
                    taxRouterLoginHandler.getCompanyDefaultAccount(context.getBizMdCompany(),
                            context.getRequest().getTaxOffice().name(),
                            context.getBizMdArea().getSn(),
                            context.getRequest().getActionSn())
            );
            setParams(buildParams(context));

        }};
    }
}
