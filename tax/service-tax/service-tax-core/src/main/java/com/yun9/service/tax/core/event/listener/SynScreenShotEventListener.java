package com.yun9.service.tax.core.event.listener;

import com.yun9.service.tax.core.event.SynScreenShotEvent;
import com.yun9.service.tax.core.v2.TaxStartFactory;
import com.yun9.service.tax.core.v2.ops.SynScreenShotOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-06-13 16:53
 */
@Component
public class SynScreenShotEventListener implements ApplicationListener<SynScreenShotEvent> {
    private final static Logger logger = LoggerFactory.getLogger(SynScreenShotEventListener.class);
    @Autowired
    private TaxStartFactory taxStartFactory;


    @Override
    public void onApplicationEvent(SynScreenShotEvent event) {
        logger.info("触发截图--->{}", event);
        taxStartFactory.handler(event.getOperationRequest());
        logger.info("完成截图发起请求");

    }
}
