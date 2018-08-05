package com.yun9.service.tax.core.v2.annotation;

import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by werewolf on  2018/5/7.
 */
@Target({})
@Retention(RUNTIME)
public @interface SnParameter {

    ActionSn sn();

    TaxOffice taxOffice() default TaxOffice.gs;

    CycleType cycleType();

    AreaSn area() default AreaSn.shenzhen;
}
