package com.yun9.service.tax.controller;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryFzItem;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.framework.web.commons.Pageable;
import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.PageParam;
import com.yun9.framework.web.commons.annotation.QueryParam;
import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.TaxInstanceCategoryFzFactory;
import com.yun9.service.tax.core.dto.BizTaxFzItemDTO;
import com.yun9.service.tax.core.impl.PageCommon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;


/**
 * Created by werewolf on  2018/5/10.
 */
@Controller
@RequestMapping("instance/category/fz")
public class TaxInstanceCategoryFzController {


    @Autowired
    private TaxInstanceCategoryFzFactory taxInstanceCategoryFzFactory;


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
            put("processState", query.getString("processState").orElse(null));//办理状态[none未办理][process办理中][success完成]
            put("audit", query.getLong("audit").orElse(null));//审核
            put("id", query.getString("id").orElse(null));//附加税id
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


        return taxInstanceCategoryFzFactory.pageByState(accountCycleIds, clientOrgTreeId, state, pageable.getPage(), pageable.getLimit(), params);
    }
    /**
     * 
     * 附征税审核
     */
    @RequestMapping(value = "/verify/confirmed",method = RequestMethod.PUT)
    @ResponseBody
    public void verify(@RequestBody BizTaxFzItemDTO bizTaxFzItemDTO,@User UserDetail userDetail){
         taxInstanceCategoryFzFactory.confirmed(bizTaxFzItemDTO,userDetail.getInstUserId());
    }

    @GetMapping("item/list/{instanceCategoryFzId}")
    @ResponseBody
    public List<BizTaxInstanceCategoryFzItem> itemList(@PathVariable long instanceCategoryFzId) {
        return taxInstanceCategoryFzFactory.itemList(instanceCategoryFzId).orElse(Collections.EMPTY_LIST);
    }

    @PostMapping("item/save/{instanceCategoryFzId}")
    @ResponseBody
    public boolean saveItem(@PathVariable long instanceCategoryFzId, @RequestBody List<BizTaxInstanceCategoryFzItem> itemList) throws IllegalAccessException {
        taxInstanceCategoryFzFactory.saveItem(instanceCategoryFzId,itemList);
        return true;
    }

    @RequestMapping(value = "verify/unconfirmed/{bizTaxInstanceCategoryFzId}",method = RequestMethod.PUT)
    @ResponseBody
    public void unconfirmed(@PathVariable long bizTaxInstanceCategoryFzId,@User UserDetail userDetail){
        taxInstanceCategoryFzFactory.unconfirmed(bizTaxInstanceCategoryFzId,userDetail.getInstUserId());
    }
    @RequestMapping(value = "item/getVatAndSoq/{bizTaxInstanceCategoryFzId}",method = RequestMethod.GET)
    @ResponseBody
    public Map<String,Object> getVatAndSoq(@PathVariable long bizTaxInstanceCategoryFzId) {
       return taxInstanceCategoryFzFactory.getVatAndSoq(bizTaxInstanceCategoryFzId);
    }
    /**
     * 批量审核
     */
    @RequestMapping(value = "verify/batchAudit/{fzIds}",method = RequestMethod.PUT)
    @ResponseBody
    public void batchAudit(@PathVariable Long[] fzIds,@User UserDetail userDetail) {
        List<Long> ids = Arrays.asList(fzIds);
        if (CollectionUtils.isEmpty(ids)){
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "参数错误，没有批量审核的ID");  
        }
        taxInstanceCategoryFzFactory.batchAudit(ids,userDetail.getInstUserId());
    }
}
