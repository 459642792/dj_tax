package com.yun9.service.tax.controller;

import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.service.tax.core.TaxMdOfficeCategoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by zhengzb on 2018/6/13.
 */
@Controller
@RequestMapping("/office/category")
public class TaxMdOfficeCategoryController {


    @Autowired
    TaxMdOfficeCategoryFactory taxMdOfficeCategoryFactory;

    /**
     * 查询公司税局 支持选择的税种列表
     * @param companyId
     * @param taxOffice
     * @return
     */
    @GetMapping("/list/{companyId}/{taxOffice}")
    @ResponseBody
    public Object list(@PathVariable long companyId,
                        @PathVariable String taxOffice) {

        return taxMdOfficeCategoryFactory.list(companyId, TaxOffice.valueOf(taxOffice));
    }
}
