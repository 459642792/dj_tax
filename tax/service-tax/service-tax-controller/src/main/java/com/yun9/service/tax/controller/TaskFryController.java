package com.yun9.service.tax.controller;


import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.QueryParam;
import com.yun9.service.tax.core.TaxInstanceCategoryFactory;
import com.yun9.service.tax.core.TaxInstanceCategoryFryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;


/**
 * 财务报表年报
 */
@Controller
@RequestMapping("/instance/category/fry")
public class TaskFryController {
    public static final Logger logger = LoggerFactory.getLogger(TaskFryController.class);

    @Autowired
    private TaxInstanceCategoryFryFactory taxInstanceCategoryFryFactory;

    @Autowired
    TaxInstanceCategoryFactory taxInstanceCategoryFactory;





//    /**
//     * 统计税种异常状态的客户数量
//     *
//     * @param accountCycleId
//     * @param orgTreeId
//     * @param state
//     * @param query
//     * @return
//     */
//    @GetMapping("/total/exception/{state}/{accountCycleId}/{orgTreeId}")
//    @ResponseBody
//    public HashMap totalByException(@PathVariable long accountCycleId,
//                                @PathVariable long orgTreeId,
//                                @PathVariable String state,
//                                @QueryParam QueryJson query) {
//
//        //1. TODO instId 是否需要？
//
//        Map<String, Object> params = new HashMap() {{
//            put("clientSn", query.getString("clientSn").orElse(null));
//            put("companyName", query.getString("companyName").orElse(null));
//        }};
//        List<TaxSn> taxSns = new ArrayList() {{
//            add(TaxSn.y_fr);
//        }};
//        return taxInstanceCategoryFactory.totalByException(taxSns, accountCycleId, orgTreeId, BizTaxInstanceCategory.State.valueOf(state), params);
//    }


    /**
     * 统计财报申报过程中各种状态下的客户数量
     * 注意：如果有客户名称过滤，则报表统计只针对该客户名称
     *
     * @param accountCycleId 会计区间
     * @param query          过滤条件
     * @return
     */

    @GetMapping("/total/state/{accountCycleId}/{orgTreeId}")
    @ResponseBody
    public HashMap totalByState(@PathVariable long accountCycleId,
                                            @PathVariable long orgTreeId,
                                            @QueryParam QueryJson query) {

        //1. TODO instId 是否需要？

        Map<String, Object> params = new HashMap() {{
            put("clientSn", query.getString("clientSn").orElse(null));
            put("companyName", query.getString("companyName").orElse(null));
        }};

//        return taxInstanceCategoryFryFactory.totalByState(accountCycleId, orgTreeId, params);
        return null;
    }









}

