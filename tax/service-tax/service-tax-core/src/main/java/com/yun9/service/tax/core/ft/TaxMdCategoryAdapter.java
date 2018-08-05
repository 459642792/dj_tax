package com.yun9.service.tax.core.ft;

import com.yun9.biz.report.BizReportService;
import com.yun9.biz.tax.BizTaxInstanceCategoryReportService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.BizTaxMdCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryReport;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.commons.exception.BizException;
import com.yun9.service.tax.core.event.ServiceTaxEventPublisher;
import com.yun9.service.tax.core.event.TaxInstanceCategoryAuditBeforeEvent;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.ft.handler.AuditHandler;
import com.yun9.service.tax.core.task.helper.JsonMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by werewolf on  2018/6/5.
 */
@Component("taxMdCategoryHandler")
public class TaxMdCategoryAdapter implements TaxMdCategoryHandler {

    public static final Logger logger = LoggerFactory.getLogger(TaxMdCategoryAdapter.class);

    @Autowired
    List<AuditHandler> auditHandlerList;

    private Map<TaxSn, AuditHandler> auditHandlers = new ConcurrentHashMap<>();

    @Autowired
    private BizTaxInstanceCategoryService bizTaxInstanceCategoryService;
    @Autowired
    private BizTaxMdCategoryService bizTaxMdCategoryService;
    @Autowired
    private BizTaxInstanceCategoryReportService bizTaxInstanceCategoryReportService;
    @Autowired
    private BizReportService bizReportService;

    private void initAuditHandlers() {
        auditHandlerList.forEach(v -> {
            TaxCategoryMapping taxCategoryMapping = v.getClass().getAnnotation(TaxCategoryMapping.class);
            if (null != taxCategoryMapping) {
                TaxSn[] sns = taxCategoryMapping.sn();
                if (sns.length > 0) {
                    for (TaxSn sn : sns) {
                        auditHandlers.put(sn, v);
                    }
                }
            }
        });
    }

    @PostConstruct
    public void init() {
        this.initAuditHandlers();
    }

    @Autowired
    private ServiceTaxEventPublisher serviceTaxEventPublisher;

    @Override
    public AuditHandler auditHandler(TaxSn taxSn) {
        return auditHandlers.get(taxSn);
    }

    @Override
    public void audit(long bizTaxInstanceCategoryId, long processBy, AuditCallback auditCallback) {
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService.findById(bizTaxInstanceCategoryId);
        this.audit(bizTaxInstanceCategory, processBy, auditCallback);
    }

    @Override
    public void audit(BizTaxInstanceCategory bizTaxInstanceCategory, long processBy, AuditCallback auditCallback) {
        this.getTaxCategoriesTaskToAudit(bizTaxInstanceCategory, null, processBy, auditCallback);
    }

    @Override
    public void getTaxCategoriesTaskToAudit(BizTaxInstanceCategory bizTaxInstanceCategory, JsonMap jsonMap, long processBy, AuditCallback auditCallback) {
        BizTaxMdCategory bizTaxMdCategory = bizTaxMdCategoryService.findById(bizTaxInstanceCategory.getBizTaxMdCategoryId());
        //=========发送审核之前事件================
        try {
            if (bizTaxInstanceCategory.isAudit()) {
                auditCallback.success();
                return;
            }

            bizTaxInstanceCategoryService.isProcess(bizTaxInstanceCategory);

            //===========先审核具体税种=========================
            AuditHandler auditHandler = auditHandlers.get(bizTaxMdCategory.getSn());
            if (null != auditHandler) {
                long start1 = System.currentTimeMillis();
                auditHandler.audit(bizTaxInstanceCategory);
                logger.debug("---------------审核-税种审核{}:{}", bizTaxInstanceCategory.getId(), System.currentTimeMillis() - start1);
            }
            //========================生成报表========================
            long start2 = System.currentTimeMillis();
            serviceTaxEventPublisher.publish(new TaxInstanceCategoryAuditBeforeEvent()
                    .setBizTaxInstanceCategory(bizTaxInstanceCategory)
                    .setBody(jsonMap)
                    .setProcessBy(processBy)
            );
            logger.debug("---------------审核-创建报表{}:{}", bizTaxInstanceCategory.getId(), System.currentTimeMillis() - start2);

            long start3 = System.currentTimeMillis();
            BizTaxInstanceCategoryReport bizTaxInstanceCategoryReport = bizTaxInstanceCategoryReportService.findByTaxInstanceCategoryId(bizTaxInstanceCategory.getId());
            if (null != bizTaxInstanceCategoryReport) {
                //================审核报表
                bizReportService.audit(bizTaxInstanceCategoryReport.getBizReportInstanceId(), String.valueOf(processBy));
                logger.debug("---------------审核-审核报表{}:{}", bizTaxInstanceCategory.getId(), System.currentTimeMillis() - start3);
            }
            //==========更新审核状态============
            bizTaxInstanceCategoryService.audit(bizTaxInstanceCategory.getId(), processBy);
            auditCallback.success();
        } catch (BizException ex) {
            //发布事件执行失败 or 审核失败
            auditCallback.exception(ex);
        }
    }

    @Override
    public void cancelOfAudit(long bizTaxInstanceCategoryId, long processBy) {
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService.findById(bizTaxInstanceCategoryId);
        if (bizTaxInstanceCategory.getProcessState() == BizTaxInstanceCategory.ProcessState.process) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.AUDIT_ERROR, "当前正在执行不能取消审核");
        }
        BizTaxMdCategory bizTaxMdCategory = bizTaxMdCategoryService.findById(bizTaxInstanceCategory.getBizTaxMdCategoryId());
        auditHandlers.get(bizTaxMdCategory.getSn()).cancelOfAudit(bizTaxInstanceCategory);
        bizTaxInstanceCategoryService.cancelOfAudit(bizTaxInstanceCategoryId, processBy);
    }
}
