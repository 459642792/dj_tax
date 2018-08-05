package com.yun9.service.tax.core.v2.ops;

import static com.yun9.service.tax.core.exception.ServiceTaxException.Codes.TASK_NOT_ALLOWED_SEND;
import static com.yun9.service.tax.core.exception.ServiceTaxException.Codes.tax_router_config_not_found;

import com.yun9.biz.md.BizMdCompanyAccountService;
import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.entity.BizMdAccount;
import com.yun9.biz.report.BizReportService;
import com.yun9.biz.report.domain.entity.BizReportInstanceSheet;
import com.yun9.biz.report.domain.entity.BizReportInstanceSheetData;
import com.yun9.biz.tax.BizTaxInstanceCategoryFrService;
import com.yun9.biz.tax.BizTaxInstanceCategoryReportService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.BizTaxMdOfficeCategoryReportService;
import com.yun9.biz.tax.BizTaxPropertiesService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryFr;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryReport;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategoryReport;
import com.yun9.biz.tax.domain.entity.properties.BizTaxProperties;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.ops.BizTaxDeclareService;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.v2.OperationContext;
import com.yun9.service.tax.core.v2.annotation.ActionMapping;
import com.yun9.service.tax.core.v2.annotation.ActionSn;
import com.yun9.service.tax.core.v2.annotation.SnParameter;
import com.yun9.service.tax.core.v2.handler.AbstractTaxRouterBuilder;
import com.yun9.service.tax.core.v2.handler.TaskSendHandler;
import com.yun9.service.tax.core.v2.handler.TaskStartHandler2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by werewolf on  2018/5/7.
 */
@ActionMapping(parameters = {
        @SnParameter(sn = ActionSn.send_fr_y, taxOffice = TaxOffice.ds, cycleType = CycleType.y),
        @SnParameter(sn = ActionSn.send_fr_y, taxOffice = TaxOffice.gs, cycleType = CycleType.y),
        @SnParameter(sn = ActionSn.send_fr_q, taxOffice = TaxOffice.gs, cycleType = CycleType.q)
}
)
public class SendFrYOperation extends AbstractTaxRouterBuilder implements TaskStartHandler2 {

    private static final Logger logger = LoggerFactory.getLogger(SendFrYOperation.class);

    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;
    @Autowired
    BizMdCompanyAccountService bizMdCompanyAccountService;
    @Autowired
    BizTaxPropertiesService bizTaxPropertiesService;
    @Autowired
    BizTaxInstanceCategoryReportService bizTaxInstanceCategoryReportService;
    @Autowired
    BizReportService bizReportService;
    @Autowired
    BizTaxDeclareService bizTaxDeclareService;
    @Autowired
    BizTaxInstanceCategoryFrService bizTaxInstanceCategoryFrService;
    @Autowired
    BizTaxMdOfficeCategoryReportService bizTaxMdOfficeCategoryReportService;
    @Autowired
    TaskSendHandler taskSendHandler;


    @Override
    protected Map<String, Object> buildParams(OperationContext context) {

        //=========================申报前置校验==============================
        BizTaxProperties propertiesAllowTaxRecord = bizTaxPropertiesService
                .findByKey(BizTaxProperties.KEY_ALLOW_TAX_RECORD);
        logger.info("全局配置中是否允许不备案申报------->{}", propertiesAllowTaxRecord);
        BizTaxProperties propertiesAllowZero = bizTaxPropertiesService
                .findByKey(BizTaxProperties.KEY_ALLOW_ZERO);

        logger.info("全局配置中是否允许0申报------->{}", propertiesAllowZero);

        //判断备案状态
        if (null != propertiesAllowTaxRecord && propertiesAllowTaxRecord.getValue().equals("N")) {
            logger.info("当前全局配置不允许不备案申报");
            BizTaxInstanceCategoryFr bizTaxInstanceCategoryFr = bizTaxInstanceCategoryFrService
                    .findByBizTaxInstanceCategoryId(context.getRequest().getTaxInstanceCategoryId());
            logger.info("准备申报的财报实例-->{}", bizTaxInstanceCategoryFr);
            if (bizTaxInstanceCategoryFr.getTaxOfficeFrType()
                    == BizTaxInstanceCategoryFr.FrType.none) {
                throw ServiceTaxException.build(TASK_NOT_ALLOWED_SEND, "系统不允许未备案情况申报财务报表");
            }
        }

        BizTaxInstanceCategoryReport bizTaxInstanceCategoryReport = bizTaxInstanceCategoryReportService
                .findByTaxInstanceCategoryId(context.getBizTaxInstanceCategory().getId());
        if (bizTaxInstanceCategoryReport == null) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.report_data_not_found);
        }

        //获取配置
        final Long bizTaxMdOfficeCategoryId = context.getBizTaxInstanceCategory()
                .getBizTaxMdOfficeCategoryId();
        List<BizTaxMdOfficeCategoryReport> categoryReportPropertyConf = bizTaxMdOfficeCategoryReportService
                .findByTaxMdOfficeCategoryId(bizTaxMdOfficeCategoryId);

        if (CollectionUtils.isEmpty(categoryReportPropertyConf)) {
            throw ServiceTaxException
                    .build(tax_router_config_not_found, "bizTaxMdOfficeCategoryReports");
        }
        Map<String, String> config = new HashMap() {{
            categoryReportPropertyConf.forEach(v -> {
                put(v.getReportSheetSn(), v.getTaxRouterSheetSn());
            });
        }};
        //组装report
        Map<BizReportInstanceSheet, BizReportInstanceSheetData> sheetListMap = bizReportService
                .findByBizReportInstanceId(bizTaxInstanceCategoryReport.getBizReportInstanceId());

        List<Map<String, Object>> report = new ArrayList<>(sheetListMap.size());
        sheetListMap.forEach((k, v) -> {
            Map<String, Object> map = new HashMap<>();
            //开始寻找智财税的配置的税务路由的sheetSn
            final String sheetSn = k.getSheetSn();
            logger.info("开始在智财税中寻找配置的税务路由报表sheetSn 传入参数sheetSn->{}", sheetSn);
            map.put("sheetSn", config.get(sheetSn));
            List<Map<String, Object>> data = new ArrayList<>();
            v.getDatas().forEach((a, e) -> {
                data.add(new HashMap() {{
                    put("key", a);
                    put("value", e);
                }});
            });
            map.put("data", data);
            report.add(map);
        });

        BizTaxInstanceCategoryFr frY = bizTaxInstanceCategoryFrService
                .findByBizTaxInstanceCategoryId(context.getBizTaxInstanceCategory().getId());

        return new HashMap(10) {{
            put("year", context.getBizMdAccountCycle().getYear());
            put("month", context.getBizMdAccountCycle().getCurrentMonth());
            put("cycleType", context.getRequest().getCycleType());
            put("reportType", frY.getFrType());
            put("reports", report);
            put("recordCheck",
                    Objects.equals(propertiesAllowTaxRecord.getValue(), "Y") ? "N"
                            : "Y");
            put("allowZero",
                    Objects.equals(propertiesAllowZero.getValue(), "normal") ? "N"
                            : propertiesAllowZero.getValue());
        }};
    }


    @Override
    public Object process(OperationContext context) {


        if (!context.getBizTaxInstanceCategory().prepare2Send()) {
            throw ServiceTaxException
                    .build(ServiceTaxException.Codes.send_tax_state_error, String.format("流程状态 :%s 执行状态 :%s,审核状态为:%s 无法发起申报", context.getBizTaxInstanceCategory().getState().getName(), context.getBizTaxInstanceCategory().getProcessState().getName(),context.getBizTaxInstanceCategory().isAudit()?"已审核":"未审核"));
        }
        taskSendHandler.send(context, super.build(context));
        return "发起财务报表年报成功";
    }
}
