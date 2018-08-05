package com.yun9.service.tax.controller;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.framework.web.commons.Pageable;
import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.PageParam;
import com.yun9.framework.web.commons.annotation.QueryParam;
import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.TaxInstanceCategoryPersonalPayrollFactory;
import com.yun9.service.tax.core.impl.PageCommon;
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
@RequestMapping("instance/category/personal/payroll")
public class TaxInstanceCategoryPersonalPayrollController {


    @Autowired
    private TaxInstanceCategoryPersonalPayrollFactory taxInstanceCategoryPersonalPayrollFactory;


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

            put("sourceType", query.getString("sourceType").orElse(null));//申报类型[first首次申报][last税局上月][sso社保局][hand手动]
            put("taxAmount", query.getString("mdAreaId").orElse(null));//
            put("cycleType", query.getString("cycleType").orElse(null));
            put("popleNum", query.getString("popleNum").orElse(null));//申报人数
            put("taxPayAmount", query.getString("taxPayAmount").orElse(null));//应纳税额（系统）
            put("realPayAmount", query.getString("realPayAmount").orElse(null));//应纳税额（税局）
            put("taxOfficeConfirm", query.getString("taxOfficeConfirm").orElse(null));//税局是否启用
            put("declareType", query.getString("declareType").orElse(null));//申报方式

            //todo 标签
            put("taxAuditType", query.getString("taxAuditType").orElse(null));//税局是否启用
            put("taxDeclareType", query.getString("taxDeclareType").orElse(null));//税额状态
            put("instClientStateType", query.getString("instClientStateType").orElse(null));//客户停止服务
        }};

        params = PageCommon.label2params(params);

        return taxInstanceCategoryPersonalPayrollFactory.pageByState(accountCycleIds, clientOrgTreeId, state, pageable.getPage(), pageable.getLimit(), params);
    }


    /**
     * 修改收入所属期
     *
     * @param id             个税工资薪金ID
     * @param accountCycleId 会计区间ID
     * @param userDetail     操作人
     */
    @RequestMapping(value = "/change/income/{id}/{accountCycleId}", method = RequestMethod.PUT)
    @ResponseBody
    public void changeIncomeAccountCycle(@PathVariable long id, @PathVariable long accountCycleId, @User UserDetail userDetail) {
        taxInstanceCategoryPersonalPayrollFactory.changeIncomeAccountCycle(id, accountCycleId, userDetail.getInstUserId());
    }

}
