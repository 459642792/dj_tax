package com.yun9.service.tax.core.event.listener;

import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.BizTaxMdCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdMsgCode;
import com.yun9.biz.tax.enums.DeclareType;
import com.yun9.commons.exception.BizException;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.event.TaskDownloadComplete;
import com.yun9.service.tax.core.ft.TaxMdCategoryHandler;
import com.yun9.service.tax.core.ft.handler.AuditHandler;
import com.yun9.service.tax.core.report.ReportFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Created by werewolf on  2018/6/12.
 */
@Component
public class TaskDownloadCompleteEventListener implements ApplicationListener<TaskDownloadComplete> {

    @Autowired
    private TaxMdCategoryHandler taxMdCategoryHandler;

    @Autowired
    private BizTaxMdCategoryService bizTaxMdCategoryService;

    @Autowired
    private ReportFactory reportFactory;
    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Override
    public void onApplicationEvent(TaskDownloadComplete event) {
//        //判断税种类型 是否需要审核 ，是否直接生成报表
        BizTaxInstanceCategory bizTaxInstanceCategory = event.getBizTaxInstanceCategory();

        //税种在清册中不存在 直接返回
        if (StringUtils.isNotEmpty(bizTaxInstanceCategory.getProcessCodeId()) && BizTaxMdMsgCode.Process.not_at_declare_tax_category.getCode() == bizTaxInstanceCategory.getProcessCodeId()) {
            return;
        }

        BizTaxMdCategory bizTaxMdCategory = bizTaxMdCategoryService.findById(bizTaxInstanceCategory.getBizTaxMdCategoryId());
        //网上申报[直接生成报表]
        if (bizTaxInstanceCategory.getDeclareType() == DeclareType.taxOffice) {
            reportFactory.generate(event.getBizTaxInstanceCategory(), event.getBody());
            return;
        }


        AuditHandler auditHandler = taxMdCategoryHandler.auditHandler(bizTaxMdCategory.getSn());
        if (null != auditHandler) {
            //判断是否需要审核
            if (auditHandler.isNeedAudit(bizTaxInstanceCategory, bizTaxMdCategory)) {
                if (bizTaxInstanceCategory.getProcessState() == BizTaxInstanceCategory.ProcessState.process) {
                    bizTaxInstanceCategory.setProcessMessage("当前税种正在处理中，不能进行审核操作。");
                    bizTaxInstanceCategoryService.create(bizTaxInstanceCategory);
                    return;
                }
                taxMdCategoryHandler.getTaxCategoriesTaskToAudit(bizTaxInstanceCategory, event.getBody(), bizTaxInstanceCategory.getCreatedBy(),
                        new TaxMdCategoryHandler.AuditCallback() {
                            @Override
                            public void success() {

                            }

                            @Override
                            public void exception(BizException ex) {
                                throw ex;
                            }
                        });
            }
        }

    }
}
