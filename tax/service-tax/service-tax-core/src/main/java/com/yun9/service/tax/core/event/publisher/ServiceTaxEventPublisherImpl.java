package com.yun9.service.tax.core.event.publisher;

import com.yun9.service.tax.core.ServiceTaxThreadPoolContainer;
import com.yun9.service.tax.core.event.ServiceTaxEvent;
import com.yun9.service.tax.core.event.ServiceTaxEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Created by werewolf on  2018/6/12.
 */
@Component
public class ServiceTaxEventPublisherImpl implements ServiceTaxEventPublisher {

    public static final Logger logger = LoggerFactory.getLogger(ServiceTaxEventPublisherImpl.class);
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Resource(name = "serviceTaxThreadPoolContainer")
    ServiceTaxThreadPoolContainer serviceTaxThreadPoolContainer;

    @Override
    public void publish(ServiceTaxEvent event) {

        logger.info("publish event {} ->{}", event.getClass().getName(), event);
        applicationEventPublisher.publishEvent(event);

    }


    @Override
    public void publishAsync(ServiceTaxEvent event) {
        logger.info("publish asynchronous event {} ->{}", event.getClass().getName(), event);
        serviceTaxThreadPoolContainer.getEventThreadPool().execute(() -> {
            applicationEventPublisher.publishEvent(event);
        });

    }
}
