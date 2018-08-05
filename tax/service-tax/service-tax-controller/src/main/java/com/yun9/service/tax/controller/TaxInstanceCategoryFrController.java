package com.yun9.service.tax.controller;

import com.yun9.biz.report.BizReportService;
import com.yun9.biz.report.domain.entity.BizReportInstance;
import com.yun9.biz.report.domain.entity.BizReportInstanceSheet;
import com.yun9.biz.report.domain.entity.BizReportInstanceSheetData;
import com.yun9.biz.tax.*;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryFr;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryReport;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdMsgCode;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategory;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategoryReport;
import com.yun9.biz.tax.domain.entity.properties.BizTaxProperties;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.enums.TaxType;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.framework.web.commons.Pageable;
import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.PageParam;
import com.yun9.framework.web.commons.annotation.QueryParam;
import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.TaxInstanceCategoryFryFactory;
import com.yun9.service.tax.core.impl.PageCommon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by werewolf on  2018/5/8.
 */
@Controller
@RequestMapping("/instance/category/fr") //todo
public class TaxInstanceCategoryFrController {

    @Autowired
    BizTaxInstanceCategoryFrService bizTaxInstanceCategoryFrService;

    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    BizTaxMdOfficeCategoryService bizTaxMdOfficeCategoryService;

    @Autowired
    BizTaxMdOfficeCategoryPropertyService bizTaxMdOfficeCategoryPropertyService;
    @Autowired
    BizTaxMdOfficeCategoryReportService BizTaxMdOfficeCategoryReportService;

    @Autowired
    private BizReportService bizReportService;

    @Autowired
    BizTaxInstanceCategoryReportService bizTaxInstanceCategoryReportService;

    @Autowired
    BizTaxPropertiesService bizTaxPropertiesService;

    @Autowired
    TaxInstanceCategoryFryFactory taxInstanceCategoryFryFactory;

    /**
     * 企业说的税A列表
     *
     * @param clientOrgTreeId
     * @param state
     * @param query           查询条件
     * @param pageable        分页参数
     * @return
     */
    @GetMapping("/list/{clientOrgTreeId}/{state}")
    @ResponseBody
    public Pagination<HashMap> list(@PathVariable long clientOrgTreeId,
                                    @PathVariable BizTaxInstanceCategory.State state,
                                    @QueryParam QueryJson query, @PageParam Pageable pageable) {

        List<Long> accountCycleIds = Arrays.asList(query.getLongArray("accountCycleIds")
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误,没有会计区间")));


        Map<String, Object> params = new HashMap() {{
            put("clientSn", query.getString("clientSn").orElse(null));
            put("companyName", query.getString("companyName").orElse(null));
            put("taxAreaId", query.getLong("taxAreaId").orElse(null));//税区
            put("taxType", query.getString("taxType").orElse(null));//纳税方式[small小规模][normal一般纳税人][personal]个体户'
            put("taxOffice", query.getString("taxOffice").orElse(null)); //税局 [gs ds]
            put("processCodeId", query.getString("processCodeId").orElse(null));//办理状态Id
            put("processState", query.getString("processState").orElse(null));//办理状态[none未办理][process办理中][success完成]
            put("audit", query.getLong("audit").orElse(null));//审核
            put("id", query.getString("id").orElse(null));//附加税id
            put("taxPayAmount", query.getString("taxPayAmount").orElse(null));//应纳税额度（系统）
            put("realPayAmount", query.getString("realPayAmount").orElse(null));//应纳税额度（税局）
            put("declareType", query.getString("declareType").orElse(null));//申报方式

            //todo 标签
            put("taxAuditType", query.getString("taxAuditType").orElse(null));//税局是否启用
            put("taxDeclareType", query.getString("taxDeclareType").orElse(null));//税额状态
            put("instClientStateType", query.getString("instClientStateType").orElse(null));//客户停止服务
        }};

        params = PageCommon.label2params(params);


        return taxInstanceCategoryFryFactory.pageByState(accountCycleIds, clientOrgTreeId, state, pageable.getPage(), pageable.getLimit(), params);
    }


    /**
     * 统计当前状态
     *
     * @param instId
     * @param accountCycleId
     * @param clientOrgTreeId
     * @param state
     * @param search
     * @return
     */
    @GetMapping("/total/{instId}/{accountCycleId}/{clientOrgTreeId}/{state}")
    @ResponseBody
    public int total(@PathVariable long instId,
                     @PathVariable long accountCycleId,
                     @PathVariable long clientOrgTreeId,
                     @PathVariable BizTaxInstanceCategory.State state,
                     @QueryParam QueryJson search) {

        String clientSn = search.getString("clientSn").orElse(null); //客户编号
        String clientName = search.getString("clientName").orElse(null); //客户名称

        return 0;
    }

    /**
     * 状态统计
     *
     * @param instId
     * @param accountCycleId
     * @param clientOrgTreeId
     * @param state
     * @param query
     * @return
     */
    @GetMapping("/total/code/{instId}/{accountCycleId}/{clientOrgTreeId}/{state}")
    @ResponseBody
    public Map<String, Integer> totalByCode(@PathVariable long instId,
                                            @PathVariable long accountCycleId,
                                            @PathVariable long clientOrgTreeId,
                                            @PathVariable BizTaxInstanceCategory.State state,
                                            @QueryParam QueryJson query) {
        Integer[] codes = query.getIntegerArray("codes").orElse(null);


        return null;
    }


    /**
     * 确定财务报表
     */
    @GetMapping("/check/to/send/{id}")
    @ResponseBody
    public void checkToSend(@PathVariable long id, @User UserDetail userDetail) {

        final BizTaxInstanceCategoryFr frY = Optional.ofNullable(bizTaxInstanceCategoryFrService.findById(id))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.BIZ_TAX_ERROR, "不存在财报"));
        BizTaxInstanceCategory bizTaxInstanceCategory = frY.getBizTaxInstanceCategory();
//        if (!bizTaxInstanceCategory.getState().equals(BizTaxInstanceCategory.State.check)) {
//            throw BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "确认财报失败,当前状态不为check");
//        }

        bizTaxInstanceCategoryService.isProcess(bizTaxInstanceCategory); //当前code是否允许继续

        if (!BizTaxInstanceCategoryFr.supportType().contains(frY.getFrType())) {
            throw BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "确认财报失败,当前不支持财报类型");
        }
        //判断是否有报表数据
        BizTaxInstanceCategoryReport bizTaxInstanceCategoryReport = bizTaxInstanceCategoryReportService.findByTaxInstanceCategoryId(bizTaxInstanceCategory.getId());
        if (null == bizTaxInstanceCategoryReport) {
            throw BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "确认财报失败,不存在报表");
        }

        //审核报表
        bizReportService.audit(bizTaxInstanceCategoryReport.getBizReportInstanceId(), userDetail.getId() + "");

        //判断是否0申报
        Map<BizReportInstanceSheet, BizReportInstanceSheetData> reportDatas = bizReportService.findByBizReportInstanceId(bizTaxInstanceCategoryReport.getBizReportInstanceId());
        Map<String, BizReportInstanceSheetData> sheetDataMap = new HashMap() {{
            reportDatas.forEach((k, v) -> {
                put(k.getSheetSn(), v);
            });
        }};


        //一般企业国税A31、A62都不能为0
        //一般企业地税A32、A63都不能为0
        //小企业国地A30、A53都不能为0
        Long executeCode = null;
        String executeMessage = null;
        if (bizTaxInstanceCategory.getBizTaxInstance().getTaxOffice().equals(TaxOffice.gs) && frY.getBizTaxInstanceCategory().getBizTaxInstance().getMdCompanyTaxType().equals(TaxType.normal.toString())) {
            Object a31 = "0";
            Object a62 = "0";
            if (sheetDataMap.get("shenzhen_gs_y_normal_fr_zcfzb") != null) {
                a31 = Optional.ofNullable(sheetDataMap.get("shenzhen_gs_y_normal_fr_zcfzb").getDatas().get("A31")).orElse("0");
                a62 = Optional.ofNullable(sheetDataMap.get("shenzhen_gs_y_normal_fr_zcfzb").getDatas().get("A62")).orElse("0");
            }

        } else if (bizTaxInstanceCategory.getBizTaxInstance().getTaxOffice().equals(TaxOffice.ds) && frY.getBizTaxInstanceCategory().getBizTaxInstance().getMdCompanyTaxType().equals(TaxType.normal.toString())) {
            Object a32 = "0";
            Object a63 = "0";
            if (sheetDataMap.get("shenzhen_ds_y_normal_fr_zcfzb") != null) {
                a32 = Optional.ofNullable(sheetDataMap.get("shenzhen_ds_y_normal_fr_zcfzb").getDatas().get("A32")).orElse("0");
                a63 = Optional.ofNullable(sheetDataMap.get("shenzhen_ds_y_normal_fr_zcfzb").getDatas().get("A63")).orElse("0");
            }


        } else if (bizTaxInstanceCategory.getBizTaxInstance().getTaxOffice().equals(TaxOffice.gs) && frY.getBizTaxInstanceCategory().getBizTaxInstance().getMdCompanyTaxType().equals(TaxType.small.toString())) {

            Object a30 = "0";
            Object a53 = "0";
            if (sheetDataMap.get("shenzhen_gs_y_small_fr_zcfzb") != null) {
                a30 = Optional.ofNullable(sheetDataMap.get("shenzhen_gs_y_small_fr_zcfzb").getDatas().get("A30")).orElse("0");
                a53 = Optional.ofNullable(sheetDataMap.get("shenzhen_gs_y_small_fr_zcfzb").getDatas().get("A53")).orElse("0");
            }


            BizTaxProperties bizTaxProperties = bizTaxPropertiesService.findByKey(BizTaxProperties.KEY_ALLOW_ZERO);

            if (BigDecimal.valueOf(Double.valueOf(a30.toString())).compareTo(new BigDecimal(0)) == 0 && BigDecimal.valueOf(Double.valueOf(a53.toString())).compareTo(new BigDecimal(0)) == 0) {

                if (bizTaxProperties.getValue().equals("Y")) {
                    executeMessage = BizTaxMdMsgCode.Process.fr_zcfzb_not_support.getMessage();
                    executeCode = BizTaxMdMsgCode.Process.fr_zcfzb_not_support.getCode();
                } else {
                    throw BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "资产负债表不允许0申报");
                }
            }
        }
        bizTaxInstanceCategoryService.updateCheckToSendWithMessage(bizTaxInstanceCategory.getId(), userDetail.getInstUserId(), "设置财报审核状态到申报状态", executeCode, executeMessage);
    }

    /**
     * 重新确认
     *
     * @param id
     */
    @GetMapping("/send/to/check/{id}")
    @ResponseBody
    public void sendToCheck(@PathVariable long id, @User UserDetail userDetail) {

        final BizTaxInstanceCategoryFr frY = Optional.ofNullable(bizTaxInstanceCategoryFrService.findById(id))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.BIZ_TAX_ERROR, "不存在财报"));
        BizTaxInstanceCategory bizTaxInstanceCategory = frY.getBizTaxInstanceCategory();

        //检查状态
        if (!bizTaxInstanceCategory.getState().equals(BizTaxInstanceCategory.State.send)) {
            throw BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "重新确认财报失败,当前状态不为Send");
        }

        if (bizTaxInstanceCategory.getProcessState().equals(BizTaxInstanceCategory.ProcessState.process)) {
            throw BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "当前还有任务正在执行不能在执行其他任务");
        }

        //修改状态
        bizTaxInstanceCategoryService.updateSendToCheck(bizTaxInstanceCategory.getId(), userDetail.getInstUserId(), "设置财报申报状态到审核状态");

    }

    /**
     * 修改财务报表类型
     *
     * @param id
     */
    @PutMapping("/update/type/{id}/{reportType}")
    @ResponseBody
    public void updateFrType(@PathVariable long id, @PathVariable BizTaxInstanceCategoryFr.FrType reportType, @User UserDetail userDetail) {

        final BizTaxInstanceCategoryFr frY = Optional.ofNullable(bizTaxInstanceCategoryFrService.findById(id))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.BIZ_TAX_ERROR, "不存在财报"));

        //===============是否支持重新设置财报=================
        BizTaxMdOfficeCategory bizTaxMdOfficeCategory = bizTaxInstanceCategoryFrService.isSupportResetReportType(id, reportType);
        if (null == bizTaxMdOfficeCategory) {
            throw BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "当前设置的报表类型系统不支持");
        }

        //===============获得财报最新报表编号==================
        List<BizTaxMdOfficeCategoryReport> reports = BizTaxMdOfficeCategoryReportService.findByTaxMdOfficeCategoryId(bizTaxMdOfficeCategory.getId());

        if (null == reports) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxMdCategoryNotFound);
        }
        //===================创建报表=============
        BizReportInstance bizReportInstance = bizReportService.create(reports.get(0).getReportSn(), new HashMap<String, Object>() {{
            put("createdBy", userDetail.getId());
            put("companyName", frY.getBizTaxInstanceCategory().getBizTaxInstance().getMdCompanyName());
            put("taxBeginDate", frY.getBizTaxInstanceCategory().getBeginDate());
            put("taxCreatedDate", System.currentTimeMillis() / 1000);
            put("taxEndDate", frY.getBizTaxInstanceCategory().getBeginDate());
            put("taxNo", "");
        }});
        //================重新设置财报================
        bizTaxInstanceCategoryFrService.resetFrType(id, bizTaxMdOfficeCategory.getId(),  userDetail.getInstUserId(), bizReportInstance.getId(), reportType);
    }

}
