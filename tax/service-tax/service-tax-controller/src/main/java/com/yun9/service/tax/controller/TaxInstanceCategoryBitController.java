package com.yun9.service.tax.controller;


import com.alibaba.fastjson.JSON;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryBit;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.framework.web.commons.Pageable;
import com.yun9.framework.web.commons.QueryJson;
import com.yun9.framework.web.commons.annotation.PageParam;
import com.yun9.framework.web.commons.annotation.QueryParam;
import com.yun9.framework.web.spring.annotation.User;
import com.yun9.framework.web.spring.auth.UserDetail;
import com.yun9.service.tax.core.TaxInstanceCategoryBitFactory;
import com.yun9.service.tax.core.dto.BizTaxBitImportDTO;
import com.yun9.service.tax.core.dto.BizTaxBitImportSheetDTO;
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
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by werewolf on  2018/5/10.
 */
@Controller
@RequestMapping("instance/category/bit")
public class TaxInstanceCategoryBitController {

    @Value("${file.upload.path}")
    private String path;
    @Autowired
    private TaxInstanceCategoryBitFactory taxInstanceCategoryBitFactory;

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
            put("taxType", query.getString("taxType").orElse(null));//纳税方式
            put("taxOffice", query.getString("taxOffice").orElse(null));
            put("processCodeId", query.getString("processCodeId").orElse(null));//办理状态Id
            put("processState", query.getString("processState").orElse(null));//办理状态
            put("id", query.getString("id").orElse(null));//所得税id
            put("cycleType", query.getString("cycleType").orElse(null));
            put("taxOfficeConfirm", query.getString("taxOfficeConfirm").orElse(null));//税局是否启用
            put("audit", query.getLong("audit").orElse(null));//审核

            put("bitType", query.getString("bitType").orElse(null));//企业所得税类型
            put("auditType", query.getString("auditType").orElse(null));//纳税方式
            put("vatSaleAmount", query.getLong("vatSaleAmount").orElse(null));//审核状态
            put("amountAuditState", query.getLong("amountAuditState").orElse(null));//审核状态
            put("amountAuditState", query.getLong("amountAuditState").orElse(null));//审核状态
            put("taxPayAmount", query.getString("taxPayAmount").orElse(null));//应纳税额度（系统）
            put("realPayAmount", query.getString("realPayAmount").orElse(null));//应纳税额度（税局）
            put("declareType", query.getString("declareType").orElse(null));//申报方式

            //todo 标签
            put("taxAuditType", query.getString("taxAuditType").orElse(null));//税局是否启用
            put("taxDeclareType", query.getString("taxDeclareType").orElse(null));//税额状态
            put("instClientStateType", query.getString("instClientStateType").orElse(null));//客户停止服务

        }};

        params = PageCommon.label2params(params);

        return taxInstanceCategoryBitFactory.pageByState(accountCycleIds, clientOrgTreeId, state, pageable.getPage(), pageable.getLimit(), params);
    }


    /**
     * 企业说得税列表统计
     *
     * @param instId
     * @param accountCycleId
     * @param clientOrgTreeId
     * @param state
     * @param search
     * @return
     */
    @GetMapping("/total/{instId}/{accountCycleId}/{clientOrgTreeId}/{state}")
    @ResponseBody
    public int total(@PathVariable long instId,
                     @PathVariable long accountCycleId,
                     @PathVariable long clientOrgTreeId,
                     @PathVariable BizTaxInstanceCategory.State state,
                     @QueryParam QueryJson search) {

        String clientSn = search.getString("clientSn").orElse(null); //客户编号
        String clientName = search.getString("clientName").orElse(null); //客户名称

        return 0;
    }

    @RequestMapping(value = "/download/excel/{mdAccountCycleId}/{categoryIds}", method = RequestMethod.GET)
    @ResponseBody
    public void getSystemExcel(HttpServletRequest request, HttpServletResponse response, @PathVariable Long[] categoryIds, @PathVariable long mdAccountCycleId) {
        List<Long> ids = Arrays.asList(categoryIds);
        if (CollectionUtils.isEmpty(ids)) {
            throw BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误，没有选择下载对象");
        }

        //todo 检查传入的 categoryIds 是否属于当前登录机构

        //下载模板
        taxInstanceCategoryBitFactory.downloadExcel(request, response, ids, mdAccountCycleId);
    }

    /**
     * 导入数据
     *
     * @return
     */
    @RequestMapping(value = "/upload/{instId}/{mdAccountCycleId}/{taxOffice}", method = RequestMethod.POST)
    @ResponseBody
    public Object upload(@PathVariable long instId,@PathVariable long mdAccountCycleId,@PathVariable TaxOffice taxOffice,
        @User UserDetail userDetail,
        MultipartHttpServletRequest request) {
        Iterator<String> itr = request.getFileNames();
        MultipartFile mpf = null;
        BizTaxBitImportDTO bizTaxBitImportDTO = null;
        while (itr.hasNext()) {
            mpf = request.getFile(itr.next());
        }
        try {
            request.setCharacterEncoding("UTF-8");
            BizTaxBitImportSheetDTO importSheetDTO=new BizTaxBitImportSheetDTO();
            importSheetDTO.setMdAccountCycleId(mdAccountCycleId);
            importSheetDTO.setFileData(mpf.getBytes());
            importSheetDTO.setFileOriginalName(mpf.getOriginalFilename());
            importSheetDTO.setFileUploadPath(path);
            importSheetDTO.setInstId(instId);
            importSheetDTO.setUserId(userDetail.getId());
            importSheetDTO.setTaxOffice(taxOffice);
            bizTaxBitImportDTO = taxInstanceCategoryBitFactory.parseBitExcel(importSheetDTO);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (null == bizTaxBitImportDTO) {
            throw BizTaxException.build(BizTaxException.Codes.DateError, "解析Excel失败!");
        }
        return JSON.toJSON(bizTaxBitImportDTO);
    }
    /**
     *
     * 确认利润核算
     */
    @RequestMapping(value = "/profit/confirmed",method = RequestMethod.PUT)
    @ResponseBody
    public void profitAccounting(@RequestBody BizTaxInstanceCategoryBit bizTaxInstanceCategoryBit,@User UserDetail userDetail)throws IllegalAccessException{
        if (bizTaxInstanceCategoryBit == null){
            throw BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "操作失败,传入数据为空");
        }
        taxInstanceCategoryBitFactory.profitAccounting(bizTaxInstanceCategoryBit,userDetail.getInstUserId(),"确认利润核算");
    }

    /**
     * 撤销确认利润核算
     *
     */
    @RequestMapping(value = "/profit/unconfirmed/{id}",method = RequestMethod.PUT)
    @ResponseBody
    public void unprofitAccounting(@PathVariable long id,@User UserDetail userDetail, @QueryParam QueryJson query ){
        String remark = query.getString("remark").orElse("撤销确认利润核算");
        taxInstanceCategoryBitFactory.cancelAudit(id,userDetail.getInstUserId(),remark);
    }

    /**
     * by-yunjie 2018-07-09
     * 根据增值税生成销售额
     * @param id
     *
     */
    @RequestMapping(value = "/profit/getSaleAmountByVat/{id}",method = RequestMethod.GET)
    @ResponseBody
    public BigDecimal getSaleAmountByVat(@PathVariable long id, @User UserDetail userDetail){
        return taxInstanceCategoryBitFactory.getSaleAmountByVat(id,userDetail.getInstUserId());//userDetail.getInstUserId());
    }

    /**
     * by-yunjie 2018-07-09
     * 根据个税生成从业人数
     * @param id
     *
     */
    @RequestMapping(value = "/profit/getEmployeeNumberByVat/{id}",method = RequestMethod.GET)
    @ResponseBody
    public int getEmployeeNumberByVat(@PathVariable long id, @User UserDetail userDetail){
        return taxInstanceCategoryBitFactory.getEmployeeNumberByPersonal(id,userDetail.getInstUserId());//userDetail.getInstUserId());
    }

    /**
     * 暂存(保存利润核算)
     * 临时
     */
    @RequestMapping(value = "/profit/tempSave",method = RequestMethod.PUT)
    @ResponseBody
    public void saveProfit(@RequestBody BizTaxInstanceCategoryBit bizTaxInstanceCategoryBit,@User UserDetail userDetail)throws IllegalAccessException{
        if (bizTaxInstanceCategoryBit == null){
            throw BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "操作失败,传入数据为空");
        }
        taxInstanceCategoryBitFactory.saveProfit(bizTaxInstanceCategoryBit,userDetail.getInstUserId(),"暂存利润核算");
    }
    /**
     * 批量审核
     */
    @RequestMapping(value = "verify/batchAudit/{bitIds}",method = RequestMethod.PUT)
    @ResponseBody
    public void batchAudit(@PathVariable Long[] bitIds,@User UserDetail userDetail) {
        List<Long> ids = Arrays.asList(bitIds);
        if (com.yun9.commons.utils.CollectionUtils.isEmpty(ids)){
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "参数错误，没有批量审核的ID");
        }
        taxInstanceCategoryBitFactory.batchAudit(ids,userDetail.getInstUserId());
    }
}
