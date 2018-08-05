package com.yun9.service.tax.controller;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.framework.web.commons.Pageable;
import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.PageParam;
import com.yun9.framework.web.commons.annotation.QueryParam;
import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.TaxInstanceCategoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@Controller
@RequestMapping("/instance")
public class TaxInstanceController {
    @Autowired
    TaxInstanceCategoryFactory taxInstanceCategoryFactory;


//    /**
//     * 统计发起申报页面异常状态的客户数量
//     *
//     * @param orgTreeId
//     * @param query
//     * @return
//     */
//    @GetMapping("/total/exception/{orgTreeId}")
//    @ResponseBody
//    public HashMap totalByException(@PathVariable long orgTreeId,
//                                    @QueryParam QueryJson query) {
//
//        Map<String, Object> params = new HashMap() {{
//            put("clientSn", query.getString("clientSn").orElse(null));
//            put("companyName", query.getString("companyName").orElse(null));
//            put("taxSn", query.getStringArray("taxSn").orElse(null));
//            put("accountCycleIds", query.getLongArray("accountCycleIds").orElse(null));
//            put("taxOffices", query.getStringArray("taxOffices").orElse(null));
//            put("companyTaxTypes", query.getStringArray("taxType").orElse(null));
//            put("mdAreaIds", query.getLongArray("taxAreaId").orElse(null));
//        }};
//
//        return taxInstanceCategoryFactory.totalByException(orgTreeId, null, params);
//    }

    /**
     * 发起处理列表
     *
     * @param orgTreeId 组织ID
     * @param query     过滤条件
     * @param pageable  分页查询
     * @return
     */
    @GetMapping("/list/{orgTreeId}")
    @ResponseBody
    public Pagination<HashMap> list(@PathVariable long orgTreeId, @QueryParam QueryJson query, @PageParam Pageable pageable) {

        //检查快捷键区间id集合
        List<Long> accountCycleIds = Arrays.asList(query.getLongArray("accountCycleIds")
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误，没有公司对象")));



        HashMap<String, Object> params = new HashMap() {{
            put("clientSn", query.getString("clientSn").orElse(null));
            put("companyName", query.getString("companyName").orElse(null));
            put("taxType", query.getString("taxType").orElse(null));
            put("taxOffice", query.getString("taxOffice").orElse(null)); //税局 [gs ds]
            put("processCodeId", query.getLong("processCodeId").orElse(null));

            put("taxAreaId", query.getLong("taxAreaId").orElse(null));
            put("cycleTypes", query.getStringArray("cycleTypes").orElse(null));
            put("sendTypes", query.getStringArray("sendTypes").orElse(null));

            put("id", query.getString("id").orElse(null));//id
        }};

        List<Object> processStates = new ArrayList();
        String processState = query.getString("processState").orElse(null);
        if (processState != null) {
            processStates.add(BizTaxInstanceCategory.ProcessState.valueOf(processState));
        } else {
            processStates.add(BizTaxInstanceCategory.ProcessState.exception);
            processStates.add(BizTaxInstanceCategory.ProcessState.process);
        }

        String[] taxSnStrs = query.getStringArray("taxSns").orElse(null);
        List taxSns = new ArrayList() {{
            if (taxSnStrs != null) {
                Arrays.asList(taxSnStrs).forEach(v -> {
                    add(TaxSn.valueOf(v));
                });
            }
        }};
        return taxInstanceCategoryFactory.pageByState(accountCycleIds, taxSns, orgTreeId, processStates, pageable.getPage(), pageable.getLimit(), params);
    }


    /**
     * 确认已报
     *
     * @param id         税种ID
     * @param userDetail 用户
     */
    @RequestMapping(value = "/confirm/declare/{id}/{taxSn}/{cycleType}", method = RequestMethod.PUT)
    @ResponseBody
    public void confirmDeclare(@PathVariable long id, @PathVariable TaxSn taxSn, @PathVariable CycleType cycleType, @QueryParam QueryJson query, @User UserDetail userDetail) {
        HashMap<String, Object> params = new HashMap() {{
            put("remark", query.getString("remark").orElse("确认已申报操作，从\"发起\"状态改为\"完成\"状态"));
            put("type", query.getString("type"));
        }};
        taxInstanceCategoryFactory.confirmDeclare(id, taxSn, cycleType, userDetail.getInstUserId(), params);
    }

}
