package com.yun9.service.tax.controller;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryYhItem;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.framework.web.commons.Pageable;
import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.PageParam;
import com.yun9.framework.web.commons.annotation.QueryParam;
import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.TaxInstanceCategoryYhFactory;
import com.yun9.service.tax.core.dto.BizTaxYhItemDTO;
import com.yun9.service.tax.core.impl.PageCommon;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by werewolf on  2018/5/10.
 */
@Controller
@RequestMapping("instance/category/yh")
public class TaxInstanceCategoryYhController {


    @Autowired
    private TaxInstanceCategoryYhFactory taxInstanceCategoryYhFactory;


    /**
     * 企业说的税A列表
     *
     * @param clientOrgTreeId
     * @param state
     * @param query           查询条件
     * @param pageable        分页参数
     * @return
     */
    @GetMapping("/list/{clientOrgTreeId}/{state}")
    @ResponseBody
    public Pagination<HashMap> list(@PathVariable long clientOrgTreeId,
                                    @PathVariable BizTaxInstanceCategory.State state,
                                    @QueryParam QueryJson query, @PageParam Pageable pageable) {

        List<Long> accountCycleIds = Arrays.asList(query.getLongArray("accountCycleIds")
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误,没有会计区间")));


        Map<String, Object> params = new HashMap() {{
            put("clientSn", query.getString("clientSn").orElse(null));
            put("companyName", query.getString("companyName").orElse(null));
            put("taxAreaId", query.getLong("taxAreaId").orElse(null));//税区
            put("taxType", query.getString("taxType").orElse(null));//纳税方式[small小规模][normal一般纳税人][personal]个体户'
            put("taxOffice", query.getString("taxOffice").orElse(null)); //税局 [gs ds]
            put("processCodeId", query.getString("processCodeId").orElse(null));//办理状态Id
            put("processState", query.getString("processState").orElse(null));//办理状态
            put("id", query.getString("id").orElse(null));//个税企业id
            put("audit", query.getLong("audit").orElse(null));//审核
            put("cycleType", query.getString("cycleType").orElse(null));
            put("taxOfficeConfirm", query.getString("taxOfficeConfirm").orElse(null));//税局是否启用
            put("declareType", query.getString("declareType").orElse(null));//申报方式
            put("taxPayAmount", query.getString("taxPayAmount").orElse(null));//应纳税额度（系统）
            put("realPayAmount", query.getString("realPayAmount").orElse(null));//应纳税额度（税局）

            //todo 标签
            put("taxAuditType", query.getString("taxAuditType").orElse(null));//税局是否启用
            put("taxDeclareType", query.getString("taxDeclareType").orElse(null));//税额状态
            put("instClientStateType", query.getString("instClientStateType").orElse(null));//客户停止服务
        }};

        params = PageCommon.label2params(params);


        return taxInstanceCategoryYhFactory.pageByState(accountCycleIds, clientOrgTreeId, state, pageable.getPage(), pageable.getLimit(), params);
    }
    
    @RequestMapping(value = "/verify/confirmed",method = RequestMethod.PUT)
    @ResponseBody
    public void confirmed(@RequestBody BizTaxYhItemDTO bizTaxYhItemDTO, @User UserDetail userDetail){
        taxInstanceCategoryYhFactory.confirmed(bizTaxYhItemDTO,userDetail.getInstUserId());
    }
    @RequestMapping(value = "/verify/unconfirmed/{bizTaxInstanceCategoryYhId}",method = RequestMethod.PUT)
    @ResponseBody
    public void unconfirmed(@PathVariable long bizTaxInstanceCategoryYhId, @User UserDetail userDetail){
        taxInstanceCategoryYhFactory.unconfirmed(bizTaxInstanceCategoryYhId,userDetail.getInstUserId());
    }

    @RequestMapping(value = "/item", method = RequestMethod.POST)
    @ResponseBody
    public Object addYhItem(@RequestBody List<BizTaxInstanceCategoryYhItem> yhItemList) {

        if (null == yhItemList || yhItemList.size() == 0) {

            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "参数错误,传递的保存数据为空!");

        }

        return taxInstanceCategoryYhFactory.addYhItem(yhItemList);

    }


    @RequestMapping(value = "/item", method = RequestMethod.GET)
    @ResponseBody
    public Object getItem(@QueryParam QueryJson query, @PageParam Pageable pageable) {


        Map<String, Object> params = query.getAll();

        if (null == params || params.size() == 0 || null == params.get("instanceCategoryYhId")) {

            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "参数错误,传递参数为空");
        }

        String categoryYhId = params.get("instanceCategoryYhId").toString();

        if(StringUtils.isEmpty(categoryYhId)) {

            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "参数错误,传递参数为空");
        }


        Map<String, Object> params_1 = new HashMap() {{
            put("instanceCategoryYhId", categoryYhId);
        }};

        return taxInstanceCategoryYhFactory.getYhItem(pageable.getPage(),pageable.getLimit(),params_1);

    }

    @RequestMapping(value = "/item/getVat/{bizTaxInstanceCategoryYhId}", method = RequestMethod.GET)
    @ResponseBody
    public Map<String,Object> getVat(@PathVariable long bizTaxInstanceCategoryYhId){
        return taxInstanceCategoryYhFactory.getVat(bizTaxInstanceCategoryYhId);
    }
    @RequestMapping(value = "verify/batchAudit/{yhIds}",method = RequestMethod.PUT)
    @ResponseBody
    public void batchAudit(@PathVariable Long[] yhIds,@User UserDetail userDetail) {
        List<Long> ids = Arrays.asList(yhIds);
        if (com.yun9.commons.utils.CollectionUtils.isEmpty(ids)){
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "参数错误，没有批量审核的ID");
        }
        taxInstanceCategoryYhFactory.batchAudit(ids,userDetail.getInstUserId());
    }
}
