package com.yun9.service.tax.core.v2.handler;

import com.yun9.biz.task.domain.bo.TaskBO;
import com.yun9.service.tax.core.v2.OperationContext;

import java.util.Map;


public interface TaskStartHandler {


    void begin(OperationContext context);

    void success(OperationContext context);

    /**
     * 发起任务产生异常
     *
     * @param context 实列ID
     * @param errors  多个错误
     */
    void exception(OperationContext context, Map<Integer, String> errors);
}
