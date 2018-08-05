package com.yun9.service.tax.controller;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.framework.web.commons.Pageable;
import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.PageParam;
import com.yun9.framework.web.commons.annotation.QueryParam;
import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.TaxInstanceCategoryPersonalBusinessFactory;
import com.yun9.service.tax.core.dto.BizTaxBusinessSheetDTO;
import com.yun9.service.tax.core.dto.BizTaxVatImportSheetDTO;
import com.yun9.service.tax.core.impl.PageCommon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Created by werewolf on  2018/5/10.
 */
@Controller
@RequestMapping("instance/category/personal/business")
public class TaxInstanceCategoryPersonalBusinessController {


    @Autowired
    private TaxInstanceCategoryPersonalBusinessFactory taxInstanceCategoryPersonalBusinessFactory;
    @Value("${file.upload.path}")
    private String path;

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
            put("taxAreaId", query.getLong("mdAreaId").orElse(null));//税区
            put("taxType", query.getString("taxType").orElse(null));//纳税方式
            put("taxOffice", query.getString("taxOffice").orElse(null));
            put("processCodeId", query.getString("processCodeId").orElse(null));//办理状态Id
            put("processState", query.getString("processState").orElse(null));//办理状态
            put("id", query.getString("id").orElse(null));//个税企业id
            put("audit", query.getLong("audit").orElse(null));//审核

            put("type", query.getString("type").orElse(null));//核定类型
            put("auditType", query.getString("auditType").orElse(null));//纳税方式
            put("vatSaleAmount", query.getLong("vatSaleAmount").orElse(null));//审核状态
            put("amountAuditState", query.getLong("amountAuditState").orElse(null));//审核状态
            put("taxPayAmount", query.getString("taxPayAmount").orElse(null));//应纳税额度
            put("cycleType", query.getString("cycleType").orElse(null));
            put("taxOfficeConfirm", query.getString("taxOfficeConfirm").orElse(null));//税局是否启用
            put("declareType", query.getString("declareType").orElse(null));//申报方式
            put("realPayAmount", query.getString("realPayAmount").orElse(null));//应纳税额度（税局）

            //todo 标签
            put("taxAuditType", query.getString("taxAuditType").orElse(null));//税局是否启用
            put("taxDeclareType", query.getString("taxDeclareType").orElse(null));//税额状态
            put("instClientStateType", query.getString("instClientStateType").orElse(null));//客户停止服务

        }};

        params = PageCommon.label2params(params);


        return taxInstanceCategoryPersonalBusinessFactory.pageByState(accountCycleIds, clientOrgTreeId, state, pageable.getPage(), pageable.getLimit(), params);
    }


    /**
     * 下载模板
     */

    @RequestMapping(value = "/download/excel/{categoryBusinessIds}", method = RequestMethod.GET)
    @ResponseBody
    public void getSystemExcel(HttpServletRequest request, HttpServletResponse response,
                               @PathVariable Long[] categoryBusinessIds) {
        List<Long> ids = Arrays.asList(categoryBusinessIds);
        if (CollectionUtils.isEmpty(ids)) {
            throw BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误，没有选择下载对象");
        }
        taxInstanceCategoryPersonalBusinessFactory.downloadExcel(request, response, ids);
    }

    @RequestMapping(value = "/upload/{instId}/{mdAccountCycleId}", method = RequestMethod.POST)
    @ResponseBody
    public Object upload(@PathVariable Long instId, @PathVariable long mdAccountCycleId,
                         @User UserDetail userDetail,
                         MultipartHttpServletRequest request) {
        Iterator<String> itr = request.getFileNames();
        MultipartFile mpf = null;
        Object object = null;
        while (itr.hasNext()) {
            mpf = request.getFile(itr.next());
        }
        try {
            request.setCharacterEncoding("UTF-8");
            BizTaxVatImportSheetDTO importSheetDTO = new BizTaxVatImportSheetDTO();
            importSheetDTO.setMdAccountCycleId(mdAccountCycleId);
            importSheetDTO.setProcessBy(userDetail.getFtId());
            importSheetDTO.setFileData(mpf.getBytes());
            importSheetDTO.setFileOriginalName(mpf.getOriginalFilename());
            importSheetDTO.setFileUploadPath(path);
            importSheetDTO.setInstId(instId);
            importSheetDTO.setUserId(userDetail.getId());
            object = taxInstanceCategoryPersonalBusinessFactory.parseVatExcel(importSheetDTO);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return JSON.toJSON(object);
    }


    /**
     * 保存发票
     */
    @PostMapping(value = "/save/{id}/{state}")
    @ResponseBody
    public void save(@PathVariable long id, @PathVariable int state, @User UserDetail userDetail, @RequestBody BizTaxBusinessSheetDTO bizTaxBusinessSheetDTO) {
        taxInstanceCategoryPersonalBusinessFactory.save(bizTaxBusinessSheetDTO, id, state, userDetail.getId());
    }
}
