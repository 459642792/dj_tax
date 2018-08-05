package com.yun9.service.tax.core.v2.target;

import com.yun9.biz.md.*;
import com.yun9.service.tax.core.v2.OperationContext;
import com.yun9.service.tax.core.v2.OperationInitialization;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractExecutor extends OperationInitialization {


    @Autowired
    protected BizMdInstService bizMdInstService;
    @Autowired
    protected BizMdCompanyService bizMdCompanyService;

    @Autowired
    protected BizMdAccountCycleService bizMdAccountCycleService;

    @Autowired
    protected BizMdAreaService bizMdAreaService;

    @Autowired
    protected BizMdInstClientService bizMdInstClientService;


    public OperationContext initInst(OperationContext context) {
        if (null == context.getBizMdInst()) {
            context.setBizMdInst(bizMdInstService.findById(context.getRequest().getInstId()));
        }
        return context;
    }


    public OperationContext initCompany(OperationContext context) {
        if (null == context.getBizMdCompany()) {
            context.setBizMdCompany(bizMdCompanyService.findById(context.getRequest().getCompanyId()));
        }

        return context;
    }


    public OperationContext initAccountCycle(OperationContext context) {
        if (null == context.getBizMdAccountCycle()) {
            context.setBizMdAccountCycle(bizMdAccountCycleService.findById(context.getRequest().getAccountCycleId()));
        }

        return context;
    }


    public OperationContext initArea(OperationContext context) {
        if (null == context.getBizMdArea()) {
            context.setBizMdArea(bizMdAreaService.findById(context.getBizMdCompany().getTaxAreaId()));
        }
        return context;
    }


    public OperationContext initInstClient(OperationContext context) {
        if (null == context.getBizMdInstClient()) {
            context.setBizMdInstClient(bizMdInstClientService.findByCompanyIdAndInstId(context.getBizMdCompany().getId(), context.getBizMdInst().getId()));
        }
        return context;
    }


}
