package com.yun9.service.tax.controller;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryDeduct;
import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.QueryParam;
import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.TaxInstanceCategoryDeductFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: service-tax
 * @description: ${description}
 * @author: leigm
 * @create: 2018-05-31 11:25
 **/

@Controller
@RequestMapping("instance/category/deduct")
public class TaxInstanceCategoryDeductController {

    @Autowired
    private TaxInstanceCategoryDeductFactory taxInstanceCategoryDeductFactory;

    /**
     * 查询扣款列表
     *
     * @param instanceCategoryId 税种申报ID
     * @return
     */
    @RequestMapping("/list/{instanceCategoryId}")
    @ResponseBody
    public Object list(@PathVariable long instanceCategoryId, @QueryParam QueryJson query) {
        Map<String, Object> params = new HashMap() {{
            put("type", query.getString("type").orElse(null)); //支付方式[bank银行账户][wx微信支付]
            put("state", query.getString("state").orElse(null)); //状态[none未支付][success支付成功][expire过期][exception支付错误]
            put("expiry", query.getString("expiry").orElse(null)); //支付有效期
        }};

        return taxInstanceCategoryDeductFactory.listByInstanceCategoryIdAndParams(instanceCategoryId, params);
    }


    /**
     * 撤销扣款
     *
     * @param id         扣款ID
     * @param userDetail 操作人信息
     */
    @PutMapping("/cancel/{id}")
    @ResponseBody
    public void cancel(@PathVariable long id, @User UserDetail userDetail) {
        taxInstanceCategoryDeductFactory.cancel(id, userDetail.getInstUserId());

    }

}
