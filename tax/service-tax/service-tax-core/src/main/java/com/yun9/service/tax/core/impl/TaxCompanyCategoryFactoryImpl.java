package com.yun9.service.tax.core.impl;

import com.yun9.biz.md.BizMdInstClientService;
import com.yun9.biz.md.BizMdInstOrgTreeClientService;
import com.yun9.biz.md.domain.dto.InstClientHelper;
import com.yun9.biz.md.domain.dto.UserDetailDTO;
import com.yun9.biz.tax.BizTaxCompanyCategoryService;
import com.yun9.biz.tax.BizTaxMdOfficeCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyCategory;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategory;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.service.tax.core.TaxCompanyCategoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by zhengzb on 2018/6/13.
 */
@Component
public class TaxCompanyCategoryFactoryImpl implements TaxCompanyCategoryFactory {


    @Autowired
    BizMdInstClientService bizMdInstClientService;
    @Autowired
    BizTaxMdOfficeCategoryService bizTaxMdOfficeCategoryService;
    @Autowired
    BizTaxCompanyCategoryService bizTaxCompanyCategoryService;

    @Autowired
    BizMdInstOrgTreeClientService bizMdInstOrgTreeClientService;

    @Override
    public void create(long companyId, long taxOfficeCategoryId) {
        Optional.ofNullable(bizMdInstClientService.findById(companyId)).orElseThrow(()-> BizTaxException.throwException(BizTaxException.Codes.BizTaxException,"机构客户不存在"));
        Optional.ofNullable(bizTaxMdOfficeCategoryService.findById(taxOfficeCategoryId)).orElseThrow(()-> BizTaxException.throwException(BizTaxException.Codes.BizTaxException,"税种实例不存在"));
        bizTaxCompanyCategoryService.create(companyId,taxOfficeCategoryId);
    }

    @Override
    public Object list(long companyId, TaxOffice taxOffice) {

        List<Long> companyIds = new ArrayList(){{
            add(companyId);
        }};

        //先查询税局支持的所有税种
        List<BizTaxMdOfficeCategory> bizTaxMdOfficeCategories = bizTaxMdOfficeCategoryService.findByTaxOffice(taxOffice);
        Map<Long, BizTaxMdCategory> result = new HashMap<>();
        Optional.ofNullable(bizTaxMdOfficeCategories).orElse(new ArrayList<>()).forEach(v -> result.put(v.getId(), v.getBizTaxMdCategory()));
        List<Long> taxOfficeCateoryIds =  Optional.ofNullable(bizTaxMdOfficeCategories).orElse(new ArrayList<>()).stream().map(v -> v.getId()).collect(Collectors.toList());

        //查询公司税种绑定信息
        List<BizTaxCompanyCategory> bizTaxCompanyCategories = bizTaxCompanyCategoryService.findByCompanyIdsAndTaxOfficeCategoryIds(companyIds,taxOfficeCateoryIds);
        if(CollectionUtils.isEmpty(bizTaxCompanyCategories)){
            return new ArrayList<>();
        }

        return new ArrayList(){{
            bizTaxCompanyCategories.forEach(v->{
                add(new HashMap(){{
                    put("id",v.getId());
                    put("source",v.getSource());
                    put("categoryName",Optional.ofNullable(result.get(v.getBizTaxMdOfficeCategoryId())).map(v->v.getName()).orElse(null));
                }});
            });
        }};
    }

    @Override
    public Object countContainTaxSns(long orgTreeId, HashMap<String, Object> params) {
        HashMap<String,Object> count = new HashMap(){{
            put("all",0);
            put("normal",0);
            put("containTaxSn",0);
            put("disabled",0);
        }};
        List<TaxSn> taxSns = new ArrayList<>();
        if (params != null) {
            if (params.get("taxSns") != null) {
                String[] obj = (String[]) params.get("taxSns");
                for (String key : obj) {
                    taxSns.add(TaxSn.valueOf(key));
                }
                params.put("taxSns", taxSns);
            }
        }
        Optional.ofNullable(taxSns).orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.IllegalArgumentError, "税种不能为空"));
        HashMap<String,Object> instClientMap = bizMdInstOrgTreeClientService.findInstIdAndClientIdByParams(orgTreeId, params);
        List<InstClientHelper> instClientHelpers = (List<InstClientHelper>) instClientMap.get("all");
        List<InstClientHelper> normalInstClientHelpers = (List<InstClientHelper>) instClientMap.get("normal");
        List<InstClientHelper> disabledInstClientHelpers = (List<InstClientHelper>) instClientMap.get("disabled");

        List<Long> normalCompanyIds = Optional.ofNullable(normalInstClientHelpers).orElse(new ArrayList<>()).stream().map(v->v.getCompanyId()).collect(Collectors.toList());

        List<Long> containTaxSnsCompanyIds = new ArrayList<>();

        if(CollectionUtils.isNotEmpty(instClientHelpers)){
            count.put("all",instClientHelpers.size());
        }
        if(CollectionUtils.isNotEmpty(disabledInstClientHelpers)){
            count.put("disabled",disabledInstClientHelpers.size());
        }
        if(CollectionUtils.isNotEmpty(normalInstClientHelpers)){
            containTaxSnsCompanyIds = bizTaxCompanyCategoryService.findContainTaxSnsCompanyIds(taxSns,normalCompanyIds,params);
            count.put("normal",normalInstClientHelpers.size());
            count.put("containTaxSn",containTaxSnsCompanyIds.size());
        }

        normalCompanyIds.removeAll(containTaxSnsCompanyIds);

        Map<Long,InstClientHelper> instClientHelperMaps = normalInstClientHelpers.stream().collect(Collectors.toMap(InstClientHelper::getCompanyId, InstClientHelper->InstClientHelper));

        count.put("notContainTaxSnCompanyName",new ArrayList(){{
            normalCompanyIds.forEach(v->{
                add(instClientHelperMaps.get(v).getCompanyName());
            });
        }});

        return count;
    }

}
