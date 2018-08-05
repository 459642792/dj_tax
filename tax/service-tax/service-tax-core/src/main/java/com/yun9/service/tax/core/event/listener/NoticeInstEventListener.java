package com.yun9.service.tax.core.event.listener;

import com.yun9.service.tax.core.TaxNoticeInstFactory;
import com.yun9.service.tax.core.event.NoticeInstEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;


@Component
public class NoticeInstEventListener implements ApplicationListener<NoticeInstEvent> {
    private final static Logger logger = LoggerFactory.getLogger(NoticeInstEventListener.class);

    @Autowired
    private TaxNoticeInstFactory taxNoticeInstFactory;


    @Override
    public void onApplicationEvent(NoticeInstEvent event) {
        logger.info("触发通知机构--->{}", event);
        taxNoticeInstFactory.noticeByBizTaxInstanceCategoryId(event.getBizTaxInstanceCategoryId());
        logger.info("完成通知机构发起请求");

    }
}
