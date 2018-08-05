package com.yun9.service.tax.core.event;

import org.springframework.context.ApplicationEvent;

/**
 * Created by werewolf on  2018/6/12.
 */
public class ServiceTaxEvent extends ApplicationEvent {

    public ServiceTaxEvent() {
        super(ServiceTaxEvent.class);
    }
}
