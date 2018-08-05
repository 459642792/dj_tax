package com.yun9.service.tax.core.impl;

import com.yun9.biz.md.BizMdCompanyAccountService;
import com.yun9.biz.md.BizMdCompanyService;
import com.yun9.biz.md.BizMdInstOrgTreeClientService;
import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.dto.CompanyDataDTO;
import com.yun9.biz.md.domain.dto.InstClientHelper;
import com.yun9.biz.md.domain.entity.BizMdCompany;
import com.yun9.biz.md.domain.entity.BizMdCompanyAccount;
import com.yun9.biz.tax.BizTaxCompanyCategoryService;
import com.yun9.biz.tax.BizTaxCompanyTaxService;
import com.yun9.biz.tax.BizTaxMdOfficeCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyCategory;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyTax;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategory;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.TaxCompanyTaxFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by zhengzb on 2018/6/26.
 */
@Component
public class TaxCompanyTaxFactoryImpl implements TaxCompanyTaxFactory {

    @Autowired
    BizMdInstOrgTreeClientService bizMdInstOrgTreeClientService;

    @Autowired
    BizTaxCompanyCategoryService bizTaxCompanyCategoryService;
    @Autowired
    BizTaxMdOfficeCategoryService bizTaxMdOfficeCategoryService;

    @Autowired
    BizTaxCompanyTaxService bizTaxCompanyTaxService;

    @Autowired
    BizMdCompanyService bizMdCompanyService;

    @Autowired
    PageCommon pageCommon;

    @Override
    public Pagination list(long orgTreeId, int page, int limit, HashMap<String, Object> params) {
        Pagination pagination = new Pagination();

        Map<String, CompanyAccountDTO> companyAccountMap ;
        //查询出所有的客户信息
        List<CompanyDataDTO> companyDataDTOS = bizMdInstOrgTreeClientService.findCompanyDataDTOByOrgTreeId(orgTreeId,params);

        if(CollectionUtils.isEmpty(companyDataDTOS)){
            return pagination.setContent(new ArrayList());
        }

        List<Long> companyIds = companyDataDTOS.stream().map(v->v.getCompanyId()).collect(Collectors.toList());

        //在过滤登陆方式
        if(null != params.get("loginType")) {
            HashMap<String, Object> filterResult = pageCommon.filterLoginType(companyIds, params);
            companyIds = (List<Long>) filterResult.get("companyIds");
            companyAccountMap = (Map<String, CompanyAccountDTO>) filterResult.get("companyAccountMap");
        }else{
            companyAccountMap = pageCommon.getCompanyAccountDTOS(companyIds);
        }

        if(CollectionUtils.isEmpty(companyIds)){
            return pagination.setContent(new ArrayList());
        }

        //到tax中补充相关的税种信息
        List<BizTaxCompanyCategory> bizTaxCompanyCategories = bizTaxCompanyCategoryService.findByCompanyIdsAndParmas(companyIds,params);

        //支持核定税种的公司id
        if(null != params.get("taxSnName")) {
            companyIds = Optional.ofNullable(bizTaxCompanyCategories).orElse(new ArrayList<>()).stream().map(v -> v.getBizMdCompanyId()).collect(Collectors.toList());

            if (CollectionUtils.isEmpty(companyIds)) {
                return pagination.setContent(new ArrayList());
            }
        }

        //到tax中补充查询国税信息并分页
        Pagination<BizTaxCompanyTax> bizTaxCompanyTaxs = bizTaxCompanyTaxService.pageByState(companyIds,page,limit,params);

        if(bizTaxCompanyTaxs == null || CollectionUtils.isEmpty(bizTaxCompanyTaxs.getContent())){
            return pagination.setContent(new ArrayList());
        }
        pagination.setTotalElements(bizTaxCompanyTaxs.getTotalElements());
        pagination.setTotalPages(bizTaxCompanyTaxs.getTotalPages());

        Map<Long,CompanyDataDTO> companyDetailMaps = companyDataDTOS.stream().collect(Collectors.toMap(CompanyDataDTO::getCompanyId, CompanyDataDTO->CompanyDataDTO));

        Map<Long,List<String>> companyCategorys = new HashMap<>();

        if(CollectionUtils.isNotEmpty(bizTaxCompanyCategories)){
            //所有税种id集合
            List<Long> officeCategoryIds =  bizTaxCompanyCategories.stream().map(v->v.getBizTaxMdOfficeCategoryId()).collect(Collectors.toList());

            List<BizTaxMdOfficeCategory> bizTaxMdOfficeCategories = bizTaxMdOfficeCategoryService.findByIds(officeCategoryIds);

            //存放税种id-税种实例
            final Map<Long,BizTaxMdOfficeCategory> officeCategoriesNames = bizTaxMdOfficeCategories.stream().collect(Collectors.toMap(BizTaxMdOfficeCategory::getId, BizTaxMdOfficeCategory->BizTaxMdOfficeCategory));

            //存放公司-税种集合
            bizTaxCompanyCategories.forEach(v->{
                if(null == companyCategorys.get(v.getBizMdCompanyId())){
                    List<String> categorys = new ArrayList(){{
                        add(officeCategoriesNames.get(v.getBizTaxMdOfficeCategoryId()).getBizTaxMdCategory().getName());
                    }};
                    companyCategorys.put(v.getBizMdCompanyId(),categorys);
                }else{
                    companyCategorys.get(v.getBizMdCompanyId()).add(officeCategoriesNames.get(v.getBizTaxMdOfficeCategoryId()).getBizTaxMdCategory().getName());
                }
            });
        }

        //遍历组装数据
        Map<String, CompanyAccountDTO> finalCompanyAccountMap = companyAccountMap;
        return pagination.setContent(new ArrayList(){{
            bizTaxCompanyTaxs.getContent().forEach(v->{
                add(new HashMap(){{
                    put("instClientId",Optional.ofNullable(companyDetailMaps.get(v.getBizMdCompanyId())).map(v -> v.getInstClientId()).orElse(null));
                    put("companyId",Optional.ofNullable(companyDetailMaps.get(v.getBizMdCompanyId())).map(v -> v.getCompanyId()).orElse(null));
                    put("clientSn",Optional.ofNullable(companyDetailMaps.get(v.getBizMdCompanyId())).map(v -> v.getClientSn()).orElse(null));
                    put("companyName",Optional.ofNullable(companyDetailMaps.get(v.getBizMdCompanyId())).map(v -> v.getCompanyName()).orElse(null));
                    put("taxType",Optional.ofNullable(companyDetailMaps.get(v.getBizMdCompanyId())).map(v -> v.getTaxType()).orElse(null));
                    put("taxAreaId",Optional.ofNullable(companyDetailMaps.get(v.getBizMdCompanyId())).map(v -> v.getTaxAreaId()).orElse(null));
                    put("state",Optional.ofNullable(companyDetailMaps.get(v.getBizMdCompanyId())).map(v -> v.getState()).orElse(null));
                    put("taxNo",Optional.ofNullable(companyDetailMaps.get(v.getBizMdCompanyId())).map(v -> v.getTaxNo()).orElse(null));
                    put("billingType",v.getBillingType());
                    put("invoiceSystem",v.getInvoiceSystem());
                    put("personDeclareType",v.getPersonDeclareType());
                    put("syncState",v.getSyncState());
                    put("processState",v.getProcessState());
                    put("processMessage",v.getProcessMessage());
                    put("processCodeId",v.getProcessCodeId());
                    put("syncAt",v.getSyncAt());
                    put("taxSns", companyCategorys.get(v.getBizMdCompanyId()));
                    put("passwordType", new HashMap() {{
                        put(TaxOffice.gs, Optional.ofNullable(finalCompanyAccountMap.get(v.getBizMdCompanyId() + "_" + TaxOffice.gs + "_" + BizMdCompanyAccount.State.activate)).orElse(null));
                        put(TaxOffice.ds, Optional.ofNullable(finalCompanyAccountMap.get(v.getBizMdCompanyId() + "_" + TaxOffice.ds + "_" + BizMdCompanyAccount.State.activate)).orElse(null));
                    }});
                }});
            });
        }});
    }

    @Override
    public HashMap<String, Object> totalByException(long orgTreeId, HashMap<String, Object> params) {
        HashMap<String,Object> totleMap = new HashMap<>();
        HashMap<String,Object> instClientMap = bizMdInstOrgTreeClientService.findInstIdAndClientIdByParams(orgTreeId,params);

        List<InstClientHelper> instClientHelpers = (List<InstClientHelper>) instClientMap.get("all");

        List<Long> companyIds = Optional.ofNullable(instClientHelpers).orElse(new ArrayList<>()).stream().map(v->v.getCompanyId()).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(companyIds)) {
            return totleMap;
        }

        totleMap = bizTaxCompanyTaxService.totalByException(companyIds,params);

        return totleMap;
    }

    @Override
    public HashMap find(long companyId) {
        BizMdCompany bizMdCompany = Optional.ofNullable(bizMdCompanyService.findById(companyId)).orElseThrow(()-> BizTaxException.throwException(BizTaxException.Codes.BizTaxException,"公司不存在:"+companyId));
        BizTaxCompanyTax bizTaxCompanyTax = Optional.ofNullable(bizTaxCompanyTaxService.findByCompanyId(companyId))
                .orElseThrow(()->BizTaxException.throwException(BizTaxException.Codes.BizTaxException,"公司税务信息不存在:"+companyId));

        return new HashMap(){{
            put("id",bizTaxCompanyTax.getId());
            put("bizMdCompanyId",bizTaxCompanyTax.getBizMdCompanyId());
            put("gsdjxh",bizTaxCompanyTax.getGsdjxh());
            put("dsdjxh",bizTaxCompanyTax.getDsdjxh());
            put("invoiceSystem",bizTaxCompanyTax.getInvoiceSystem());
            put("billingType",bizTaxCompanyTax.getBillingType());
            put("processState",bizTaxCompanyTax.getProcessState());
            put("processCodeId",bizTaxCompanyTax.getProcessCodeId());
            put("processMessage",bizTaxCompanyTax.getProcessMessage());
            put("syncState",bizTaxCompanyTax.getSyncState());
            put("syncBy",bizTaxCompanyTax.getSyncBy());
            put("syncAt",bizTaxCompanyTax.getSyncAt());
            put("seq",bizTaxCompanyTax.getSeq());
            put("personDeclareType",bizTaxCompanyTax.getPersonDeclareType());
            put("taxNo",bizMdCompany.getTaxNo());
        }};
    }
}
