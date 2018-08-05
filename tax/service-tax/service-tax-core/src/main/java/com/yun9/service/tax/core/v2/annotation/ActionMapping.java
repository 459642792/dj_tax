package com.yun9.service.tax.core.v2.annotation;

import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Created by werewolf on  2018/5/7.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ActionMapping {

    String value() default "";

    SnParameter[] parameters() default {};

//    ActionSn sn();
//
//    TaxOffice taxOffice() default TaxOffice.gs;
//
//    CycleType cycleType();
}
