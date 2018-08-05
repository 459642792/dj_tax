package com.yun9.service.tax.controller;

import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.TaxInstanceCategoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * @program: service-tax
 * @description: ${description}
 * @author: leigm
 * @create: 2018-06-08 11:57
 **/
@Controller
@RequestMapping("/bank")
public class TaxCompanyBankController {

    @Autowired
    TaxInstanceCategoryFactory taxInstanceCategoryFactory;

    /**
     * 修改首选纳税银行
     *
     * @param id
     * @param userDetail
     */
    @RequestMapping(value = "/first/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public void update(@PathVariable long id, @User UserDetail userDetail) {
        taxInstanceCategoryFactory.updateFirstTaxBank(id);
    }
}
