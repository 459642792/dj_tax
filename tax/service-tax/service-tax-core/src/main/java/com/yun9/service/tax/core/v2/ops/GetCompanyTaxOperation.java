package com.yun9.service.tax.core.v2.ops;

import com.yun9.biz.md.domain.entity.BizMdAccountCycle;
import com.yun9.biz.md.domain.entity.BizMdCompany;
import com.yun9.biz.tax.BizTaxCompanyTaxService;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyTax;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.ops.BizTaxCompanyTaxStartService;
import com.yun9.commons.exception.BizException;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.exception.ServiceTaxException;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by werewolf on  2018/6/21.
 * 同步客户资料信息
 */
@ActionMapping(parameters = {
        @SnParameter(sn = ActionSn.syn_company_tax, taxOffice = TaxOffice.gs, cycleType = CycleType.m, area = AreaSn.shenzhen),
        @SnParameter(sn = ActionSn.syn_company_tax, taxOffice = TaxOffice.gs, cycleType = CycleType.q, area = AreaSn.shenzhen)
}
)
public class GetCompanyTaxOperation implements TaskStartHandler2, TaxRouterBuilder {
    private final static Logger logger = LoggerFactory.getLogger(GetCompanyTaxOperation.class);

    @Autowired
    private BizTaxCompanyTaxStartService bizTaxCompanyTaxStartService;
    @Autowired
    private BizTaxCompanyTaxService bizTaxCompanyTaxService;
    @Autowired
    private TaskSendHandler taskSendHandler;

    @Autowired
    private TaxRouterLoginHandler taxRouterLoginHandler;

    @Override
    public Object process(OperationContext context) {
        logger.info("查询公司信息开始{}", context.getBizMdCompany().getId());

        BizTaxCompanyTax bizTaxCompanyTax = bizTaxCompanyTaxService.findByCompanyId(context.getBizMdCompany().getId());
        if (null != bizTaxCompanyTax && BizTaxCompanyTax.ProcessState.process.equals(bizTaxCompanyTax.getProcessState())) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.TASK_NOT_ALLOWED_SEND, "任务正在办理中");
        }

        bizTaxCompanyTaxStartService.start(
                context.getBizMdCompany().getId(),
                context.getRequest().getUserId());

        logger.info("查询公司start信息结束{}", bizTaxCompanyTax);
        try {
            LocalDate localDate = LocalDate.now();
            String cycleSn = String.valueOf(this.cycle(localDate.getYear(), localDate.getMonthValue()) + "");
            BizMdAccountCycle bizMdAccountCycle = new BizMdAccountCycle();
            bizMdAccountCycle.setSn(cycleSn);
            context.setBizMdAccountCycle(bizMdAccountCycle);
            // TaskBO taskBO = taskSendHandler.sendToTask(context, this.build(context));
            taskSendHandler.send(context, this.build(context));
            bizTaxCompanyTaxStartService.success(context.getBizMdCompany().getId(),
                    context.getTaskSeq(),
                    context.getRequest().getUserId());
        } catch (BizException ex) {
            //todo 发起异常不处理【{"message":"任务还未执行完成，请勿重复发起。","status":500}】 更新后会变成exception
            bizTaxCompanyTaxStartService.exception(context.getBizMdCompany().getId(), ex.getCode() + "", ex.getMessage());
            throw ex;
        }
        return context;
    }


    public int cycle(int year, int month) {
        int d = 0;
        switch (month) {
            case 1:
            case 2:
            case 3:
                d = year * 100 + 4;
                break;
            case 4:
            case 5:
            case 6:
                d = year * 100 + 7;
                break;
            case 7:
            case 8:
            case 9:
                d = year * 100 + 10;
                break;
            case 10:
            case 11:
            case 12:
                d = year + 1 * 100 + 1;
                break;

        }
        return d;
    }


    @Override
    public TaxRouterRequest build(OperationContext context) {
        BizMdCompany bizMdCompany = context.getBizMdCompany();
        if (ActionSn.syn_company_tax.name().equals(context.getRequest().getActionSn().name())) {
            if (StringUtils.isNotEmpty(bizMdCompany) && StringUtils.isEmpty(bizMdCompany.getFullName())) {
                bizMdCompany.setFullName("同步客户资料公司为空");
            }
        }
        LoginInfo loginInfo = taxRouterLoginHandler.getCompanyDefaultAccount(
                bizMdCompany,
                context.getRequest().getTaxOffice().name(),
                context.getBizMdArea().getSn(),
                context.getRequest().getActionSn()
        );
        Map<String, Object> params = new HashMap();
        //税号
        params.put("taxNo", context.getBizMdCompany().getTaxNo());
        params.put("endaccountcyclesn", context.getBizMdAccountCycle().getSn());
        //是否校验公司名称(Y:是,N否)
        params.put("checkCompanyName", "N");
        TaxRouterRequest taxRouterRequest = new TaxRouterRequest();
        taxRouterRequest.setLoginInfo(loginInfo);
        taxRouterRequest.setParams(params);
        return taxRouterRequest;
    }

}
