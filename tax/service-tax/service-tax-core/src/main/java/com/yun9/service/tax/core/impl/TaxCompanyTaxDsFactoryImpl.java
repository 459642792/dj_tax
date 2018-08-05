package com.yun9.service.tax.core.impl;

import com.yun9.biz.md.BizMdInstOrgTreeClientService;
import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.dto.CompanyDataDTO;
import com.yun9.biz.md.domain.entity.BizMdCompanyAccount;
import com.yun9.biz.tax.BizTaxCompanyCategoryService;
import com.yun9.biz.tax.BizTaxCompanyTaxDsService;
import com.yun9.biz.tax.BizTaxMdOfficeCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyCategory;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyTaxDs;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategory;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.TaxCompanyTaxDsFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by zhengzb on 2018/6/11.
 */
@Component
public class TaxCompanyTaxDsFactoryImpl implements TaxCompanyTaxDsFactory {

    @Autowired
    BizMdInstOrgTreeClientService bizMdInstOrgTreeClientService;

    @Autowired
    BizTaxCompanyTaxDsService bizTaxCompanyTaxDsService;

    @Autowired
    BizTaxCompanyCategoryService bizTaxCompanyCategoryService;

    @Autowired
    BizTaxMdOfficeCategoryService bizTaxMdOfficeCategoryService;
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

        //到tax中补充查询地税信息
        List<BizTaxCompanyTaxDs> bizTaxCompanyTaxDs = bizTaxCompanyTaxDsService.findByCompanyIds(companyIds);

        Map<String, CompanyAccountDTO> companyAccountMap = pageCommon.getCompanyAccountDTOS(companyIds);

        Map<Long,BizTaxCompanyTaxDs> bizTaxCompanyTaxDsMap = new HashMap(){{
            bizTaxCompanyTaxDs.forEach(v->{
                put(v.getBizTaxCompanyTax().getBizMdCompanyId(),v);
            });
        }};

        Map<Long,List<String>> companyCategorys = new HashMap<>();

        //到tax中补充相关的税种信息
        List<BizTaxCompanyCategory> bizTaxCompanyCategories = bizTaxCompanyCategoryService.findByCompanyIdsAndTaxOffice(companyIds, TaxOffice.ds);

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
                    put("syncState",bizTaxCompanyTaxDsMap.get(v.getCompanyId()) == null ? null : bizTaxCompanyTaxDsMap.get(v.getCompanyId()).getSyncState());
                    put("processState",bizTaxCompanyTaxDsMap.get(v.getCompanyId()) == null ? null : bizTaxCompanyTaxDsMap.get(v.getCompanyId()).getBizTaxCompanyTax().getProcessState());
                    put("processMessage",bizTaxCompanyTaxDsMap.get(v.getCompanyId()) == null ? null : bizTaxCompanyTaxDsMap.get(v.getCompanyId()).getBizTaxCompanyTax().getProcessMessage());
                    put("taxSns", companyCategorys.get(v.getInstClientId()));
                    put("passwordType", new HashMap() {{
                        put(TaxOffice.ds, Optional.ofNullable(companyAccountMap.get(v.getCompanyId() + "_" + TaxOffice.ds + "_" + BizMdCompanyAccount.State.activate)).orElse(null));
                    }});
                }});
            });
        }});
    }

}
