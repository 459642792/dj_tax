package com.yun9.service.tax.core.exception;

import com.yun9.commons.exception.BizException;

/**
 * Created by werewolf on  2018/5/31.
 */
public class ServiceTaxSendTaskException extends BizException {
    private static final int mainCode = 700000;


    public ServiceTaxSendTaskException(String message) {
        super(message);
        this.setCode(3000 + mainCode);
    }

    public ServiceTaxSendTaskException(String message, int code) {
        super(message);
        this.setCode(code);
    }

    public ServiceTaxSendTaskException(String message, Throwable cause) {
        super(message, cause);
        this.setCode(3000 + mainCode);
    }
}
