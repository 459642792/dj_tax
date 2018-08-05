package com.yun9.service.tax.controller;

import com.yun9.service.tax.core.event.ServiceTaxEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by werewolf on  2018/6/12.
 */
@Controller
@RequestMapping("/test")
public class TestController {

    @Autowired
    private ServiceTaxEventPublisher serviceTaxEventPublisher;

    @GetMapping
    @ResponseBody
    public String t() {
        System.out.println("发送下载税种事件. ");
//        GetTaxCategoriesEvent getTaxCategoriesEvent = new GetTaxCategoriesEvent();
//        getTaxCategoriesEvent.setTaxInstanceCategoryId(1L);
//        serviceTaxEventPublisher.publish(getTaxCategoriesEvent);
//
//
//        TaxInstanceCategoryAuditEvent taxInstanceCategoryAuditEvent = new TaxInstanceCategoryAuditEvent();
//        taxInstanceCategoryAuditEvent.setTaxInstanceCategoryId(2L);
//        serviceTaxEventPublisher.publish(taxInstanceCategoryAuditEvent);

        return "123";
    }
}
