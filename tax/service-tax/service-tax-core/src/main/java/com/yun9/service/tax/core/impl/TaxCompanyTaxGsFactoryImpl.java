package com.yun9.service.tax.core.impl;

import com.yun9.biz.md.BizMdCompanyService;
import com.yun9.biz.md.BizMdInstOrgTreeClientService;
import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.dto.CompanyDataDTO;
import com.yun9.biz.md.domain.entity.BizMdCompany;
import com.yun9.biz.md.domain.entity.BizMdCompanyAccount;
import com.yun9.biz.tax.BizTaxCompanyCategoryService;
import com.yun9.biz.tax.BizTaxCompanyTaxGsService;
import com.yun9.biz.tax.BizTaxCompanyTaxService;
import com.yun9.biz.tax.BizTaxMdOfficeCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyCategory;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyTax;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyTaxGs;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategory;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.TaxCompanyTaxGsFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by zhengzb on 2018/6/11.
 */
@Component
public class TaxCompanyTaxGsFactoryImpl implements TaxCompanyTaxGsFactory {

    @Autowired
    BizMdInstOrgTreeClientService bizMdInstOrgTreeClientService;

    @Autowired
    BizTaxCompanyCategoryService bizTaxCompanyCategoryService;
    @Autowired
    BizTaxMdOfficeCategoryService bizTaxMdOfficeCategoryService;

    @Autowired
    BizTaxCompanyTaxGsService bizTaxCompanyTaxGsService;

    @Autowired
    BizMdCompanyService bizMdCompanyService;

    @Autowired
    BizTaxCompanyTaxService bizTaxCompanyTaxService;

    @Autowired
    PageCommon pageCommon;


    @Override
    public Pagination list(long orgTreeId, int page, int limit, HashMap<String, Object> params) {
        Pagination pagination = new Pagination();

        Pagination<CompanyDataDTO> companyDataDTOs = bizMdInstOrgTreeClientService.pageCompanyDTOSByOrgTreeIdAndParams(orgTreeId,page,limit,params);

        if(StringUtils.isEmpty(companyDataDTOs.getContent())){
            return pagination;
        }

        pagination.setTotalPages(companyDataDTOs.getTotalPages());
        pagination.setTotalElements(companyDataDTOs.getTotalElements());

        //符合条件的机构客户id
        List<Long> instClientIds = companyDataDTOs.getContent().stream().map(v -> v.getInstClientId()).collect(Collectors.toList());

        //符合条件的公司id
        List<Long> companyIds = companyDataDTOs.getContent().stream().map(v->v.getCompanyId()).collect(Collectors.toList());

        //到tax中补充查询国税信息
        List<BizTaxCompanyTaxGs> bizTaxCompanyTaxGs = bizTaxCompanyTaxGsService.findByCompanyIds(companyIds);

        Map<String, CompanyAccountDTO> companyAccountMap = pageCommon.getCompanyAccountDTOS(companyIds);

        Map<Long,BizTaxCompanyTaxGs> bizTaxCompanyTaxGsMap = new HashMap(){{
            bizTaxCompanyTaxGs.forEach(v->{
                put(v.getBizTaxCompanyTax().getBizMdCompanyId(),v);
            });
        }};

        Map<Long,List<String>> companyCategorys = new HashMap<>();

        //到tax中补充相关的税种信息
        List<BizTaxCompanyCategory> bizTaxCompanyCategories = bizTaxCompanyCategoryService.findByCompanyIdsAndTaxOffice(companyIds,TaxOffice.gs);

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
        return pagination.setContent(new ArrayList(){{
            companyDataDTOs.getContent().forEach(v->{
                add(new HashMap(){{
                    put("instClientId",v.getInstClientId());
                    put("companyId",v.getCompanyId());
                    put("clientSn",v.getClientSn());
                    put("companyName",v.getCompanyName());
                    put("taxType",v.getTaxType());
                    put("taxAreaId",v.getTaxAreaId());
                    put("state",v.getState());
                    put("billingType",bizTaxCompanyTaxGsMap.get(v.getCompanyId()) == null ? null : bizTaxCompanyTaxGsMap.get(v.getCompanyId()).getBizTaxCompanyTax().getBillingType());
                    put("invoiceSystem",bizTaxCompanyTaxGsMap.get(v.getCompanyId()) == null ? null : bizTaxCompanyTaxGsMap.get(v.getCompanyId()).getBizTaxCompanyTax().getBillingType());
                    put("syncState",bizTaxCompanyTaxGsMap.get(v.getCompanyId()) == null ? null : bizTaxCompanyTaxGsMap.get(v.getCompanyId()).getSyncState());
                    put("processState",bizTaxCompanyTaxGsMap.get(v.getCompanyId()) == null ? null : bizTaxCompanyTaxGsMap.get(v.getCompanyId()).getBizTaxCompanyTax().getProcessState());
                    put("processMessage",bizTaxCompanyTaxGsMap.get(v.getCompanyId()) == null ? null : bizTaxCompanyTaxGsMap.get(v.getCompanyId()).getBizTaxCompanyTax().getProcessMessage());
                    put("taxSns", companyCategorys.get(v.getInstClientId()));
                    put("passwordType", new HashMap() {{
                        put(TaxOffice.gs, Optional.ofNullable(companyAccountMap.get(v.getCompanyId() + "_" + TaxOffice.gs + "_" + BizMdCompanyAccount.State.activate)).orElse(null));
                    }});
                }});
            });
        }});
    }

    @Override
    public HashMap find(long companyId) {
        BizMdCompany bizMdCompany = Optional.ofNullable(bizMdCompanyService.findById(companyId)).orElseThrow(()-> BizTaxException.throwException(BizTaxException.Codes.BizTaxException,"公司不存在"));
        BizTaxCompanyTax bizTaxCompanyTax = bizTaxCompanyTaxService.findByCompanyId(companyId);
        if(null == bizTaxCompanyTax){
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException,"公司国税信息不存在");
        }
        return new HashMap(){{
            put("companyId",bizMdCompany.getId());
            put("billingType",bizTaxCompanyTax.getBillingType());
            put("invoiceSystem",bizTaxCompanyTax.getInvoiceSystem());
            put("taxNo",bizMdCompany.getTaxNo());
        }};
    }
}
