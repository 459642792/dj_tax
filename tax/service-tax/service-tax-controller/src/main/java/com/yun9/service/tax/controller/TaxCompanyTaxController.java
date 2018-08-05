package com.yun9.service.tax.controller;

import com.yun9.biz.tax.BizTaxCompanyTaxService;
import com.yun9.biz.tax.domain.dto.BizTaxCompanyTaxDTO;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.framework.web.commons.Pageable;
import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.PageParam;
import com.yun9.framework.web.commons.annotation.QueryParam;
import com.yun9.service.tax.core.TaxCompanyTaxFactory;
import com.yun9.service.tax.core.TaxCompanyTaxGsFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

/**
 * Created by zhengzb on 2018/6/11.
 * 国税信息
 */
@Controller
@RequestMapping("company/tax")
public class TaxCompanyTaxController {

    @Autowired
    TaxCompanyTaxFactory taxCompanyTaxFactory;

    @Autowired
    BizTaxCompanyTaxService bizTaxCompanyTaxService;


    /**
     * 税务信息列表
     * @param orgTreeId     组织id
     * @param query         查询条件
     * @param pageable
     * @return
     */
    @GetMapping("/list/{orgTreeId}")
    @ResponseBody
    public Pagination gsList(@PathVariable long orgTreeId,
                                 @QueryParam QueryJson query,
                                 @PageParam Pageable pageable) {

        HashMap<String, Object> params = new HashMap() {{
            put("clientSn", query.getString("clientSn").orElse(null));
            put("companyName", query.getString("companyName").orElse(null));
            put("taxType", query.getString("taxType").orElse(null));
            put("mdAreaId", query.getLong("taxAreaId").orElse(null));
            put("invoiceSystem", query.getString("invoiceSystem").orElse(null));
            put("billingType", query.getString("billingType").orElse(null));
            put("processState", query.getString("processState").orElse(null));
            put("syncState", query.getString("syncState").orElse(null));
            put("loginType", query.getString("passwordType").orElse(null));
            put("taxSnName", query.getString("taxSnName").orElse(null));
            put("processCodeId", query.getLong("processCodeId").orElse(null));
            put("state", query.getString("state").orElse(null));
        }};

        return taxCompanyTaxFactory.list(orgTreeId, pageable.getPage(), pageable.getLimit(), params);
    }

    @GetMapping("/total/exception/{orgTreeId}")
    @ResponseBody
    public HashMap totalByException(@PathVariable long orgTreeId,
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

        return taxCompanyTaxFactory.totalByException(orgTreeId, params);
    }

    /**
     * 查询公司国税信息
     * @param companyId
     * @return
     */
    @RequestMapping(value = "/{companyId}", method = RequestMethod.GET)
    @ResponseBody
    public Object find(@PathVariable(value = "companyId") long companyId) {
        return taxCompanyTaxFactory.find(companyId);
    }

    /**
     * 修改客户国税信息
     * @param companyId  客户id
     * @param bizTaxCompanyTaxDTO  客户国税信息
     * @return
     */
    @RequestMapping(value = "/{companyId}", method = RequestMethod.PUT)
    @ResponseBody
    public void update(@PathVariable(value = "companyId") long companyId, @RequestBody BizTaxCompanyTaxDTO bizTaxCompanyTaxDTO) {
        bizTaxCompanyTaxService.update(companyId, bizTaxCompanyTaxDTO);
    }



}
