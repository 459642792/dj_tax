package com.yun9.service.tax.core.v2.ops;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.report.BizReportService;
import com.yun9.biz.report.domain.entity.BizReportInstanceSheet;
import com.yun9.biz.report.domain.entity.BizReportInstanceSheetData;
import com.yun9.biz.tax.*;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryReport;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategoryReport;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.ops.BizTaxDeclareService;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.ft.TaxMdCategoryHandler;
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
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 个税个体经营
 *
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-06-11 18:02
 */
@ActionMapping(parameters = {
        @SnParameter(sn = ActionSn.send_personal_business, taxOffice = TaxOffice.ds, cycleType = CycleType.m)
}
)
public class SendPersonalBusinessOperation extends AbstractTaxRouterBuilder implements TaskStartHandler2 {

    private final static Logger logger = LoggerFactory.getLogger(SendPersonalBusinessOperation.class);

    @Autowired
    TaskSendHandler taskSendHandler;

    @Autowired
    BizTaxDeclareService bizTaxDeclareService;

    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    TaxMdCategoryHandler taxMdCategoryHandler;

    @Autowired
    BizTaxInstanceCategoryPersonalBusinessService bizTaxInstanceCategoryPersonalBusinessService;

    @Autowired
    BizTaxInstanceCategoryPersonalBusinessItemService bizTaxInstanceCategoryPersonalBusinessItemService;

    @Autowired
    BizTaxMdOfficeCategoryReportService bizTaxMdOfficeCategoryReportService;

    @Autowired
    BizReportService bizReportService;

    @Autowired
    BizTaxInstanceCategoryReportService bizTaxInstanceCategoryReportService;

    @Autowired
    TaxRouterTaxCategoryHelper taxRouterTaxCategoryHelper;

    @Override
    protected Map<String, Object> buildParams(OperationContext context) {

        //开始获取报表数据
        logger.debug("开始查询税种报表实例对应数据");
        BizTaxInstanceCategoryReport bizTaxInstanceCategoryReport = bizTaxInstanceCategoryReportService
                .findByTaxInstanceCategoryId(context.getRequest().getTaxInstanceCategoryId());
        logger.debug("bizTaxInstanceCategoryReport -->{}", bizTaxInstanceCategoryReport);
        logger.debug("开始获取报表数据");
        Map<BizReportInstanceSheet, BizReportInstanceSheetData> reportDateMap = bizReportService
                .findByBizReportInstanceId(bizTaxInstanceCategoryReport.getBizReportInstanceId());
        logger.debug("报表数据--->{}", reportDateMap);


        Map<String,Object> rst = new HashMap<String, Object>(11) {{
            put("year", context.getBizMdAccountCycle().getYear());
            put("month", context.getBizMdAccountCycle().getMonth());
            put("startDate", context.getBizTaxInstanceCategory().getBeginDate() * 1000);
            put("endDate", context.getBizTaxInstanceCategory().getEndDate() * 1000);
            put("currStartDate", context.getBizTaxInstanceCategory().getStartDate() * 1000);
            put("currEndDate", context.getBizTaxInstanceCategory().getDeclareDate() * 1000);
            put("personals", buildDatas(reportDateMap, context.getBizTaxInstanceCategory().getBizTaxMdOfficeCategoryId()));
        }};
        logger.info("-->发送到税务路由{}", JSON.toJSON(rst));

        return rst;
    }

    @Override
    public Object process(OperationContext context) {

        //================申报实例申报前状态校验==========================
        logger.debug("----开始进行申报前的状态检查---");
        if (!context.getBizTaxInstanceCategory().prepare2Send()) {
            logger.error("申报状态错误",
                    context.getBizTaxInstanceCategory().getState().name(),
                    context.getBizTaxInstanceCategory().getProcessState().name());
            throw ServiceTaxException.build(ServiceTaxException.Codes.send_tax_state_error, String.format("流程状态 :%s 执行状态 :%s,审核状态为:%s 无法发起申报", context.getBizTaxInstanceCategory().getState().getName(), context.getBizTaxInstanceCategory().getProcessState().getName(),context.getBizTaxInstanceCategory().isAudit()?"已审核":"未审核"));
        }

        if (!taxRouterTaxCategoryHelper.isInDeclareRangeTime(context.getBizTaxInstanceCategory())) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.send_tax_state_error, "该税种不在申报期");
        }

        Optional.ofNullable(
                bizTaxInstanceCategoryPersonalBusinessService
                        .findByInstanceCategoryId(context.getBizTaxInstanceCategory().getId()))
                .orElseThrow(
                        () -> ServiceTaxException.build(ServiceTaxException.Codes.declare_info_not_found, "没有找到该个税个体经营申报记录"));

        //添加判断


        //=====================发起任务==============================================
        taskSendHandler.send(context, this.build(context));
        //====================调用申报发起成功========================================================
        bizTaxDeclareService.startDeclare(context.getBizTaxInstanceCategory(), context.getTaskSn(),
                context.getTaskInstanceId(), context.getTaskSeq(),
                context.getTaskBizId(), context.getRequest().getUserId(), null);
        return "任务发起成功";
    }


    private Map<String, Object> buildDatas(
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
                    throw ServiceTaxException.build(ServiceTaxException.Codes.tax_router_config_not_found, "报表配置信息未找到");
                }
                Map<String, String> config = new HashMap(categoryReportPropertyConf.size()) {{
                    categoryReportPropertyConf.forEach(v -> {
                        put(v.getTaxRouterSheetSn(), v.getReportSheetSn());
                    });
                }};
                logger.debug("config--->{}", config);
                logger.debug("开始组装数据---");


                Map<String, Object> reportData = new HashMap<String, Object>((int) (reportDateMap.size() / 0.75) + 1) {{
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
