package com.yun9.service.tax.core.v2.ops;

import com.yun9.biz.md.BizMdDictionaryCodeService;
import com.yun9.biz.md.domain.entity.BizMdDictionaryCode;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.BizTaxInstanceCategoryYhItemService;
import com.yun9.biz.tax.BizTaxInstanceCategoryYhService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryYh;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryYhItem;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.ops.BizTaxDeclareService;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.ft.ops.TaxYhOperation;
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

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 发送印花税
 */
@ActionMapping(parameters = {
        @SnParameter(sn = ActionSn.send_yh_m, taxOffice = TaxOffice.ds, cycleType = CycleType.m,area = AreaSn.shenzhen)
}
)
public class SendMYhOperation implements TaskStartHandler2, TaxRouterBuilder {
    private final static Logger logger = LoggerFactory.getLogger(SendMYhOperation.class);
    private final static Map<String, String> yhItemCodeHash = new ConcurrentHashMap<>();
    @Autowired
    TaskSendHandler taskSendHandler;
    @Autowired
    TaxRouterLoginHandler taxRouterLoginHandler;
    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;
    @Autowired
    BizTaxDeclareService bizTaxDeclareService;
    @Autowired
    BizTaxInstanceCategoryYhService bizTaxInstanceCategoryYhService;
    @Autowired
    BizMdDictionaryCodeService bizMdDictionaryCodeService;
    @Autowired
    BizTaxInstanceCategoryYhItemService bizTaxInstanceCategoryYhItemService;

    @Override
    public TaxRouterRequest build(OperationContext context) {
        //组织报表数据
        List reportDataList = new ArrayList();
        //查询税种
        BizTaxInstanceCategoryYh bizTaxInstanceCategoryYh = bizTaxInstanceCategoryYhService.findByInstanceCategoryId(context.getBizTaxInstanceCategory().getId());
        if (null == bizTaxInstanceCategoryYh) ServiceTaxException.build(ServiceTaxException.Codes.declare_info_not_found, "没有找到该印花税申报税种");

        //查询税种item
        List<BizTaxInstanceCategoryYhItem> bizTaxInstanceCategoryYhItemList = bizTaxInstanceCategoryYhItemService.findByBizTaxInstanceCategoryYhId(bizTaxInstanceCategoryYh.getId());
        if (null == bizTaxInstanceCategoryYhItemList || bizTaxInstanceCategoryYhItemList.size() <= 0 ) ServiceTaxException.build(ServiceTaxException.Codes.declare_info_not_found, "没有找到该印花税申报表");
        //查询控制编码
        List<BizMdDictionaryCode> bizMdDictionaryCodes = bizMdDictionaryCodeService.findByDefsn("yhitemcode");
        if (null == bizMdDictionaryCodes && bizMdDictionaryCodes.size() <= 0) ServiceTaxException.build(ServiceTaxException.Codes.md_code_not_found, "无法找到印花税项目的控制编码");
        bizMdDictionaryCodes.forEach(item-> {
            yhItemCodeHash.put(item.getSn(), item.getDefname());
        });
        bizTaxInstanceCategoryYhItemList.forEach(item-> {
            Map<String, Object> reportData = new HashMap();
            if (null == yhItemCodeHash.get(item.getItemCode())) ServiceTaxException.build(ServiceTaxException.Codes.md_code_not_found, "无法找到印花税项目<<" + item.getItemCode() + ">>的控制编码");
            TaxYhOperation.checkDataCount(item);
            reportData.put("a1", yhItemCodeHash.get(item.getItemCode()));
            reportData.put("b1", (item.getTaxBase().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            reportData.put("c1", (item.getApprovAmount().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            reportData.put("d1", (item.getApprovRate().doubleValue()));
            reportData.put("e1", (item.getTaxRate().toString()));
            reportData.put("f1", (item.getTaxPayAmount().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            reportData.put("g1", (item.getTaxAlreadyPayAmount().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            reportData.put("h1", Optional.ofNullable(item.getTaxRemitCode()).orElse(""));
            reportData.put("i1", (item.getTaxRemitAmount().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            reportData.put("j1", (item.getTaxShouldPayAmount().setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue()));
            reportData.put("taxsn", item.getItemCode());
            reportDataList.add(reportData);
        });
        return new TaxRouterRequest() {{
            logger.debug("开始构建税务路由参数：");
            setLoginInfo(taxRouterLoginHandler.getCompanyDefaultAccount(
                    context.getBizMdCompany(),
                    context.getRequest().getTaxOffice().name(),
                    context.getBizMdArea().getSn(),
                    context.getRequest().getActionSn()));
            setParams(new HashMap<String, Object>(10) {{
                put("taxclosingdate", context.getBizTaxInstanceCategory().getCloseDate() * 1000); //申报截止时间
                put("year", context.getBizMdAccountCycle().getYear()); //年
                put("month", context.getBizMdAccountCycle().getMonth()); //月
                put("startDate", context.getBizTaxInstanceCategory().getBeginDate() * 1000); //申报时间起
                put("endDate", context.getBizTaxInstanceCategory().getEndDate() * 1000); //申报时间止
                put("report001", reportDataList);

            }});
            logger.debug("完成参数构建-->{}", this);
        }};
    }

    //数据格式化(number  0.00转成0)
    private static String doubleTrans(double num)
    {
        if(num % 1.0 == 0)
        {
            return String.valueOf((long)num);
        }

        return String.valueOf(num);
    }

    @Override
    public Object process(OperationContext context) {

//        if (!context.getBizTaxInstanceCategory().inDeclareRangeTime()) {
//            throw ServiceTaxException.build(ServiceTaxException.Codes.send_tax_state_error, "该税种不在申报期");
//        }

        if (!context.getBizTaxInstanceCategory().prepare2Send()) {
            logger.error("申报状态错误",
                    context.getBizTaxInstanceCategory().getState().name(),
                    context.getBizTaxInstanceCategory().getProcessState().name());
            throw ServiceTaxException.build(ServiceTaxException.Codes.send_tax_state_error, String.format("流程状态 :%s 执行状态 :%s,审核状态为:%s 无法发起申报", context.getBizTaxInstanceCategory().getState().getName(), context.getBizTaxInstanceCategory().getProcessState().getName(),context.getBizTaxInstanceCategory().isAudit()?"已审核":"未审核"));
        }
        Optional.ofNullable(
                bizTaxInstanceCategoryYhService.findByInstanceCategoryId(context.getBizTaxInstanceCategory().getId()))
                .orElseThrow(
                        () -> ServiceTaxException.build(ServiceTaxException.Codes.declare_info_not_found, "没有找到该印花税申报记录"));

        //=====================发起任务==============================================
        taskSendHandler.send(context, this.build(context));
        //====================调用申报发起成功========================================================
        bizTaxDeclareService.startDeclare(context.getBizTaxInstanceCategory(), context.getTaskSn(),
                context.getTaskInstanceId(), context.getTaskSeq(),
                context.getTaskBizId(), context.getRequest().getUserId(), null);
        return "任务发起成功";
    }
}
