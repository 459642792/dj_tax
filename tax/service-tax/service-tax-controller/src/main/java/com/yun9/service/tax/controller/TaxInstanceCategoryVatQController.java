package com.yun9.service.tax.controller;


import com.alibaba.fastjson.JSON;
import com.yun9.biz.bill.domain.bo.BizBillInvoiceAgentInvoiceDto;
import com.yun9.biz.bill.domain.entity.BizBillInvoice;
import com.yun9.biz.bill.domain.entity.BizBillInvoiceItem;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryVatSmall;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.framework.web.commons.Pageable;
import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.PageParam;
import com.yun9.framework.web.commons.annotation.QueryParam;
import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.TaxInstanceCategoryVatQFactory;
import com.yun9.service.tax.core.dto.BillDTO;
import com.yun9.service.tax.core.dto.BizTaxVatImportSheetDTO;
import com.yun9.service.tax.core.impl.PageCommon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.PathParam;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

//import com.yun9.service.tax.core.utils.ExportExcelUtil;


/**
 * 增值税季报
 */
@Controller
@RequestMapping("/instance/category/vat")
public class TaxInstanceCategoryVatQController {
    public static final Logger logger = LoggerFactory.getLogger(TaxInstanceCategoryVatQController.class);

    @Autowired
    private TaxInstanceCategoryVatQFactory taxInstanceCategoryVatQFactory;


    @Value("${file.upload.path}")
    private String path;


    /**
     * 根据申报类型统计
     *
     * @return
     */
    @GetMapping("/total/declare/type/{orgTreeId}/{state}")
    @ResponseBody
    public HashMap totalByDeclareType(@PathVariable long orgTreeId, @PathVariable BizTaxInstanceCategory.State state, @QueryParam
            QueryJson query) {

        List<Long> accountCycleIds = Arrays.asList(query.getLongArray("accountCycleIds")
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误,没有会计区间")));

        Map<String, Object> params = new HashMap() {{
            put("clientSn", query.getString("clientSn").orElse(null));
            put("companyName", query.getString("companyName").orElse(null));
        }};

        return taxInstanceCategoryVatQFactory.totalByDeclareType(accountCycleIds, orgTreeId, state, params);
    }


    /**
     * 获取列表
     *
     * @param orgTreeId 组装ID
     * @param state     状态
     * @param query     过滤条件
     * @param pageable  分页查询
     * @return
     */
    @GetMapping("/list/{orgTreeId}/{state}")
    @ResponseBody
    public Pagination<HashMap> list(@PathVariable long orgTreeId, @PathVariable BizTaxInstanceCategory.State state, @QueryParam
            QueryJson query, @PageParam Pageable pageable) {

        List<Long> accountCycleIds = Arrays.asList(query.getLongArray("accountCycleIds")
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误,没有会计区间")));

        Map<String, Object> params = new HashMap() {{
            put("clientSn", query.getString("clientSn").orElse(null));
            put("companyName", query.getString("companyName").orElse(null));
            put("taxAreaId", query.getLong("taxAreaId").orElse(null));//税区

            put("taxTypes", query.getStringArray("taxType").orElse(null));//纳税方式
            if (get("taxTypes") == null){
                put("taxType", query.getString("taxType").orElse(null));//纳税方式
            }

            put("invoiceSystem", query.getString("invoiceSystem").orElse(null));//开票系统
            put("billingType", query.getString("billingType").orElse(null));//增值税纳税人类型

            put("declareType", query.getString("declareType").orElse(null));//申报方式 例如网上申报
            put("taxDeclareType", query.getString("taxDeclareType").orElse(null));//申报类型
            put("processCodeId", query.getString("processCodeId").orElse(null));//办理状态Id
            put("processState", query.getString("processState").orElse(null));//办理状态
            put("id", query.getString("id").orElse(null));//个税企业id
            put("audit", query.getLong("audit").orElse(null));//审核

            //排序
            put("prepayTaxAmount", query.getString("prepayTaxAmount").orElse(null));//预缴金额
            put("agentAmount", query.getString("agentAmount").orElse(null));//代开金额
            put("selfAmount", query.getString("selfAmount").orElse(null));//自开金额
            put("nobillAmount", query.getString("nobillAmount").orElse(null));//无票金额
            put("taxPayAmount", query.getString("taxPayAmount").orElse(null));//应纳税额度
            put("realPayAmount", query.getString("realPayAmount").orElse(null));//应纳税额度（税局）
            put("cycleType", query.getString("cycleType").orElse(null));
            put("taxOfficeConfirm", query.getString("taxOfficeConfirm").orElse(null));//税局是否启用
            put("ticketCheckState", query.getString("ticketCheckState").orElse(null));//票表核对状态
            put("declareType", query.getString("declareType").orElse(null));//申报方式

            //todo 标签
            put("taxAuditType", query.getString("taxAuditType").orElse(null));//税局是否启用
            put("taxDeclareType", query.getString("taxDeclareType").orElse(null));//税额状态
            put("instClientStateType", query.getString("instClientStateType").orElse(null));//客户停止服务
        }};

        params = PageCommon.label2params(params);
        return taxInstanceCategoryVatQFactory.pageByState(accountCycleIds, orgTreeId, state, pageable.getPage(), pageable.getLimit(), params);
    }


    /**
     * @param id 增值税ID
     * @return
     */
    @GetMapping("/{id}")
    @ResponseBody
    public Object findById(@PathVariable long id) {
        return taxInstanceCategoryVatQFactory.findById(id);
    }


    /**
     * 下载模板
     *
     * @param request
     * @param response
     */

    @RequestMapping(value = "/download/excel/{mdAccountCycleId}/{categoryIds}", method = RequestMethod.GET)
    @ResponseBody
    public void getSystemExcel(HttpServletRequest request, HttpServletResponse response,
                               @PathVariable Long[] categoryIds, @PathVariable long mdAccountCycleId) {
        List<Long> ids = Arrays.asList(categoryIds);
        if (CollectionUtils.isEmpty(ids)) {
            throw BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误，没有选择下载对象");
        }
        taxInstanceCategoryVatQFactory.downloadExcel(request, response, ids, mdAccountCycleId);
    }

    /**
     * 导入数据
     *
     * @return
     */
    @RequestMapping(value = "/upload/{instId}/{mdAccountCycleId}", method = RequestMethod.POST)
    @ResponseBody
    public Object upload(@PathVariable Long instId, @PathVariable long mdAccountCycleId,
                         @User UserDetail userDetail,
                         MultipartHttpServletRequest request) {
        Iterator<String> itr = request.getFileNames();
        MultipartFile mpf = null;
        Object object = null;
        while (itr.hasNext()) {
            mpf = request.getFile(itr.next());
        }
        try {
            request.setCharacterEncoding("UTF-8");
            logger.info("文件名称{}", mpf.getOriginalFilename());
            BizTaxVatImportSheetDTO importSheetDTO = new BizTaxVatImportSheetDTO();
            importSheetDTO.setMdAccountCycleId(mdAccountCycleId);
            importSheetDTO.setProcessBy(userDetail.getFtId());
            importSheetDTO.setFileData(mpf.getBytes());
            importSheetDTO.setFileOriginalName(mpf.getOriginalFilename());
            importSheetDTO.setFileUploadPath(path);
            importSheetDTO.setInstId(instId);
//            importSheetDTO.setUserId(userDetail.getInstUserId());
            object = taxInstanceCategoryVatQFactory.parseVatExcel(importSheetDTO);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return JSON.toJSON(object);
    }


    /**
     * 审核
     *
     * @param id
     */
    @PutMapping(value = "/audit/{id}")
    @ResponseBody
    public void audit(@PathVariable long id, @User UserDetail userDetail) {
        taxInstanceCategoryVatQFactory.audit(id, userDetail.getInstUserId());
    }

    /**
     * 取消 审核
     *
     * @param id         增值税id
     * @param userDetail
     */
    @PutMapping(value = "/cancel/audit/{id}")
    @ResponseBody
    public void cancelAudit(@PathVariable long id
            , @User UserDetail userDetail) {
        taxInstanceCategoryVatQFactory.cancelAudit(id, userDetail.getInstUserId());
    }


    /**
     * 保存累计预缴
     *
     * @param id         增值税id
     * @param userDetail
     */
    @PostMapping(value = "/prepay/{id}/{prepayTaxSource}")
    @ResponseBody
    public void prepayBill(@PathVariable long id
            , @PathVariable BizTaxInstanceCategoryVatSmall.PrepayTaxSource prepayTaxSource
            , @User UserDetail userDetail) {
        taxInstanceCategoryVatQFactory.savePrepay(id, prepayTaxSource, userDetail.getInstUserId());

    }

    /**
     * 保存累计预缴
     *
     * @param id         增值税id
     * @param userDetail
     */
    @PostMapping(value = "/prepay/{id}")
    @ResponseBody
    public void prepayBill(@PathVariable long id
            , @PathParam("cargoAmount") BigDecimal cargoAmount, @PathParam("serviceAmount") BigDecimal serviceAmount, @User UserDetail userDetail) {
        taxInstanceCategoryVatQFactory.savePrepay(id, cargoAmount, serviceAmount, userDetail.getInstUserId());

    }

    /**
     * 保存发票
     *
     * @param billDTO                发票类
     * @param instanceCategoryVatQId 增值税
     */
    @PostMapping(value = "/save/bills/{instanceCategoryVatQId}/{state}")
    @ResponseBody
    public void saveBill(@PathVariable long instanceCategoryVatQId, @PathVariable int state, @User UserDetail userDetail, @RequestBody List<BillDTO> billDTO) {
        taxInstanceCategoryVatQFactory.saveBill(billDTO, instanceCategoryVatQId, state, userDetail.getInstUserId());
    }

    /**
     * 新增发票
     *
     * @param bizBillInvoice 发票信息
     * @return
     */
    @RequestMapping(value = "/save/bill/{id}/{billType}", method = RequestMethod.POST)
    @ResponseBody
    public void saveBill(@PathVariable long id, @PathVariable BizBillInvoice.BillType billType, @RequestBody BizBillInvoiceAgentInvoiceDto bizBillInvoice, @User UserDetail userDetail) {
        taxInstanceCategoryVatQFactory.createBill(id, billType, bizBillInvoice, userDetail.getInstUserId());
    }

    /**
     * 修改发票信息
     *
     * @param id
     * @param bizBillInvoice
     */
    @RequestMapping(value = "/update/bill/{id}/{billId}", method = RequestMethod.PUT)
    @ResponseBody
    public void updateDeclareprepaid(@PathVariable long id, @PathVariable long billId, @RequestBody BizBillInvoice bizBillInvoice, @User UserDetail userDetail) {
        taxInstanceCategoryVatQFactory.updateBill(id, billId, bizBillInvoice, userDetail.getInstUserId());
    }

    /**
     * 删除发票信息
     *
     * @param id
     */
    @RequestMapping(value = "/delete/bill/{id}/{billId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteBill(@PathVariable long id, @PathVariable long billId, @User UserDetail userDetail) {

        taxInstanceCategoryVatQFactory.deleteById(id, billId, userDetail.getInstUserId());
    }

    /**
     * 查询应纳税额
     * by-yunjie 2018-07-05
     * @param id
     * @param type
     */
    @RequestMapping(value = "/query/payTaxAmount/{id}/{type}", method = RequestMethod.GET)
    @ResponseBody
    public Map queryPayTaxAmount(@PathVariable long id, @PathVariable String type, @User UserDetail userDetail) {
        return taxInstanceCategoryVatQFactory.queryPayTaxAmountById(id, type, userDetail.getInstUserId());//userDetail.getInstUserId());
    }

    /**
     * 修改申报明细
     *
     * @param id
     * @param bizBillInvoiceItem
     */
    @RequestMapping(value = "/update/bill/item/{id}/{itemId}", method = RequestMethod.PUT)
    @ResponseBody
    public void updateDeclareamount(@PathVariable long id, @PathVariable long itemId, @RequestBody BizBillInvoiceItem bizBillInvoiceItem, @User UserDetail userDetail) {
        taxInstanceCategoryVatQFactory.updateBillItem(id, itemId, bizBillInvoiceItem, userDetail.getInstUserId());
    }


}

