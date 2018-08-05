package com.yun9.service.tax.core.v2.ops;

import com.yun9.biz.tax.BizTaxInstanceCategoryDeductService;
import com.yun9.biz.tax.BizTaxInstanceCategoryHistoryService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryDeduct;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryHistory;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.ops.BizRepealTaxService;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.orm.commons.entity.Disabled;
import com.yun9.service.tax.core.ServiceTaxThreadPoolContainer;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.exception.ServiceTaxException.Codes;
import com.yun9.service.tax.core.v2.OperationContext;
import com.yun9.service.tax.core.v2.annotation.ActionMapping;
import com.yun9.service.tax.core.v2.annotation.ActionSn;
import com.yun9.service.tax.core.v2.annotation.SnParameter;
import com.yun9.service.tax.core.v2.handler.AbstractTaxRouterBuilder;
import com.yun9.service.tax.core.v2.handler.TaskSendHandler;
import com.yun9.service.tax.core.v2.handler.TaskStartHandler2;
import com.yun9.service.tax.core.v2.handler.TaxRouterTaxCategoryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-05-17 11:46
 */
@ActionMapping(parameters = {
        @SnParameter(sn = ActionSn.repeal_tax, taxOffice = TaxOffice.ds, cycleType = CycleType.q),
        @SnParameter(sn = ActionSn.repeal_tax, taxOffice = TaxOffice.gs, cycleType = CycleType.q),
        @SnParameter(sn = ActionSn.repeal_tax, taxOffice = TaxOffice.ds, cycleType = CycleType.m),
        @SnParameter(sn = ActionSn.repeal_tax, taxOffice = TaxOffice.gs, cycleType = CycleType.m),
        @SnParameter(sn = ActionSn.repeal_tax, taxOffice = TaxOffice.ds, cycleType = CycleType.y),
        @SnParameter(sn = ActionSn.repeal_tax, taxOffice = TaxOffice.gs, cycleType = CycleType.y),
}
)
public class RepealTaxOperation extends AbstractTaxRouterBuilder implements TaskStartHandler2 {

    private final static Logger logger = LoggerFactory.getLogger(RepealTaxOperation.class);
    @Autowired
    BizRepealTaxService bizRepealTaxService;
    @Autowired
    TaskSendHandler taskSendHandler;
    @Autowired
    TaxRouterTaxCategoryHelper taxRouterTaxCategoryHelper;
    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;
    @Autowired
    BizTaxInstanceCategoryDeductService bizTaxInstanceCategoryDeductService;
    @Autowired
    BizTaxInstanceCategoryHistoryService bizTaxInstanceCategoryHistoryService;
    @Autowired
    ServiceTaxThreadPoolContainer serviceTaxThreadPoolContainer;

    @Override
    public Object process(OperationContext context) {
        //======================判断当前状态十分允许作废=========================
        if (!context.getBizTaxInstanceCategory().prepare2Repeal()) {
            throw ServiceTaxException
                    .build(Codes.repeal_tax_state_error, String.format("流程状态 :%s 执行状态 :%s,申报状态为:%s 无法发起作废", context.getBizTaxInstanceCategory().getState().getName(), context.getBizTaxInstanceCategory().getProcessState().getName(), context.getBizTaxInstanceCategory().getDeclareType().getName()));
        }
        final long instanceCategoryId = context.getBizTaxInstanceCategory().getId();
        List<BizTaxInstanceCategoryDeduct> deducts = bizTaxInstanceCategoryDeductService.findByBizTaxInstanceCategoryId(instanceCategoryId);
        if (CollectionUtils.isNotEmpty(deducts)) {
            deducts.forEach(v -> {
                if (v.getDisabled() == Disabled.ENABLE.getValue() && v.getState() == BizTaxInstanceCategoryDeduct.State.success) {
                    throw ServiceTaxException.build(Codes.task_start_faied, "已扣款成功，无法发起作废");
                }
            });
        }
        //===========================任务发起===================================
        // TaskBO taskBO = taskSendHandler.sendToTask(context, super.defaultAccountAndParams(context));
        taskSendHandler.send(context, super.defaultAccountAndParams(context));
        //==============================seq已经taxinstancecategory状态回写=======================================
        bizRepealTaxService.startRepeal(context.getBizTaxInstanceCategory(), context.getTaskSn(),
                context.getTaskInstanceId(), context.getTaskSeq(),
                context.getTaskBizId(), context.getRequest().getUserId(), null);
        //记录用户作废日志
        bizTaxInstanceCategoryHistoryService.asynLog(context.getBizTaxInstanceCategory().getId(), context.getRequest().getUserId(), BizTaxInstanceCategoryHistory.Type.repeal, String.format("用户发起了作废，任务seq: %s", context.getTaskSeq()));
        return "作废发起成功";
    }

    @Override
    protected Map<String, Object> buildParams(OperationContext context) {

        Long taxMdOfficeCategoryId =context.getBizTaxInstanceCategory().getBizTaxMdOfficeCategoryId();
        if (taxMdOfficeCategoryId==null){
            throw ServiceTaxException.build(Codes.IllegalArgumentException,"数据错误，申报记录没有税种信息");
        }
        String taxCode = taxRouterTaxCategoryHelper.findTaxCodeByBizTaxMdOfficeCategoryId(taxMdOfficeCategoryId);
        if (StringUtils.isEmpty(taxCode)){
            throw ServiceTaxException.build(Codes.IllegalArgumentException,"数据错误，申报记录没有税种信息");
        }


        return new HashMap<String, Object>(6) {{
            put("startDate", context.getBizTaxInstanceCategory().getBeginDate() * 1000);
            put("endDate", context.getBizTaxInstanceCategory().getEndDate() * 1000);
            put("taxPaySn", context.getBizTaxInstanceCategory().getTaxPaySn());
            put("taxCode", taxCode);
            put("currentStartDate", context.getBizTaxInstanceCategory().getBeginDate() * 1000);
            put("currentEndDate", context.getBizTaxInstanceCategory().getDeclareDate() * 1000);

        }};
    }


}
