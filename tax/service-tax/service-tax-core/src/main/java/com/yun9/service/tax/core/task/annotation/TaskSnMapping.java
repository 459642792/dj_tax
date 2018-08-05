package com.yun9.service.tax.core.task.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Created by werewolf on  2018/3/28.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface TaskSnMapping {

    String value() default "";

    String[] sns();
}
