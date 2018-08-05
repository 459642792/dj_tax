package com.yun9.service.tax.core.event;

import com.yun9.service.tax.core.v2.OperationContext;
import com.yun9.service.tax.core.v2.OperationRequest;
import lombok.Data;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-06-13 16:53
 */
@Data
public class SynScreenShotEvent extends ServiceTaxEvent {
    private  OperationRequest operationRequest;
}
