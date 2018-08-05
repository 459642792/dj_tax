package com.yun9.service.tax.core.v2.ops;

import com.yun9.biz.tax.BizTaxInstanceCategoryFzItemService;
import com.yun9.biz.tax.BizTaxInstanceCategoryFzService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryFz;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryFzItem;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.ops.BizTaxDeclareService;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.ft.ops.TaxFzOperation;
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

/**
 * 发送附征税
 */
@ActionMapping(parameters = {
        @SnParameter(sn = ActionSn.send_fz_m, taxOffice = TaxOffice.ds, cycleType = CycleType.m, area = AreaSn.shenzhen),
        @SnParameter(sn = ActionSn.send_fz_q, taxOffice = TaxOffice.ds, cycleType = CycleType.q, area = AreaSn.shenzhen)
}
)
public class SendMQFzOperation implements TaskStartHandler2, TaxRouterBuilder {
    private final static Logger logger = LoggerFactory.getLogger(SendMQFzOperation.class);
    @Autowired
    TaskSendHandler taskSendHandler;
    @Autowired
    TaxRouterLoginHandler taxRouterLoginHandler;
    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;
    @Autowired
    BizTaxDeclareService bizTaxDeclareService;
    @Autowired
    BizTaxInstanceCategoryFzService bizTaxInstanceCategoryFzService;
    @Autowired
    BizTaxInstanceCategoryFzItemService bizTaxInstanceCategoryFzItemService;

    @Override
    public TaxRouterRequest build(OperationContext context) {
        //组织报表数据
        List reportDataList = new ArrayList();
        //查询税种
        BizTaxInstanceCategoryFz bizTaxInstanceCategoryFz = bizTaxInstanceCategoryFzService.findByBizTaxInstanceCategoryId(context.getBizTaxInstanceCategory().getId());
        if (null == bizTaxInstanceCategoryFz)
            throw ServiceTaxException.build(ServiceTaxException.Codes.declare_info_not_found, "没有找到该附征税申报税种");
        //查询税种item
        List<BizTaxInstanceCategoryFzItem> bizTaxInstanceCategoryFzItemList = bizTaxInstanceCategoryFzItemService.findByBizTaxInstanceCategoryFzId(bizTaxInstanceCategoryFz.getId());
        if (null == bizTaxInstanceCategoryFzItemList || bizTaxInstanceCategoryFzItemList.size() <= 0)
            throw ServiceTaxException.build(ServiceTaxException.Codes.declare_info_not_found, "没有找到该附征税申报表");
        bizTaxInstanceCategoryFzItemList.forEach(item -> {
            //校验
            TaxFzOperation.checkFzItemsData(item);
            Map<String, Object> reportData = new HashMap();
            reportData.put("a1", (item.getSaleAmountTotal().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            reportData.put("a2", item.getTaxRate().doubleValue());
            reportData.put("a3", (item.getTaxPayAmount().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            reportData.put("a4", Optional.ofNullable(item.getTaxRemitCode()).orElse(""));
            reportData.put("a5", (item.getTaxRemitAmount().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            reportData.put("a6", (item.getTaxAlreadyPayAmount().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            reportData.put("a7", (item.getTaxShouldPayAmount().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            reportData.put("taxsn", item.getItemDetailCode());
            reportData.put("s1", (item.getSaleAmountVatNormal().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            reportData.put("s2", (item.getSaleAmountVatFree().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            reportData.put("s3", (item.getSaleAmountSoq().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
            reportData.put("s4", (item.getSaleAmountBusiness().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
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
                put("report002", reportDataList);

            }});
            logger.debug("完成参数构建-->{}", this);
        }};

    }

    //数据格式化(number  0.00转成0)
    private static String doubleTrans(double num) {
        if (num % 1.0 == 0) {
            return String.valueOf((long) num);
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
            throw ServiceTaxException.build(ServiceTaxException.Codes.send_tax_state_error, String.format("流程状态 :%s 执行状态 :%s,审核状态为:%s 无法发起申报", context.getBizTaxInstanceCategory().getState().getName(), context.getBizTaxInstanceCategory().getProcessState().getName(), context.getBizTaxInstanceCategory().isAudit() ? "已审核" : "未审核"));
        }
        Optional.ofNullable(
                bizTaxInstanceCategoryFzService.findByBizTaxInstanceCategoryId(context.getBizTaxInstanceCategory().getId()))
                .orElseThrow(
                        () -> ServiceTaxException.build(ServiceTaxException.Codes.declare_info_not_found, "没有找到该附征税申报记录"));

        //=====================发起任务==============================================
        taskSendHandler.send(context, this.build(context));
        //====================调用申报发起成功========================================================
        bizTaxDeclareService.startDeclare(context.getBizTaxInstanceCategory(), context.getTaskSn(),
                context.getTaskInstanceId(), context.getTaskSeq(),
                context.getTaskBizId(), context.getRequest().getUserId(), null);
        return "任务发起成功";
    }
}
