package com.yun9.service.tax.core.v2.ops;

import com.yun9.biz.md.BizMdAccountCycleService;
import com.yun9.biz.md.domain.entity.BizMdAccountCycle;
import com.yun9.biz.tax.domain.dto.BizTaxInitDTO;
import com.yun9.biz.tax.domain.entity.BizTaxInstance;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.enums.TaxType;
import com.yun9.biz.tax.ops.BizTaxMultipleStartService;
import com.yun9.commons.exception.BizException;
import com.yun9.commons.utils.DateUtils;
import com.yun9.service.tax.core.taxrouter.request.LoginInfo;
import com.yun9.service.tax.core.taxrouter.request.TaxRouterRequest;
import com.yun9.service.tax.core.v2.OperationContext;
import com.yun9.service.tax.core.v2.annotation.ActionMapping;
import com.yun9.service.tax.core.v2.annotation.ActionSn;
import com.yun9.service.tax.core.v2.annotation.AreaSn;
import com.yun9.service.tax.core.v2.annotation.SnParameter;
import com.yun9.service.tax.core.v2.handler.TaskSendHandler;
import com.yun9.service.tax.core.v2.handler.TaskStartHandler2;
import com.yun9.service.tax.core.v2.handler.TaxRouterBuilder;
import com.yun9.service.tax.core.v2.handler.TaxRouterLoginHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by werewolf on  2018/5/30.
 * 多税种下载
 */
@ActionMapping(parameters = {
        @SnParameter(sn = ActionSn.get_taxes, taxOffice = TaxOffice.ds, cycleType = CycleType.q, area = AreaSn.shenzhen),
        @SnParameter(sn = ActionSn.get_taxes, taxOffice = TaxOffice.ds, cycleType = CycleType.y, area = AreaSn.shenzhen),
        @SnParameter(sn = ActionSn.get_taxes, taxOffice = TaxOffice.ds, cycleType = CycleType.m, area = AreaSn.shenzhen),
        @SnParameter(sn = ActionSn.get_taxes, taxOffice = TaxOffice.gs, cycleType = CycleType.q, area = AreaSn.shenzhen),
        @SnParameter(sn = ActionSn.get_taxes, taxOffice = TaxOffice.gs, cycleType = CycleType.y, area = AreaSn.shenzhen),
        @SnParameter(sn = ActionSn.get_taxes, taxOffice = TaxOffice.gs, cycleType = CycleType.m, area = AreaSn.shenzhen)
}
)
public class GetTaxCategoriesOperation implements TaskStartHandler2, TaxRouterBuilder {


    @Autowired
    private BizTaxMultipleStartService bizTaxMultipleStartService;

    @Autowired
    private TaskSendHandler taskSendHandler;


    @Autowired
    private TaxRouterLoginHandler taxRouterLoginHandler;

    public static final Logger logger = LoggerFactory.getLogger(GetTaxCategoriesOperation.class);

    @Autowired
    private BizMdAccountCycleService bizMdAccountCycleService;

    @Override
    public Object process(OperationContext context) {

        if (context.getBizMdCompany().getTaxType().equals(com.yun9.biz.md.enums.TaxType.none)) {
            throw new com.yun9.commons.exception.IllegalArgumentException("发起下载失败,客户纳税方式不正确");
        }

        if (context.getBizMdAccountCycle().getType().equals(com.yun9.biz.md.enums.CycleType.m)) {
            //判断当前会计期间是否存在季度
            final BizMdAccountCycle _bizMdAccountCycle = bizMdAccountCycleService.findBySnAndType(context.getBizMdAccountCycle().getSn(), com.yun9.biz.md.enums.CycleType.q);
            if (null != _bizMdAccountCycle) {
                if (_bizMdAccountCycle.getId() != context.getBizMdAccountCycle().getId()) {
                    throw new com.yun9.commons.exception.IllegalArgumentException("当前时间为季度,传入的会计期间为月");
                }
            }

        }
        BizTaxInstance bizTaxInstance = bizTaxMultipleStartService.start(
                context.getRequest().getAccountCycleId(),
                context.getRequest().getTaxOffice(),
                context.getRequest().getUserId(),
                new BizTaxInitDTO()
                        .setCompanyId(context.getBizMdCompany().getId())
                        .setCompanyName(context.getBizMdCompany().getFullName())
                        .setTaxAreaId(context.getBizMdCompany().getTaxAreaId())
                        .setInstClientId(context.getBizMdInstClient().getId())
                        .setTaxType(TaxType.valueOf(context.getBizMdCompany().getTaxType().toString()))
                        .setClientSn(context.getBizMdInstClient().getSn()));
        context.setBizTaxInstance(bizTaxInstance);
        try {

            // TaskBO taskBO = taskSendHandler.sendToTask(context, this.build(context));
            taskSendHandler.send(context, this.build(context));
            bizTaxMultipleStartService.success(context.getBizTaxInstance().getId(),
                    context.getTaskSn(),
                    context.getTaskInstanceId(),
                    context.getTaskSeq(),
                    context.getTaskBizId(),
                    context.getRequest().getUserId());
        } catch (BizException ex) {
            logger.error("发送下载任务失败{}", ex.getMessage());
            //todo 发起异常不处理【{"message":"任务还未执行完成，请勿重复发起。","status":500}】 更新后会变成exception
            //bizTaxMultipleStartService.exception(bizTaxInstance.getId(), ex.getCode() + "", ex.getMessage());
            throw ex;
        }
        return context;
    }

    @Override
    public TaxRouterRequest build(OperationContext context) {

        LoginInfo loginInfo = taxRouterLoginHandler.getCompanyDefaultAccount(
                context.getBizMdCompany(),
                context.getRequest().getTaxOffice().name(),
                context.getBizMdArea().getSn(),
                context.getRequest().getActionSn()
        );
        Map<String, Object> params = new HashMap();

        params.put("startDate", DateUtils.unixTime(DateUtils.unixTimeToLocalDate(context.getBizMdAccountCycle().getBeginDate()).with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay()) * 1000); //毫秒
        params.put("endDate", DateUtils.unixTime(DateUtils.unixTimeToLocalDate(context.getBizMdAccountCycle().getEndDate()).with(TemporalAdjusters.lastDayOfMonth()).atStartOfDay()) * 1000); //毫秒
        params.put("endaccountcyclesn", context.getBizMdAccountCycle().getSn());
        //当前会计期间closeDate 第一天-最后一天
        LocalDate localDate = DateUtils.unixTimeToLocalDate(context.getBizMdAccountCycle().getTaxClosingDate());
        params.put("currStartDate", DateUtils.unixTime(localDate.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay()) * 1000);//毫秒
        params.put("currEndDate", DateUtils.unixTime(localDate.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59)) * 1000 + 999);//毫秒

        params.put("year", context.getBizMdAccountCycle().getYear());
        params.put("month", context.getBizMdAccountCycle().getMonth());
        params.put("taxType", context.getRequest().getTaxOffice().name().toUpperCase());
        TaxRouterRequest taxRouterRequest = new TaxRouterRequest();
        taxRouterRequest.setLoginInfo(loginInfo);
        taxRouterRequest.setParams(params);
        return taxRouterRequest;

    }
}

