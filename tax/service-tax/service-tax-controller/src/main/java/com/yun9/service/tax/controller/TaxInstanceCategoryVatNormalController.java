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
import com.yun9.service.tax.core.TaxInstanceCategoryVatNormalFactory;
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
@RequestMapping("/instance/category/vat/normal")
public class TaxInstanceCategoryVatNormalController {
    public static final Logger logger = LoggerFactory.getLogger(TaxInstanceCategoryVatNormalController.class);

    @Autowired
    private TaxInstanceCategoryVatNormalFactory taxInstanceCategoryVatNormalFactory;


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
            put("taxType", query.getString("taxType").orElse(null));//纳税方式
            put("invoiceSystem", query.getString("invoiceSystem").orElse(null));//开票系统
            put("billingType", query.getString("billingType").orElse(null));//增值税纳税人类型

            put("declareType", query.getString("declareType").orElse(null));//申报方式 例如网上申报
            put("vatDeclareType", query.getString("vatDeclareType").orElse(null));//申报类型
            put("processState", query.getString("processState").orElse(null));//办理状态
            put("audit", query.getLong("audit").orElse(null));//审核

            //排序
            put("selfAmount", query.getString("selfAmount").orElse(null));//自开金额
            put("nobillAmount", query.getString("nobillAmount").orElse(null));//无票金额
            put("taxPayAmount", query.getString("taxPayAmount").orElse(null));//应纳税额度
            put("realPayAmount", query.getString("realPayAmount").orElse(null));//应纳税额度（税局）
            put("processCodeId", query.getString("processCodeId").orElse(null));//办理状态Id
            put("id", query.getString("id").orElse(null));//个税企业id
            put("cycleType", query.getString("cycleType").orElse(null));
            put("taxOfficeConfirm", query.getString("taxOfficeConfirm").orElse(null));//税局是否启用
            put("declareType", query.getString("declareType").orElse(null));//申报方式

            //todo 标签
            put("taxAuditType", query.getString("taxAuditType").orElse(null));//税局是否启用
            put("taxDeclareType", query.getString("taxDeclareType").orElse(null));//税额状态
            put("instClientStateType", query.getString("instClientStateType").orElse(null));//客户停止服务
        }};

        params = PageCommon.label2params(params);
        return taxInstanceCategoryVatNormalFactory.pageByState(accountCycleIds, orgTreeId, state, pageable.getPage(), pageable.getLimit(), params);
    }


}

