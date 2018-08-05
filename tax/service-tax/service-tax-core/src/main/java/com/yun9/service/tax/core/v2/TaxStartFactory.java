package com.yun9.service.tax.core.v2;

public interface TaxStartFactory {

    Object handler(OperationRequest actionRequest);

    Object handler(MultOperationRequest actionRequest);
}
