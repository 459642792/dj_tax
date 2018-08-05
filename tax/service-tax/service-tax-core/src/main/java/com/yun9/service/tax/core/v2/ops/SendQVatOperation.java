package com.yun9.service.tax.core.v2.ops;

import com.yun9.biz.report.BizReportService;
import com.yun9.biz.report.domain.entity.BizReportInstanceSheet;
import com.yun9.biz.report.domain.entity.BizReportInstanceSheetData;
import com.yun9.biz.tax.BizTaxInstanceCategoryReportService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.BizTaxInstanceCategoryVatSmallService;
import com.yun9.biz.tax.BizTaxMdOfficeCategoryReportService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryReport;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategoryReport;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.ops.BizTaxDeclareService;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.exception.ServiceTaxException.Codes;
import com.yun9.service.tax.core.ft.TaxMdCategoryHandler;
import com.yun9.service.tax.core.v2.OperationContext;
import com.yun9.service.tax.core.v2.annotation.ActionMapping;
import com.yun9.service.tax.core.v2.annotation.ActionSn;
import com.yun9.service.tax.core.v2.annotation.AreaSn;
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
import java.util.Optional;

import static com.yun9.service.tax.core.exception.ServiceTaxException.Codes.tax_router_config_not_found;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-05-21 16:24
 */
@ActionMapping(parameters = {
        @SnParameter(sn = ActionSn.send_vat_q, taxOffice = TaxOffice.gs, cycleType = CycleType.q, area = AreaSn.shenzhen)
}
)
public class SendQVatOperation extends AbstractTaxRouterBuilder implements TaskStartHandler2 {

    private final static Logger logger = LoggerFactory.getLogger(SendQVatOperation.class);
    @Autowired
    BizTaxInstanceCategoryReportService bizTaxInstanceCategoryReportService;
    @Autowired
    BizReportService bizReportService;

    @Autowired
    BizTaxMdOfficeCategoryReportService bizTaxMdOfficeCategoryReportService;
    @Autowired
    TaskSendHandler taskSendHandler;
    @Autowired
    BizTaxInstanceCategoryVatSmallService bizTaxInstanceCategoryVatSmallService;
    @Autowired
    BizTaxDeclareService bizTaxDeclareService;
    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;
    @Autowired
    TaxMdCategoryHandler taxMdCategoryHandler;
    @Autowired
    TaxRouterTaxCategoryHelper taxRouterTaxCategoryHelper;

    @Override
    protected Map<String, Object> buildParams(OperationContext context) {
        //开始获取报表数据
        logger.debug("开始查询税种报表实例对应数据");
        BizTaxInstanceCategoryReport bizTaxInstanceCategoryReport = Optional.ofNullable(bizTaxInstanceCategoryReportService
                .findByTaxInstanceCategoryId(context.getRequest().getTaxInstanceCategoryId())).orElseThrow(() ->
                ServiceTaxException.build(Codes.declare_info_not_found, "报表相关信息未找到"));

        logger.debug("bizTaxInstanceCategoryReport -->{}", bizTaxInstanceCategoryReport);

        //==================报表系统必须是已审核=========================================
        if (!bizReportService.check(bizTaxInstanceCategoryReport.getBizReportInstanceId())) {
            throw ServiceTaxException.build(Codes.task_start_faied, "报表信息未确认");
        }

        logger.debug("开始获取报表数据");
        Map<BizReportInstanceSheet, BizReportInstanceSheetData> reportDateMap = bizReportService
                .findByBizReportInstanceId(bizTaxInstanceCategoryReport.getBizReportInstanceId());
        logger.debug("报表数据-->{}", reportDateMap);
        return new HashMap<String, Object>(11) {{
            put("year", context.getBizMdAccountCycle().getYear());
            put("month", context.getBizMdAccountCycle().getMonth());
            put("startDate", context.getBizTaxInstanceCategory().getBeginDate() * 1000);
            put("endDate", context.getBizTaxInstanceCategory().getEndDate() * 1000);
            putAll(reportDataAssemble(reportDateMap,
                    context.getBizTaxInstanceCategory().getBizTaxMdOfficeCategoryId()));

        }};
    }


    @Override
    public Object process(OperationContext context) {
        logger.debug("传入的参数 -->{}", context.getRequest());
        context.setBizTaxInstanceCategory(Optional.ofNullable(
                bizTaxInstanceCategoryService.findById(context.getRequest().getTaxInstanceCategoryId()))
                .orElseThrow(
                        () -> ServiceTaxException.build(Codes.declare_info_not_found, "税种申报实例信息未找到")));
        //================申报实例申报前状态校验==========================
        logger.debug("----开始进行申报前的状态检查---");
        if (!context.getBizTaxInstanceCategory().prepare2Send()) {
            logger.error("申报状态错误",
                    context.getBizTaxInstanceCategory().getState().name(),
                    context.getBizTaxInstanceCategory().getProcessState().name());
            throw ServiceTaxException
                    .build(Codes.send_tax_state_error, String.format("流程状态 :%s 执行状态 :%s,审核状态为:%s 无法发起申报", context.getBizTaxInstanceCategory().getState().getName(), context.getBizTaxInstanceCategory().getProcessState().getName(), context.getBizTaxInstanceCategory().isAudit() ? "已审核" : "未审核"));
        }
        if (!taxRouterTaxCategoryHelper.isInDeclareRangeTime(context.getBizTaxInstanceCategory())) {
            throw ServiceTaxException.build(Codes.send_tax_state_error, "该税种不在申报期");
        }
        Optional.ofNullable(
                bizTaxInstanceCategoryVatSmallService
                        .findByBizTaxInstanceCategoryId(context.getBizTaxInstanceCategory().getId()))
                .orElseThrow(
                        () -> ServiceTaxException.build(Codes.declare_info_not_found, "没有找到该增值税申报记录"));

        //=====================发起任务==============================================
        taskSendHandler.send(context, super.defaultAccountAndParams(context));
        //====================调用申报发起成功========================================================
        bizTaxDeclareService.startDeclare(context.getBizTaxInstanceCategory(), context.getTaskSn(),
                context.getTaskInstanceId(), context.getTaskSeq(),
                context.getTaskBizId(), context.getRequest().getUserId(), null);

        return "任务发起成功";
    }

    private Map<String, Object> reportDataAssemble(
            final Map<BizReportInstanceSheet, BizReportInstanceSheetData> reportDateMap,
            final Long bizTaxMdOfficeCategoryId) {

        return new HashMap<String, Object>(5) {
            {
                //获取配置
                logger.debug("开始获取bizTaxMdOfficeCategoryId--{}的报表配置信息");
                List<BizTaxMdOfficeCategoryReport> categoryReportPropertyConf = bizTaxMdOfficeCategoryReportService
                        .findByTaxMdOfficeCategoryId(bizTaxMdOfficeCategoryId);
                logger.debug("categoryReportPropertyConf-->{}", categoryReportPropertyConf);
                if (CollectionUtils.isEmpty(categoryReportPropertyConf)) {
                    throw ServiceTaxException.build(tax_router_config_not_found, "报表配置信息未找到");
                }
                Map<String, String> config = new HashMap(categoryReportPropertyConf.size()) {{
                    categoryReportPropertyConf.forEach(v -> {
                        put(v.getTaxRouterSheetSn(), v.getReportSheetSn());
                    });
                }};
                logger.debug("config--->{}", config);
                logger.debug("开始组装数据---");

//                Map<String, Map<String, String>> reportData = new HashMap<String, Map<String, String>>(reportDateMap.size()) {{
                Map<String, Object> reportData = new HashMap<String, Object>(
                        (int) (reportDateMap.size() / 0.75) + 1) {{
                    reportDateMap.forEach((k, v) -> {
                        put(k.getSheetSn(), v.getDatas());
                    });
                }};
                put("report001", reportData.get(config.get("report001")));
                put("report002", reportData.get(config.get("report002")));
                put("report003", reportData.get(config.get("report003")));
                logger.debug("组装完成-->{}", this);

            }
        };
    }

}
