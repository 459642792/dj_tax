package com.yun9.service.tax.core.v2.target;

import com.yun9.service.tax.core.v2.ITargetExecutor;
import com.yun9.service.tax.core.v2.OperationContext;
import org.springframework.stereotype.Component;

/**
 * ft操作
 */
@Component(value = "target_ft")
public class ToFtExecutor extends AbstractExecutor implements ITargetExecutor {


    @Override
    public Object execute(OperationContext context) {
        return null;
    }
}
