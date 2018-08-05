package com.yun9.service.tax.controller;

import com.yun9.biz.tax.BizTaxCompanyCategoryService;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.framework.web.commons.Pageable;
import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.PageParam;
import com.yun9.framework.web.commons.annotation.QueryParam;
import com.yun9.service.tax.core.TaxCompanyCategoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

/**
 * 公司核定税种信息
 */
@Controller
@RequestMapping("/company/category")
public class TaxCompanyCategoryController {

    @Autowired
    TaxCompanyCategoryFactory taxCompanyCategoryFactory;
    @Autowired
    BizTaxCompanyCategoryService bizTaxCompanyCategoryService;

    /**
     * 绑定客户税种
     * @param companyId
     * @param taxOfficeCategoryId
     */
    @RequestMapping(value = "/create/{companyId}/{taxOfficeCategoryId}", method = RequestMethod.PUT)
    @ResponseBody
    public void create(@PathVariable long companyId,
                       @PathVariable long taxOfficeCategoryId) {
        taxCompanyCategoryFactory.create(companyId,taxOfficeCategoryId);
    }

    /**
     * 查询公司绑定税种信息
     * @param companyId
     * @param taxOffice
     * @return
     */
    @GetMapping("/list/{companyId}/{taxOffice}")
    @ResponseBody
    public Object list(@PathVariable long companyId,
                       @PathVariable String taxOffice) {
        return taxCompanyCategoryFactory.list(companyId, TaxOffice.valueOf(taxOffice));
    }

    /**
     * 删除机构客户税种信息
     * @param taxCompanyCategoryId
     */
    @RequestMapping(value = "/delete/{taxCompanyCategoryId}", method = RequestMethod.PUT)
    @ResponseBody
    public void delete(@PathVariable long taxCompanyCategoryId) {
        bizTaxCompanyCategoryService.delete(taxCompanyCategoryId);
    }

    /**
     * 统计包含税种的正常客户数量
     * @param orgTreeId
     * @param query
     */
    @RequestMapping(value = "/count/contain/{orgTreeId}", method = RequestMethod.GET)
    @ResponseBody
    public Object countContainTaxNos(@PathVariable long orgTreeId,
                                      @QueryParam QueryJson query){

        HashMap<String, Object> params = new HashMap() {{
            put("taxSns", query.getStringArray("taxSns").orElse(null));
        }};

        return taxCompanyCategoryFactory.countContainTaxSns(orgTreeId,params);
    }

}
