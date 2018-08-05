package com.yun9.service.tax.core.ft;

import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.service.tax.core.v2.annotation.SnParameter;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Created by werewolf on  2018/6/5.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface TaxCategoryMapping {
    String value() default "";

    TaxSn[] sn() default {};
}
