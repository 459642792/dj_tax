package com.yun9.service.tax.controller;

import com.yun9.biz.md.domain.dto.InstClientDTO;
import com.yun9.biz.md.domain.entity.BizMdIncrement;
import com.yun9.biz.tax.BizTaxCompanyTaxService;
import com.yun9.biz.tax.domain.dto.BizTaxCompanyTaxDTO;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.framework.web.commons.Pageable;
import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.PageParam;
import com.yun9.framework.web.commons.annotation.QueryParam;
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
@RequestMapping("company/tax/gs")
public class TaxCompanyTaxGsController {

    @Autowired
    TaxCompanyTaxGsFactory taxCompanyTaxGsFactory;

    @Autowired
    BizTaxCompanyTaxService bizTaxCompanyTaxService;


    /**
     * 国税信息列表
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
        }};

        return taxCompanyTaxGsFactory.list(orgTreeId, pageable.getPage(), pageable.getLimit(), params);
    }

    /**
     * 查询公司国税信息
     * @param companyId
     * @return
     */
    @RequestMapping(value = "/{companyId}", method = RequestMethod.GET)
    @ResponseBody
    public Object find(@PathVariable(value = "companyId") long companyId) {
        return taxCompanyTaxGsFactory.find(companyId);
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
