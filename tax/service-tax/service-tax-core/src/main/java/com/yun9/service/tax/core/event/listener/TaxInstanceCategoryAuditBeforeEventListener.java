package com.yun9.service.tax.core.event.listener;

import com.yun9.service.tax.core.event.TaxInstanceCategoryAuditBeforeEvent;
import com.yun9.service.tax.core.report.ReportFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Created by werewolf on  2018/6/13.
 * 审核之前操作
 */
@Component
public class TaxInstanceCategoryAuditBeforeEventListener implements ApplicationListener<TaxInstanceCategoryAuditBeforeEvent> {


    @Autowired
    private ReportFactory reportFactory;


    @Override
    public void onApplicationEvent(TaxInstanceCategoryAuditBeforeEvent event) {
        //生成报表
        reportFactory.generate(event.getBizTaxInstanceCategory(), event.getBody());
    }
}
