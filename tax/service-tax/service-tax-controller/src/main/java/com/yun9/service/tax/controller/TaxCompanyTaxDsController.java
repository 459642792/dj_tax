package com.yun9.service.tax.controller;

import com.yun9.biz.tax.BizTaxCompanyTaxDsService;
import com.yun9.biz.tax.domain.dto.BizTaxCompanyTaxDTO;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyTaxDs;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.framework.web.commons.Pageable;
import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.PageParam;
import com.yun9.framework.web.commons.annotation.QueryParam;
import com.yun9.service.tax.core.TaxCompanyTaxDsFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

/**
 * Created by zhengzb on 2018/6/11.
 * 地税信息
 */
@Controller
@RequestMapping("company/tax/ds")
public class TaxCompanyTaxDsController {

    @Autowired
    TaxCompanyTaxDsFactory taxCompanyTaxDsFactory;

    @Autowired
    BizTaxCompanyTaxDsService bizTaxCompanyTaxDsService;

    /**
     * 地税信息列表
     * @param orgTreeId     组织id
     * @param query         查询条件
     * @param pageable
     * @return
     */
    @GetMapping("/list/{orgTreeId}")
    @ResponseBody
    public Pagination list(@PathVariable long orgTreeId,
                                 @QueryParam QueryJson query,
                                 @PageParam Pageable pageable) {

        HashMap<String, Object> params = new HashMap() {{
            put("clientSn", query.getString("clientSn").orElse(null));
            put("companyName", query.getString("companyName").orElse(null));
            put("taxType", query.getString("taxType").orElse(null));
            put("mdAreaId", query.getLong("taxAreaId").orElse(null));
        }};

        return taxCompanyTaxDsFactory.list(orgTreeId, pageable.getPage(), pageable.getLimit(), params);
    }

    /**
     * 查询公司地税信息
     * @param companyId
     * @return
     */
    @RequestMapping(value = "/{companyId}", method = RequestMethod.GET)
    @ResponseBody
    public Object find(@PathVariable(value = "companyId") long companyId) {
        BizTaxCompanyTaxDs bizTaxCompanyTaxDs = bizTaxCompanyTaxDsService.findByCompanyId(companyId);
        if(null == bizTaxCompanyTaxDs){
            return new HashMap();
        }
        return new HashMap(){{
            put("id",bizTaxCompanyTaxDs.getId());
            put("taxNo",bizTaxCompanyTaxDs.getTaxNo());
            put("personDeclareType",bizTaxCompanyTaxDs.getPersonDeclareType());
        }};
    }

    @RequestMapping(value = "/{companyId}", method = RequestMethod.PUT)
    @ResponseBody
    public void update(@PathVariable(value = "companyId") long companyId,
                       @RequestBody BizTaxCompanyTaxDs bizTaxCompanyTaxDs) {
        bizTaxCompanyTaxDsService.update(companyId, bizTaxCompanyTaxDs);
    }

}
