package com.yun9.service.tax.core.event;

/**
 * Created by werewolf on  2018/6/12.
 */
public interface ServiceTaxEventPublisher {

    /**
     * deprecated
     * use com.yun9.service.tax.core.event.ServiceTaxEventPublisher#synPublish(com.yun9.service.tax.core.event.ServiceTaxEvent) to replace
     * @param event
     */
    void publish(ServiceTaxEvent event);


    /**
     * publish the asynchronous event
     * @param event
     */
    void publishAsync(ServiceTaxEvent event);
}
