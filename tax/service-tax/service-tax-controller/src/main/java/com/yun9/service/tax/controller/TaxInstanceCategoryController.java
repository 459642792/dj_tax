package com.yun9.service.tax.controller;

import com.yun9.biz.md.BizMdCompanyService;
import com.yun9.biz.md.domain.entity.BizMdCompany;
import com.yun9.biz.md.exception.BizMdException;
import com.yun9.biz.report.BizReportService;
import com.yun9.biz.report.domain.entity.BizReportInstanceSheet;
import com.yun9.biz.report.domain.entity.BizReportInstanceSheetData;
import com.yun9.biz.tax.BizTaxInstanceCategoryAttachmentService;
import com.yun9.biz.tax.BizTaxInstanceCategoryReportService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryAttachment;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryReport;
import com.yun9.biz.tax.enums.DeclareType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.biz.tax.enums.TaxType;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.framework.web.commons.Pageable;
import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.PageParam;
import com.yun9.framework.web.commons.annotation.QueryParam;
import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.TaxInstanceCategoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Created by werewolf on  2018/5/8.
 */
@Controller
@RequestMapping("instance/category")
public class TaxInstanceCategoryController {

    @Autowired
    BizTaxInstanceCategoryReportService bizTaxInstanceCategoryReportService;

    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    BizTaxInstanceCategoryAttachmentService bizTaxInstanceCategoryAttachmentService;

    @Autowired
    BizReportService bizReportService;

    @Autowired
    BizMdCompanyService bizMdCompanyService;

    @Autowired
    TaxInstanceCategoryFactory taxInstanceCategoryFactory;


    /**
     * 未申报列表（所有税种通用）
     * @param orgTreeId     组织id
     * @param query         查询条件
     * @param pageable
     * @return
     */
    @GetMapping("/list/launch/{orgTreeId}")
    @ResponseBody
    public Pagination listLaunch(@PathVariable long orgTreeId,
                                 @QueryParam QueryJson query,
                                 @PageParam Pageable pageable) {

        HashMap<String, Object> params = new HashMap() {{
            //这几个参数暂时过滤不到了
            put("clientSn", query.getString("clientSn").orElse(null));
            put("companyName", query.getString("companyName").orElse(null));
            put("taxType", query.getString("taxType").orElse(null));

            put("accountCycleIds", query.getLongArray("accountCycleIds").orElse(null));
            put("taxSns", query.getStringArray("taxSns").orElse(null));
            put("taxOffice", query.getString("taxOffice").orElse(null));
            put("mdAreaIds", query.getLongArray("taxAreaIds").orElse(null));
            put("cycleTypes", query.getStringArray("cycleTypes").orElse(null));
            put("sendTypes", query.getStringArray("sendTypes").orElse(null));
            put("loginType", query.getString("passwordType").orElse(null));
            put("taxAreaId", query.getString("taxAreaId").orElse(null));
        }};

        return taxInstanceCategoryFactory.listLaunch(orgTreeId, pageable.getPage(), pageable.getLimit(), params);
    }

    /**
     * 统计申报流程中各种状态下客户数量（所有税种通用）
     * @param orgTreeId         组织树id
     * @param query             查询条件
     * @return
     */
    @GetMapping("/total/state/{orgTreeId}")
    @ResponseBody
    public HashMap totalByState(@PathVariable long orgTreeId,
                                @QueryParam QueryJson query) {

        HashMap<String, Object> params = new HashMap() {{
            put("clientSn", query.getString("clientSn").orElse(null));
            put("companyName", query.getString("companyName").orElse(null));
            put("taxSns", query.getStringArray("taxSns").orElse(null));
            put("accountCycleIds", query.getLongArray("accountCycleIds").orElse(null));
            put("taxOffice", query.getString("taxOffice").orElse(null));
            put("taxTypes", query.getStringArray("taxTypes").orElse(null));
            put("loginType", query.getString("passwordType").orElse(null));
            put("taxAreaId", query.getString("taxAreaId").orElse(null));
        }};
        List<String> processStates = new ArrayList() {{
            add(BizTaxInstanceCategory.ProcessState.exception.name());
            add(BizTaxInstanceCategory.ProcessState.process.name());
        }};
        return taxInstanceCategoryFactory.totalByState(orgTreeId, processStates, params);
    }


    /**
     * 统计申报后税种异常状态的客户数量(各个税种通用)
     *
     * @param orgTreeId
     * @param state         可为空 表示查询发起处理页面的异常状态
     * @param query
     * @return
     */
    @GetMapping("/total/exception/{orgTreeId}")
    @ResponseBody
    public HashMap totalByException(@PathVariable long orgTreeId,
                                    @RequestParam(value = "state", required = false) String state,
                                    @QueryParam QueryJson query) {

        //1. TODO instId 是否需要？

        HashMap<String, Object> params = new HashMap() {{
            put("taxSns", query.getStringArray("taxSns").orElse(null));
            put("accountCycleIds", query.getLongArray("accountCycleIds").orElse(null));
            put("taxTypes", query.getStringArray("taxTypes").orElse(null));
            put("taxOffice", query.getString("taxOffice").orElse(null));
            put("clientSn", query.getString("clientSn").orElse(null));
            put("companyName", query.getString("companyName").orElse(null));
        }};

        return taxInstanceCategoryFactory.totalByException(orgTreeId, state, params);
    }

    /**
     * 查询报表【报表定义，和报表数据】
     *
     * @param instanceCategoryId
     * @return
     */
    @RequestMapping("/report/data/{instanceCategoryId}")
    @ResponseBody
    public Map<BizReportInstanceSheet, BizReportInstanceSheetData> reportData(@PathVariable long instanceCategoryId) {
        //获得报表ID
        BizTaxInstanceCategoryReport report = Optional.ofNullable(bizTaxInstanceCategoryReportService.findByTaxInstanceCategoryId(instanceCategoryId)).orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.BIZ_TAX_REPORT_NOT_FOUND));

        //根据报表ID查询数据
        return bizReportService.findByBizReportInstanceId(report.getBizReportInstanceId());
    }

    /**
     * 查询报表【只有报表定义，没有数据】
     *
     * @param instanceCategoryId
     * @return
     */
    @RequestMapping("/report/define/{instanceCategoryId}")
    @ResponseBody
    public Map<String, Object> reportDefine(@PathVariable long instanceCategoryId) {
        //税种信息
        BizTaxInstanceCategory bizTaxInstanceCategory = Optional.ofNullable(bizTaxInstanceCategoryService.findById(instanceCategoryId)).orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.TaxInstanceCategoryNotFound));
        //公司信息
        BizMdCompany bizMdCompany = Optional.ofNullable(bizMdCompanyService.findById(bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId())).orElseThrow(() -> BizMdException.throwException(BizMdException.Codes.BizMdCompanyNotFound));
        //报表定义
        BizTaxInstanceCategoryReport report = Optional.ofNullable(bizTaxInstanceCategoryReportService.findByTaxInstanceCategoryId(instanceCategoryId)).orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.BIZ_TAX_REPORT_NOT_FOUND));

        List<BizReportInstanceSheet> list = bizReportService.findByInstanceId(report.getBizReportInstanceId());
        return new HashMap() {{
            put("taxNo", bizMdCompany.getTaxNo());
            put("reportDefine", list);
        }};
    }


    /**
     * 获取TaxNo
     *
     * @param instanceCategoryId
     * @return
     */
    @RequestMapping("/taxNo/{instanceCategoryId}")
    @ResponseBody
    public Map<String, Object> taxNo(@PathVariable long instanceCategoryId) {
        //税种信息
        BizTaxInstanceCategory bizTaxInstanceCategory = Optional.ofNullable(bizTaxInstanceCategoryService.findById(instanceCategoryId)).orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.TaxInstanceCategoryNotFound));
        //公司信息
        BizMdCompany bizMdCompany = Optional.ofNullable(bizMdCompanyService.findById(bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId())).orElseThrow(() -> BizMdException.throwException(BizMdException.Codes.BizMdCompanyNotFound));

        return new HashMap() {{
            put("taxNo", bizMdCompany.getTaxNo());
        }};
    }



    /**
     * 查询附件
     *
     * @param instanceCategoryId
     * @return
     */
    @RequestMapping("/attachment/{instanceCategoryId}/{type}")
    @ResponseBody
    public Object attachment(@PathVariable long instanceCategoryId, @PathVariable BizTaxInstanceCategoryAttachment.Type type, @QueryParam QueryJson query) {

        Long bizTaxInstanceCategoryDeductId = query.getLong("bizTaxInstanceCategoryDeductId").orElse(null);

        if (bizTaxInstanceCategoryDeductId == null) {
            return Optional.ofNullable(bizTaxInstanceCategoryAttachmentService.findByTaxInstanceCategoryIdAndType(instanceCategoryId, type))
                    .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.DateError, "没有扣款记录"));
        }
        return Optional.ofNullable(bizTaxInstanceCategoryAttachmentService.findByBizTaxInstanceCategoryIdAndTypeAndBizTaxInstanceCategoryDeductId(instanceCategoryId, type, bizTaxInstanceCategoryDeductId))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.DateError, "没有扣款记录"));

    }


    /**
     * 查询税种详情
     *
     * @param taxInstanceCategoryId
     * @return
     */
    @RequestMapping(value = "/detail/{taxInstanceCategoryId}", method = RequestMethod.GET)
    @ResponseBody
    public Object detail(@PathVariable long taxInstanceCategoryId) {
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService.findById(taxInstanceCategoryId);
        return new HashMap() {{
            put("taxInstanceCategoryId", bizTaxInstanceCategory.getId());
            put("state", bizTaxInstanceCategory.getState());
            put("processState", bizTaxInstanceCategory.getProcessState());
            put("processMessage", bizTaxInstanceCategory.getProcessMessage());
        }};
    }

    /**
     * 取消警告
     *
     * @param taxInstanceCategoryId
     * @return
     */
    @RequestMapping(value = "/cancel/warning/{taxInstanceCategoryId}/{createBy}", method = RequestMethod.PUT)
    @ResponseBody
    public Object cancelWarning(@PathVariable long taxInstanceCategoryId, @PathVariable long createBy) {
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService.cancelWarning(taxInstanceCategoryId, createBy);
        return new HashMap() {{
            put("taxInstanceCategoryId", bizTaxInstanceCategory.getId());
            put("state", bizTaxInstanceCategory.getState());
            put("processState", bizTaxInstanceCategory.getProcessState());
        }};
    }

    /**
     * 修改开票系统
     *
     * @param taxInstanceCategoryId
     * @param invoiceSystem
     * @param updateBy
     * @return
     */
    @RequestMapping(value = "/update/invoice/system/{taxInstanceCategoryId}/{invoiceSystem}/{updateBy}", method = RequestMethod.PUT)
    @ResponseBody
    public void updateInvoiceSystem(@PathVariable long taxInstanceCategoryId, @PathVariable String invoiceSystem, @PathVariable long updateBy) {
        taxInstanceCategoryFactory.updateInvoiceSystem(taxInstanceCategoryId, invoiceSystem, updateBy);
    }

    /**
     * 修改开票类型
     *
     * @param taxInstanceCategoryId
     * @param billType
     * @param updateBy
     * @return
     */
    @RequestMapping(value = "/update/bill/type/{taxInstanceCategoryId}/{billType}/{updateBy}", method = RequestMethod.PUT)
    @ResponseBody
    public void updateBillingType(@PathVariable long taxInstanceCategoryId, @PathVariable String billType, @PathVariable long updateBy) {
        taxInstanceCategoryFactory.updateBillType(taxInstanceCategoryId, billType, updateBy);
    }



    /**
     * 撤销已报
     *
     * @param id         增值税ID
     * @param userDetail 用户
     */
    @RequestMapping(value = "/cancel/declare/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public void declareCompleteToStart(@PathVariable long id, @User UserDetail userDetail) {
        taxInstanceCategoryFactory.cancelDeclare(id, userDetail.getInstUserId(), "撤销确认已申报操作，从\"完成\"状态改为\"未发起\"状态");
    }


    /**
     * 无需申报
     *
     * @param id         增值税ID
     * @param userDetail 用户
     */
    @RequestMapping(value = "/undeclared/start/to/complete/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public void undeclaredStartToComplete(@PathVariable long id, @QueryParam QueryJson query, @User UserDetail userDetail) {
        String remark = query.getString("remark").orElse("无需申报操作，从\"申报\"状态改为\"完成\"");
        taxInstanceCategoryFactory.startToComplete(id, userDetail.getInstUserId(), DeclareType.undeclare, remark);

    }

    /**
     * 撤销无需申报
     *
     * @param id         增值税ID
     * @param userDetail 用户
     */
    @RequestMapping(value = "/undeclared/complete/to/start/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public void undeclaredCompleteToStart(@PathVariable long id, @User UserDetail userDetail) {
        taxInstanceCategoryFactory.completeToState(id, userDetail.getInstUserId(), DeclareType.undeclare, "撤销无需申报操作，从\"完成\"状态改为\"申报\"");
    }

    /**
     * 下载申报扣款截图
     *
     * @param request
     * @param response
     */
    @GetMapping("/download")
    @ResponseBody
    public Object download(HttpServletRequest request, HttpServletResponse response,
                           @QueryParam QueryJson query) {

        List<Long> ids = Optional.ofNullable(Arrays.asList(query.getLongArray("ids").orElse(null)))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误，没有选择下载对象"));

        if (ids.size() > 200) {
            throw BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "下载量不能超过200");
        }


        return taxInstanceCategoryFactory.downloadDeclareAndPayImage(request, response, ids);

    }
    /**
     * 根据申报类型统计
     *
     * @return
     */
    @GetMapping("/total/tax/{taxType}/{taxSn}/{taxOffice}/{state}/{orgTreeId}")
    @ResponseBody
    public Object totalByTaxType(@PathVariable TaxType taxType, @PathVariable TaxSn taxSn, @PathVariable TaxOffice taxOffice, @PathVariable BizTaxInstanceCategory.State state, @PathVariable long orgTreeId, @QueryParam
            QueryJson query) {

        List<Long> accountCycleIds = Arrays.asList(query.getLongArray("accountCycleIds")
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误,没有会计区间")));



        Map<String, Object> params = new HashMap<String, Object>() {{
            put("clientSn", query.getString("clientSn").orElse(null));
            put("companyName", query.getString("companyName").orElse(null));
        }};

        return taxInstanceCategoryFactory.totalByTaxType(accountCycleIds,orgTreeId,taxType,taxSn,taxOffice,state, params);
    }
    /**
     * 确认已申报
     *
     * @param id         增值税ID
     * @param userDetail 用户
     */
    @RequestMapping(value = "/declare/start/to/complete/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public void declareStartToComplete(@PathVariable long id, @QueryParam QueryJson query, @User UserDetail userDetail) {
        String remark = query.getString("remark").orElse("确认已申报操作，从\"申报\"状态改为\"完成\"状态");
        taxInstanceCategoryFactory.startToComplete(id, userDetail.getInstUserId(), DeclareType.handwork, remark);
    }
    /**
     * 批量撤销审核
     */
    @RequestMapping(value = "/verify/batchCancelAudit/{taxSn}/{ids}",method = RequestMethod.PUT)
    @ResponseBody
    public void batchCancelAudit(@PathVariable TaxSn taxSn,@PathVariable Long[] ids,@User UserDetail userDetail) {
        if(StringUtils.isEmpty(taxSn)){
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "参数错误，没有税种代码");
        }
        List<Long> ids_ = Arrays.asList(ids);
        if (CollectionUtils.isEmpty(ids_)){
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "参数错误，没有批量审核的ID");
        }
        taxInstanceCategoryFactory.batchCancelAudit(taxSn,ids_,userDetail.getInstUserId());
    }

}
