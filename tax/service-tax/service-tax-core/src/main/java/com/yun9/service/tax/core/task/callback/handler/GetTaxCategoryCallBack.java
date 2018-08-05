package com.yun9.service.tax.core.task.callback.handler;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.report.BizReportService;
import com.yun9.biz.report.domain.dto.ReportDataDTO;
import com.yun9.biz.report.domain.entity.BizReportInstance;
import com.yun9.biz.tax.BizTaxInstanceCategoryReportService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.BizTaxMdOfficeCategoryReportService;
import com.yun9.biz.tax.BizTaxMdOfficeCategoryService;
import com.yun9.biz.tax.domain.dto.tax.DeclareTaxCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategory;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategoryReport;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.biz.tax.ops.BizTaxSingleStartService;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.JsonUtils;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.task.annotation.TaskSnMapping;
import com.yun9.service.tax.core.task.callback.TaskCallBackContext;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static com.yun9.service.tax.core.exception.ServiceTaxException.Codes.CallbackProcessFailed;

/**
 * Created by werewolf on  2018/5/7.
 * // * GD_GS_DECLARE_LIST("GD0015", "国税-获取税种"),
 * // * GD_DS_DECLARE_LIST("GD0013", "地税-获取税种"),
 * // * SZ_DS_DECLARE_LIST("SZ0013", "地税-获取税种"),
 * // * SZ_GS_DECLARE_LIST("SZ0015", "国税-获取税种"),
 */
@TaskSnMapping(sns = {"SZ0031", "SZ0039"})
public class GetTaxCategoryCallBack extends AbstractCallbackHandlerMapping {
    private static final Logger logger = LoggerFactory.getLogger(GetTaxCategoryCallBack.class);

    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;
    @Autowired
    BizTaxInstanceCategoryService BizTaxMdCategoryService;

    @Autowired
    BizTaxSingleStartService bizTaxSingleStartService;

    @Autowired
    BizReportService bizReportService;

    @Autowired
    BizTaxInstanceCategoryReportService bizTaxInstanceCategoryReportService;

    @Autowired
    BizTaxMdOfficeCategoryReportService BizTaxMdOfficeCategoryReportService;


    @Autowired
    BizTaxMdOfficeCategoryService bizTaxMdOfficeCategoryService;


    @Override
    public TaskCallBackContext process(TaskCallBackContext context) {
        String body = context.getTaskCallBackResponse().getBody();


        final BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService.findById(context.getBizTaxInstanceSeq().getBizTaxInstanceCategoryId());

        if (null == bizTaxInstanceCategory) {
            logger.error("没有找到下载税种实列seq:{}", context.getBizTaxInstanceSeq().getSeq());
            return context;
        }

        if (!bizTaxInstanceCategory.getState().equals(BizTaxInstanceCategory.State.start)) {
            logger.error("税种实例category{}状态不正常:{}", bizTaxInstanceCategory.getId(), bizTaxInstanceCategory.getState());
            return context;
        }
        DeclareList declareList = JsonUtils.parseObject(body, DeclareList.class);

        List<DeclareTaxCategory> _list = new ArrayList<>();
        declareList.getTaxCategories().forEach(v -> {
            DeclareTaxCategory response = new DeclareTaxCategory();
            response.setCloseDate(v.getCloseDate() / 1000);
            response.setStartDate(v.getStartDate() / 1000);
            response.setEndDate(v.getEndDate() / 1000);
            response.setTaxCode(v.getTaxCode());
            response.setName(v.getName());
            response.setTaxCustomData(v.getTaxCustomData());
            response.setTaxPaySn(v.getTaxPaySn());
            response.setDeclared(v.getAlreadyDeclared().equals("Y"));
            _list.add(response);
        });


        //回调处理
        final BizTaxInstanceCategory _bizTaxInstanceCategory = bizTaxSingleStartService.callback(
                bizTaxInstanceCategory.getId(),
                _list, declareList.getBaseClientInfo(),
                context.getBizTaxInstanceSeq().getSeq());


        if (null == _bizTaxInstanceCategory || null == _bizTaxInstanceCategory.getBizTaxMdOfficeCategoryId()) {
            logger.debug("当前没有下载到指定税种,可能是code不匹配");
            return context;
        }


        //获得报表配置
        List<BizTaxMdOfficeCategoryReport> bizTaxReportProperties = BizTaxMdOfficeCategoryReportService.findByTaxMdOfficeCategoryId(_bizTaxInstanceCategory.getBizTaxMdOfficeCategoryId());


        if (CollectionUtils.isEmpty(bizTaxReportProperties)) {
            logger.debug("没有找到税种对应的report");
            return context;
        }


        BizReportInstance bizReportInstance = null;

        try {
            //创建报表
            bizReportInstance = bizReportService.create(bizTaxReportProperties.get(0).getReportSn(), new HashMap() {{
                put("createdBy", _bizTaxInstanceCategory.getCreatedBy());
                put("company", bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyName());
            }});
            //获得下载税种的taxCode
            BizTaxMdOfficeCategory bizTaxMdOfficeCategory = bizTaxMdOfficeCategoryService.findById(_bizTaxInstanceCategory.getBizTaxMdOfficeCategoryId());

            //下载数据
            TaxCategory taxCategory = declareList.getTaxCategories().stream().filter(v -> v.getTaxCode().equals(bizTaxMdOfficeCategory.getCode())).findFirst().orElse(null);


            if (null != taxCategory && CollectionUtils.isNotEmpty(taxCategory.getDeclareData())) {
                logger.debug("---保存报表数据---{}", taxCategory.getTaxCode());
                Map<String, String> sheetSnConvert = new HashMap<>();

                bizTaxReportProperties.forEach(v -> {
                    sheetSnConvert.put(v.getTaxRouterSheetSn(), v.getReportSheetSn());
                });

                Map<String, List<ReportDataDTO>> reportData = new HashMap() {{
                    taxCategory.getDeclareData().forEach(v -> {
                        put(Optional.ofNullable(sheetSnConvert.get(v.getSheetSn()))
                                .orElseThrow(() -> ServiceTaxException.build(CallbackProcessFailed, "回调成功报表报表数据失败,税务路由报表sn" + v.getSheetSn() + "没有配置")), v.data);
                    });
                }};

                bizReportService.createSheet(bizReportInstance.getId(), reportData, new HashMap() {{
                    put("processBy", _bizTaxInstanceCategory.getCreatedBy());
                    put("company", bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyName());
                }});
                logger.debug("------保存报表数据完成{}-------", JSON.toJSONString(reportData.keySet()));
            }
        } catch (BizTaxException ex) {
            //创建报表 失败 修改回调为失败
            logger.error("创建报表发生异常{}", ex.getMessage());
            throw ServiceTaxException.build(CallbackProcessFailed, "回调成功创建报表失败" + ex.getMessage());
        }


        if (null == bizReportInstance) {
            throw ServiceTaxException.build(CallbackProcessFailed, "回调成功创建报表失败");
        }

        bizTaxInstanceCategoryReportService.create(_bizTaxInstanceCategory.getId(), bizReportInstance.getId());
        return context;
    }


    /**
     * 申报类别
     */
    @Data
    public static class DeclareList {
        private List<TaxCategory> taxCategories;//税种信息
        private Map<String, Object> baseClientInfo;//其他信息
    }

    /**
     * 下载具体税种
     */
    @Data
    public static class TaxCategory {
        private long closeDate;
        private long startDate;
        private long endDate;
        private String name;
        private String taxCode;
        private String alreadyDeclared;//是否申报
        private String taxPaySn;
        private Map<String, String> taxCustomData; // taxRecord:Y 是否备案，taxRecordName:备案名称小企业会计准则
        private List<DeclareData> declareData; //已经申报表的会返回数据
    }

    @Data
    public static class DeclareData {
        private String sheetSn;
        private List<ReportDataDTO> data;
    }


//    @Data
//    public static class ReportDataDTO {
//        private String key;
//        private String value;
//    }

}
