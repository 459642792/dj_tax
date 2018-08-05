package com.yun9.service.tax.core.v2.ops;

import com.yun9.biz.md.BizMdCompanyAccountService;
import com.yun9.biz.md.BizMdInstClientProcessService;
import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.entity.BizMdAccount;
import com.yun9.biz.task.domain.bo.TaskBO;
import com.yun9.biz.tax.domain.dto.BizTaxInitDTO;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.biz.tax.enums.TaxType;
import com.yun9.biz.tax.ops.BizTaxSingleStartService;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.taxrouter.request.LoginInfo;
import com.yun9.service.tax.core.taxrouter.request.TaxRouterRequest;
import com.yun9.service.tax.core.v2.OperationContext;
import com.yun9.service.tax.core.v2.annotation.ActionMapping;
import com.yun9.service.tax.core.v2.annotation.ActionSn;
import com.yun9.service.tax.core.v2.annotation.SnParameter;
import com.yun9.service.tax.core.v2.handler.TaskStartHandler;
import com.yun9.service.tax.core.v2.handler.TaxRouterBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 下载税种
 */
@ActionMapping(parameters = {
        @SnParameter(sn = ActionSn.get_fr_y, taxOffice = TaxOffice.ds, cycleType = CycleType.y),
        @SnParameter(sn = ActionSn.get_fr_y, taxOffice = TaxOffice.gs, cycleType = CycleType.y)
}
)
public class GetFrYOperation implements TaskStartHandler, TaxRouterBuilder {

    @Autowired(required = false)
    private BizTaxSingleStartService bizTaxSingleStartService;

    @Autowired
    BizMdInstClientProcessService bizMdInstClientProcessService;

    @Autowired
    BizMdCompanyAccountService bizMdCompanyAccountService;

    @Override
    public void begin(OperationContext context) {

        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxSingleStartService.start(TaxSn.y_fr,
                context.getRequest().getAccountCycleId(),
                context.getRequest().getTaxOffice(),
                context.getRequest().getCycleType(),
                context.getRequest().getUserId(),
                new BizTaxInitDTO()
                        .setCompanyId(context.getBizMdCompany().getId())
                        .setCompanyName(context.getBizMdCompany().getFullName())
                        .setTaxAreaId(context.getBizMdCompany().getTaxAreaId())
                        .setInstClientId(context.getBizMdInstClient().getId())
                        .setTaxType(TaxType.valueOf(context.getBizMdCompany().getTaxType().toString()))
                        .setClientSn(context.getBizMdInstClient().getSn())
        );
        context.setBizTaxInstanceCategory(bizTaxInstanceCategory);

        if (null == bizTaxInstanceCategory) {
            context.setBizTaxInstance(null);
            return;
        }

        context.setBizTaxInstance(bizTaxInstanceCategory.getBizTaxInstance());

        if (null != context.getBizTaxInstanceCategory() && null != context.getBizTaxInstance()) {
            bizMdInstClientProcessService.startByKey(
                    context.getBizMdCompany().getId(),
                    context.getBizMdInst().getId(),
                    new StringJoiner("_")
                            .add(context.getBizMdAccountCycle().getSn())
                            .add(context.getBizMdAccountCycle().getType().name())
                            .add(TaxSn.y_fr.name()).toString(),
                    context.getRequest().getUserId());
        }
    }


    @Override
    public void success(OperationContext context) {
        bizTaxSingleStartService.success(context.getBizTaxInstanceCategory().getId(),
                context.getTaskSn(),
                context.getTaskInstanceId(),
                context.getTaskSeq(),
                context.getTaskBizId(),
                context.getRequest().getUserId());
    }

    @Override
    public void exception(OperationContext context, Map<Integer, String> errors) {
        StringJoiner codes = new StringJoiner("|");
        StringJoiner messages = new StringJoiner("|");
        errors.forEach((k, v) -> {
            codes.add(String.valueOf(k));
            messages.add(v);
        });
        bizTaxSingleStartService.exception(context.getBizTaxInstanceCategory().getId(), codes.toString(), messages.toString());


    }


    @Override
    public TaxRouterRequest build(OperationContext context) {
        TaxRouterRequest taxRouterRequest = new TaxRouterRequest();
        CompanyAccountDTO companyAccountDTO = bizMdCompanyAccountService.findActivateByCompanyIdAndType(context.getBizMdCompany().getId(), BizMdAccount.Type.valueOf(context.getRequest().getTaxOffice().name()));

        if (companyAccountDTO.getLoginType() == BizMdAccount.LoginType.gsjump) {
            companyAccountDTO = bizMdCompanyAccountService.findActivateByCompanyIdAndType(context.getBizMdCompany().getId(), BizMdAccount.Type.gs);
        }
        if (null == companyAccountDTO) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.company_has_not_default_account);
        }
        context.setDefaultCompanyAccount(companyAccountDTO);
        LoginInfo loginInfo = LoginInfo.build(context.getDefaultCompanyAccount(), context.getBizMdCompany(), context.getBizMdArea());

        taxRouterRequest.setLoginInfo(loginInfo);
        taxRouterRequest.setParams(new HashMap() {{
            put("year", context.getBizMdAccountCycle().getYear());
            put("month", context.getBizMdAccountCycle().getCurrentMonth());
            put("cycleType", context.getRequest().getCycleType());
            put("taxCode", new String[]{});//下载说有税种
            put("needHistory", false);
        }});
        return taxRouterRequest;
    }


}
