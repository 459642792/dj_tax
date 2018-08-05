package com.yun9.service.tax.controller;

import com.yun9.biz.tax.BizTaxMdMsgCodeService;
import com.yun9.biz.tax.BizTaxPropertiesService;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdMsgCode;
import com.yun9.commons.utils.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by zhengzb on 2018/5/7.
 */
@RestController
@RequestMapping("/msg/code")
public class MsgCodeController {

    @Autowired
    BizTaxMdMsgCodeService bizTaxMdMsgCodeService;
    @Autowired
    BizTaxPropertiesService bizTaxPropertiesService;

    /**
     * 错误码字典表
     *
     * @return
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public Object list() {
        List<BizTaxMdMsgCode> bizTaxMdMsgCodes = bizTaxMdMsgCodeService.findAll();
        if (CollectionUtils.isNotEmpty(bizTaxMdMsgCodes)) {
            return new ArrayList() {{
                bizTaxMdMsgCodes.forEach(v -> {
                    add(new HashMap() {{
                        put("id", v.getId());
                        put("code", v.getCode());
                        put("type", v.getType());
                        put("name", v.getName());
                    }});
                });
            }};
        }
        return new ArrayList();
    }
}
