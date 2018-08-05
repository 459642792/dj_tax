package com.yun9.service.tax.core.v2.ops;

import com.yun9.biz.tax.BizTaxInstanceCategoryDeductService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.BizTaxInstanceSeqService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryDeduct;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceSeq;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.ops.BizTaxDeclareService;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.ft.TaxMdCategoryHandler;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ActionMapping(parameters = {
        @SnParameter(sn = ActionSn.find_deduct, taxOffice = TaxOffice.ds, cycleType = CycleType.m),
        @SnParameter(sn = ActionSn.find_deduct, taxOffice = TaxOffice.ds, cycleType = CycleType.q),
        @SnParameter(sn = ActionSn.find_deduct, taxOffice = TaxOffice.ds, cycleType = CycleType.y)
}
)
public class FindCompanyDeduct  extends AbstractTaxRouterBuilder implements TaskStartHandler2
{    private final static Logger logger = LoggerFactory.getLogger(FindCompanyDeduct.class);
    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;
    @Autowired
    TaskSendHandler taskSendHandler;
    @Autowired
    BizTaxDeclareService bizTaxDeclareService;
    @Autowired
    BizTaxInstanceSeqService bizTaxInstanceSeqService;
    @Autowired
    BizTaxInstanceCategoryDeductService bizTaxInstanceCategoryDeductService;


    @Override
    protected Map<String, Object> buildParams(OperationContext context) {
        //开始获取报表数据
        logger.debug("开始查询税种实例对应数据");
        BizTaxInstanceCategory bizTaxInstanceCategory = context.getBizTaxInstanceCategory();
        return new HashMap<String, Object>(7) {{
            put("pzxh",bizTaxInstanceCategory.getTaxPaySn());
            put("startDate", bizTaxInstanceCategory.getDeclareDate() * 1000);
            put("endDate", System.currentTimeMillis());
        }};
    }

    @Override
    public Object process(OperationContext context) {

        context.setBizTaxInstanceCategory(Optional.ofNullable(
                context.getBizTaxInstanceCategory())
                .orElseThrow(
                        () -> ServiceTaxException.build(ServiceTaxException.Codes.declare_info_not_found, "税种申报实例信息未找到")));

        if (!(BizTaxInstanceCategory.State.deduct.equals(context.getBizTaxInstanceCategory().getState())
                && BizTaxInstanceCategory.ProcessState.process.equals(context.getBizTaxInstanceCategory().getProcessState())) )  {
            logger.error("申报状态错误state-->{},ProcessState-->{}",
                    context.getBizTaxInstanceCategory().getState().name(),
                    context.getBizTaxInstanceCategory().getProcessState().name());
            throw ServiceTaxException.build(ServiceTaxException.Codes.send_tax_state_error, String.format("流程状态 :%s 执行状态 :%s,审核状态为:%s 无法发起申报", context.getBizTaxInstanceCategory().getState().getName(), context.getBizTaxInstanceCategory().getProcessState().getName(), context.getBizTaxInstanceCategory().isAudit() ? "已审核" : "未审核"));
        }
        if (StringUtils.isEmpty(context.getBizTaxInstanceCategory().getTaxPaySn())) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.TASK_NOT_ALLOWED_SEND,
                    "没有应征凭证序号");
        }
        if (StringUtils.isEmpty(context.getBizTaxInstanceCategory().getDeclareDate())) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.TASK_NOT_ALLOWED_SEND,
                    "申报时间为null");
        }
        BizTaxInstanceCategoryDeduct  bizTaxInstanceCategoryDeduct =     bizTaxInstanceCategoryDeductService.findByBizTaxInstanceCategoryIdAndStateAndTypeDisabled(context.getBizTaxInstanceCategory().getId(),
                BizTaxInstanceCategoryDeduct.State.none,BizTaxInstanceCategoryDeduct.Type.wx);
        logger.error("发起的微信扣款数据==============:{}",bizTaxInstanceCategoryDeduct);
        if (null == bizTaxInstanceCategoryDeduct){
            throw ServiceTaxException.build(ServiceTaxException.Codes.TASK_NOT_ALLOWED_SEND,
                    "没有发起微信扣款");
        }
        if (StringUtils.isEmpty(bizTaxInstanceCategoryDeduct.getPayExpiry()) || getDateTimeOfTimestamp(bizTaxInstanceCategoryDeduct.getPayExpiry()*1000).isBefore(LocalDateTime.now())){
            throw ServiceTaxException.build(ServiceTaxException.Codes.TASK_NOT_ALLOWED_SEND,
                    "发起的微信扣款有效期过期");
        }

        //=====================发起任务==============================================
        taskSendHandler.send(context, this.build(context));
        //====================调用申报发起成功========================================================
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
    static LocalDateTime getDateTimeOfTimestamp(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZoneId zone = ZoneId.systemDefault();
        return LocalDateTime.ofInstant(instant, zone);
    }

}
