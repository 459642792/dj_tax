package com.yun9.service.tax.core.report;

import com.yun9.biz.report.BizReportService;
import com.yun9.biz.report.domain.dto.ReportDataDTO;
import com.yun9.biz.report.domain.entity.BizReportInstance;
import com.yun9.biz.tax.BizTaxInstanceCategoryReportService;
import com.yun9.biz.tax.BizTaxInstanceService;
import com.yun9.biz.tax.BizTaxMdOfficeCategoryReportService;
import com.yun9.biz.tax.domain.entity.BizTaxInstance;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryReport;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategoryReport;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.task.helper.JsonMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by werewolf on  2018/6/1.
 */
@Component
public class ReportFactoryImpl implements ReportFactory {

    public final static Logger logger = LoggerFactory.getLogger(ReportFactory.class);

    @Autowired
    private List<IReportGenerate> iReportGenerates;

    private static Map<String, IReportGenerate> reportGenerates = new ConcurrentHashMap<>();

    @Autowired
    private BizTaxInstanceService bizTaxInstanceService;

    @Autowired
    private BizReportService bizReportService;

    @Autowired
    private BizTaxInstanceCategoryReportService bizTaxInstanceCategoryReportService;

    @Autowired
    private BizTaxMdOfficeCategoryReportService bizTaxMdOfficeCategoryReportService;


    @Override
    public void generate(BizTaxInstanceCategory bizTaxInstanceCategory, JsonMap body) {
        //获得当前税种sn
        List<BizTaxMdOfficeCategoryReport> bizTaxMdOfficeCategoryReport = bizTaxMdOfficeCategoryReportService.findByTaxMdOfficeCategoryId(bizTaxInstanceCategory.getBizTaxMdOfficeCategoryId());

        if (CollectionUtils.isEmpty(bizTaxMdOfficeCategoryReport)) {
            logger.error("没有找到税种[biz_md_tax_office_category]id{},对应报表配置", bizTaxInstanceCategory.getBizTaxMdOfficeCategoryId());
            return;
            //  throw ServiceTaxException.build(ServiceTaxException.Codes.task_callback_success_report_generate, "没有找到税种[biz_md_tax_office_category]id{" + bizTaxInstanceCategory.getBizTaxMdCategoryId() + "},对应报表配置");
        }
        //获得报表sn
        IReportGenerate reportGenerate = reportGenerates.get(bizTaxMdOfficeCategoryReport.get(0).getReportSn());
        if (null == reportGenerate) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.not_found_report_generate_handler, bizTaxMdOfficeCategoryReport.get(0).getReportSn());
        }

        BizTaxInstance bizTaxInstance = bizTaxInstanceCategory.getBizTaxInstance() == null ?
                bizTaxInstanceService.findById(bizTaxInstanceCategory.getBizTaxInstanceId()) : bizTaxInstanceCategory.getBizTaxInstance();
        bizTaxInstanceCategory.setBizTaxInstance(bizTaxInstance);

        //获得报表配置
        List<BizTaxMdOfficeCategoryReport> bizTaxReportProperties = bizTaxMdOfficeCategoryReportService.findByTaxMdOfficeCategoryId(bizTaxInstanceCategory.getBizTaxMdOfficeCategoryId());
        if (CollectionUtils.isEmpty(bizTaxReportProperties)) {
            logger.debug("没有找到税种对应的report");
            return;
        }

        Long itemId = null;
        BizTaxInstanceCategoryReport bizTaxInstanceCategoryReport = null;
        if (null != body && body.containsKey(ReportFactory.PARAM_REPORT_ITEM_ID)) {
            itemId = body.getLong(ReportFactory.PARAM_REPORT_ITEM_ID);
            bizTaxInstanceCategoryReport = bizTaxInstanceCategoryReportService.findByBizTaxInstanceCategoryIdAndItemId(bizTaxInstanceCategory.getId(), body.getLong(ReportFactory.PARAM_REPORT_ITEM_ID));
        } else {
            bizTaxInstanceCategoryReport = bizTaxInstanceCategoryReportService.findByTaxInstanceCategoryId(bizTaxInstanceCategory.getId());
        }
        if (reportGenerate.isResetCreate()) {
            if (null != bizTaxInstanceCategoryReport) {
                bizTaxInstanceCategoryReportService.disabled(bizTaxInstanceCategoryReport.getId());
                bizTaxInstanceCategoryReport = null;
            }
        }


        try {
            //生成报表
            Map<String, List<ReportDataDTO>> reportData = reportGenerate.generate(bizTaxInstanceCategory, body);
            BizReportInstance bizReportInstance = null;
            logger.debug("报表数据{}", reportData);
            if (null == bizTaxInstanceCategoryReport) {
                if (null == reportData) {
                    bizReportInstance = bizReportService.create(bizTaxReportProperties.get(0).getReportSn(), new HashMap() {{
                        put("createdBy", bizTaxInstanceCategory.getCreatedBy());
                        put("company", bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyName());
                    }});
                } else {

                    bizReportInstance = bizReportService.create(bizTaxReportProperties.get(0).getReportSn(), new HashMap() {{
                        put("createdBy", bizTaxInstanceCategory.getCreatedBy());
                        put("company", bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyName());
                    }}, reportData);
                }
                bizTaxInstanceCategoryReportService.create(bizTaxInstanceCategory.getId(), bizReportInstance.getId(), itemId);
            } else {
                if (null != reportData) {
                    bizReportService.createSheet(bizTaxInstanceCategoryReport.getBizReportInstanceId(), reportData, new HashMap() {{
                        put("processBy", bizTaxInstanceCategory.getCreatedBy());
                        put("company", bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyName());
                    }});
                }
            }
        } catch (ServiceTaxException ex) {
            logger.error("创建报表失败{}", ex.getMessage());
            throw ServiceTaxException.build(ServiceTaxException.Codes.ReportCreateException, ex.getMessage());
        }

    }


    @PostConstruct
    public void init() {
        iReportGenerates.forEach(v -> {
            ReportSnMapping snMapping = v.getClass().getAnnotation(ReportSnMapping.class);
            if (null != snMapping) {
                String[] sns = snMapping.sns();
                if (sns.length > 0) {
                    for (String sn : sns) {
                        reportGenerates.put(sn, v);
                    }
                }
            }

        });
    }

}
