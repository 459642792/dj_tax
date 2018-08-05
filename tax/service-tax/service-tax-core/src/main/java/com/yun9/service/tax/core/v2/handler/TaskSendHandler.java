package com.yun9.service.tax.core.v2.handler;

import com.yun9.service.tax.core.taxrouter.request.TaxRouterRequest;
import com.yun9.service.tax.core.v2.OperationContext;

/**
 * Created by werewolf on  2018/5/22.
 */

public interface TaskSendHandler {


    void send(final OperationContext context, final TaxRouterRequest taxRouterRequest);

   // TaskBO sendToTask(final OperationContext context, final TaxRouterRequest taxRouterRequest);

    /**
     * 发起任务，并把seq置为process状态
     *
     * @param context
     * @param taxRouterRequest
     */
    void processTask(final OperationContext context, final TaxRouterRequest taxRouterRequest);
}
