package com.yun9.service.tax.core.v2.ops;

import com.yun9.biz.tax.BizTaxCompanyBankService;
import com.yun9.biz.tax.BizTaxInstanceCategoryHistoryService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.BizTaxMdOfficeCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyBank;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryHistory;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategory;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.PayWay;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.ops.BizDeductService;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.exception.ServiceTaxException.Codes;
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

import java.util.*;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-05-22 19:46
 */
@ActionMapping(parameters = {
        @SnParameter(sn = ActionSn.send_deduct, taxOffice = TaxOffice.gs, cycleType = CycleType.y),
        @SnParameter(sn = ActionSn.send_deduct, taxOffice = TaxOffice.gs, cycleType = CycleType.m),
        @SnParameter(sn = ActionSn.send_deduct, taxOffice = TaxOffice.gs, cycleType = CycleType.q),
        @SnParameter(sn = ActionSn.send_deduct, taxOffice = TaxOffice.ds, cycleType = CycleType.y),
        @SnParameter(sn = ActionSn.send_deduct, taxOffice = TaxOffice.ds, cycleType = CycleType.m),
        @SnParameter(sn = ActionSn.send_deduct, taxOffice = TaxOffice.ds, cycleType = CycleType.q)
}
)
public class SendDeductOperation extends AbstractTaxRouterBuilder implements TaskStartHandler2 {

    private final static Logger logger = LoggerFactory.getLogger(SendDeductOperation.class);

    @Autowired
    private BizTaxMdOfficeCategoryService bizTaxMdOfficeCategoryService;
    @Autowired
    private TaskSendHandler taskSendHandler;
    @Autowired
    BizDeductService bizDeductService;
    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;
    private final static String PAY_WAY = "payWay";
    @Autowired
    BizTaxInstanceCategoryHistoryService bizTaxInstanceCategoryHistoryService;

    @Autowired
    BizTaxCompanyBankService bizTaxCompanyBankService;

    @Override
    public Object process(OperationContext context) {
        if (context.getRequest().getParams().get(PAY_WAY) == null) {
            throw ServiceTaxException.build(Codes.IllegalArgumentException, "payWay 不能为null");
        }
        //===================状态检查==========================
        logger.debug("----开始进行扣款前的状态检查---");
        if (!context.getBizTaxInstanceCategory().prepare2Deduct()) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.send_deduct_state_error, String.format("流程状态 :%s 执行状态 :%s,审核状态 :%s", context.getBizTaxInstanceCategory().getState().getName(), context.getBizTaxInstanceCategory().getProcessState().getName(), context.getBizTaxInstanceCategory().isAudit() ? "已审核" : "未审核"));
        }
        //===================税款是否缴清判断==========================
        if (!context.getBizTaxInstanceCategory().needDeduct()) {
            throw ServiceTaxException.build(Codes.do_not_need_deduct);
        }
        logger.debug("发起任务");
        taskSendHandler.send(context, super.defaultAccountAndParams(context));
        logger.debug("任务发送成功");
        //===================创建扣款记录==========================
        logger.debug("开始创建扣款记录");
        bizDeductService.startDeduct(context.getBizTaxInstanceCategory(),
                PayWay.valueOf(context.getRequest().getParams().get(PAY_WAY).toString()),
                context.getTaskSn(),
                context.getTaskInstanceId(), context.getTaskSeq(),
                context.getTaskBizId(), context.getRequest().getUserId(), null);
        //记录用户扣款日志
        bizTaxInstanceCategoryHistoryService.asynLog(context.getBizTaxInstanceCategory().getId(), context.getRequest().getUserId(), BizTaxInstanceCategoryHistory.Type.deduct, String.format("用户发起了扣款，扣款方式:%s，任务seq: %s", context.getRequest().getParams().get(PAY_WAY).toString(), context.getTaskSeq()));
        return context;
    }

    @Override
    protected Map<String, Object> buildParams(OperationContext context) {
        Optional<BizTaxMdOfficeCategory> optionalBizTaxMdOfficeCategory = Optional
                .ofNullable(bizTaxMdOfficeCategoryService
                        .findById(context.getBizTaxInstanceCategory().getBizTaxMdOfficeCategoryId()));
        optionalBizTaxMdOfficeCategory.orElseThrow(() -> ServiceTaxException
                .build(Codes.IllegalArgumentException, "没有找到对应税种BizTaxMdOfficeCategory")
        );

        return new HashMap<String, Object>(10) {{
            put("startDate", context.getBizTaxInstanceCategory().getBeginDate() * 1000);
            put("endDate", context.getBizTaxInstanceCategory().getEndDate() * 1000);
            put("contAmount", context.getBizTaxInstanceCategory().getRealPayAmount());
            final TaxOffice taxOffice = context.getBizTaxInstanceCategory().getBizTaxInstance().getTaxOffice();
            if (PayWay.valueOf(context.getRequest().getParams().get(PAY_WAY).toString()) == PayWay.bank && taxOffice == TaxOffice.ds) {
                final Long companyId = context.getBizTaxInstanceCategory().getBizTaxInstance().getMdCompanyId();
                final List<BizTaxCompanyBank> bizTaxCompanyBank = bizTaxCompanyBankService.findByAndBizMdCompanyIdInAndTaxOffice(new ArrayList<Long>() {{
                    add(companyId);
                }}, taxOffice);
                if (taxOffice == TaxOffice.ds && CollectionUtils.isEmpty(bizTaxCompanyBank)) {
                    throw ServiceTaxException.build(Codes.task_start_faied, "没有找到对应的扣款银行信息");
                }
                if (StringUtils.isEmpty(bizTaxCompanyBank.get(0).getBindSn())) {
                    throw ServiceTaxException.build(Codes.task_start_faied, "没有对应银行扣款标识信息");
                }
                put("clientBankSn", bizTaxCompanyBank.get(0).getBindSn());
            }
            put("taxPaySn", context.getBizTaxInstanceCategory().getTaxPaySn());
            put("taxCode", optionalBizTaxMdOfficeCategory.get().getCode());
            put("payWay", context.getRequest().getParams().get(PAY_WAY));
        }};
    }
}
