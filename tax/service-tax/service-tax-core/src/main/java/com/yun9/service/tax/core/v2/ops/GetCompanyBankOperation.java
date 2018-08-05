package com.yun9.service.tax.core.v2.ops;

import com.yun9.biz.tax.BizTaxInstanceCategoryReportService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.BizTaxInstanceSeqService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceSeq;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.v2.OperationContext;
import com.yun9.service.tax.core.v2.annotation.ActionMapping;
import com.yun9.service.tax.core.v2.annotation.ActionSn;
import com.yun9.service.tax.core.v2.annotation.SnParameter;
import com.yun9.service.tax.core.v2.handler.AbstractTaxRouterBuilder;
import com.yun9.service.tax.core.v2.handler.TaskSendHandler;
import com.yun9.service.tax.core.v2.handler.TaskStartHandler2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ActionMapping(parameters = {
        @SnParameter(sn = ActionSn.get_company_bank, taxOffice = TaxOffice.ds, cycleType = CycleType.y),
        @SnParameter(sn = ActionSn.get_company_bank, taxOffice = TaxOffice.ds, cycleType = CycleType.m),
        @SnParameter(sn = ActionSn.get_company_bank, taxOffice = TaxOffice.ds, cycleType = CycleType.q)

}
)
public class GetCompanyBankOperation extends AbstractTaxRouterBuilder implements TaskStartHandler2 {
    private final static Logger logger = LoggerFactory.getLogger(GetCompanyBankOperation.class);

    @Autowired
    BizTaxInstanceCategoryReportService bizTaxInstanceCategoryReportService;
    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;
    @Autowired
    TaskSendHandler taskSendHandler;
    @Autowired
    BizTaxInstanceSeqService bizTaxInstanceSeqService;

    @Override
    public Object process(OperationContext context) {

        logger.debug("传入的参数 -->{}", context.getRequest());
        context.setBizTaxInstanceCategory(Optional.ofNullable(
                bizTaxInstanceCategoryService.findById(context.getRequest().getTaxInstanceCategoryId()))
                .orElseThrow(() -> ServiceTaxException.build(ServiceTaxException.Codes.declare_info_not_found, "税种申报实例信息未找到")));


        //================申报实例申报前状态校验==========================
        logger.debug("----开始进行申报前的状态检查---");
        if (!context.getBizTaxInstanceCategory().prepare2Deduct()) {
            logger.error("申报状态错误state-->{},ProcessState-->{}",
                    context.getBizTaxInstanceCategory().getState().name(),
                    context.getBizTaxInstanceCategory().getProcessState().name());
            throw ServiceTaxException.build(ServiceTaxException.Codes.tax_state_error,
                    context.getBizTaxInstanceCategory().getState().name(),
                    context.getBizTaxInstanceCategory().getProcessState().name());
        }

        if (StringUtils.isEmpty(context.getBizTaxInstanceCategory().getTaxPaySn())) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.TASK_NOT_ALLOWED_SEND,
                    "没有应征凭证序号");
        }
        if (StringUtils.isEmpty(context.getBizTaxInstanceCategory().getDeclareDate())) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.TASK_NOT_ALLOWED_SEND,
                    "申报时间为null");
        }
        //=====================发起任务==============================================
        taskSendHandler.send(context, this.build(context));

        //保存seq
        BizTaxInstanceSeq bizTaxInstanceSeq = new BizTaxInstanceSeq();
        bizTaxInstanceSeq.setBizTaxInstanceId(context.getBizTaxInstanceCategory().getBizTaxInstanceId());
        bizTaxInstanceSeq.setBizTaxInstanceCategoryId(context.getBizTaxInstanceCategory().getId());
        bizTaxInstanceSeq.setTaskInstanceId(context.getTaskInstanceId());
        bizTaxInstanceSeq.setTaskSn(context.getTaskSn());
        bizTaxInstanceSeq.setSeq(context.getTaskSeq());
        //设置任务为处理中
        bizTaxInstanceSeq.setState(BizTaxInstanceSeq.State.process);
        bizTaxInstanceSeq.setTaskBizId(context.getTaskBizId());
        bizTaxInstanceSeq.setCreatedBy(context.getRequest().getUserId());
        bizTaxInstanceSeqService.save(bizTaxInstanceSeq);

        return "任务发起成功";
    }


    @Override
    protected Map<String, Object> buildParams(OperationContext context) {
        logger.debug("开始查询税种实例对应数据");
        BizTaxInstanceCategory bizTaxInstanceCategory = context.getBizTaxInstanceCategory();
        return new HashMap<String, Object>(7) {{
            put("pzxh", context.getBizTaxInstanceCategory().getTaxPaySn());
            put("startDate", context.getBizTaxInstanceCategory().getDeclareDate() * 1000);
            put("endDate", System.currentTimeMillis());
        }};
    }
}
