package com.yun9.service.tax.core.impl;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.md.BizMdCompanyAccountService;
import com.yun9.biz.md.BizMdInstClientService;
import com.yun9.biz.md.BizMdInstOrgTreeClientService;
import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.entity.BizMdCompanyAccount;
import com.yun9.biz.md.domain.entity.BizMdInstClient;
import com.yun9.biz.tax.BizTaxInstanceCategoryDeductService;
import com.yun9.biz.tax.BizTaxInstanceCategoryVatNormalService;
import com.yun9.biz.tax.domain.entity.*;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.TaxInstanceCategoryVatNormalFactory;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @program: service-tax
 * @description: ${description}
 * @author: leigm
 * @create: 2018-05-19 16:48
 **/
@Component
public class TaxInstanceCategoryVatNormalFactoryImpl implements TaxInstanceCategoryVatNormalFactory {
    public static final Logger logger = LoggerFactory.getLogger(TaxInstanceCategoryVatNormalFactoryImpl.class);


    @Autowired
    BizTaxInstanceCategoryVatNormalService bizTaxInstanceCategoryVatNormalService;

    @Autowired
    BizMdInstOrgTreeClientService bizMdInstOrgTreeClientService;

    @Autowired
    BizMdCompanyAccountService bizMdCompanyAccountService;

    @Autowired
    BizTaxInstanceCategoryDeductService bizTaxInstanceCategoryDeductService;

    @Autowired
    PageCommon pageCommon;

    @Autowired
    BizMdInstClientService bizMdInstClientService;


    @Override
    public Pagination<HashMap> pageByState(List<Long> accountCycleIds, long orgTreeId, BizTaxInstanceCategory.State state, int page, int limit, Map<String, Object> params) {

        Pagination pagination = new Pagination();
        pagination.setContent(new ArrayList());

        //1 检查组织id
        List<BizMdInstClient> bizMdInstClients = pageCommon.getBizMdInstClients(orgTreeId, params);
        if (CollectionUtils.isEmpty(bizMdInstClients)) {
            return pagination;
        }

        List<Long> instClientIds = bizMdInstClients.stream().map(v -> v.getId()).collect(Collectors.toList());
        Pagination<BizTaxInstanceCategoryVatNormal> pageObj = bizTaxInstanceCategoryVatNormalService.pageByState(accountCycleIds, instClientIds, state, page, limit, params);
        pagination.setTotalElements(pageObj.getTotalElements());
        pagination.setTotalPages(pageObj.getTotalPages());


        //组织参数
        List<Long> companyIds = pageObj.getContent().stream().map(v -> v.getBizTaxInstanceCategory().getBizTaxInstance().getMdCompanyId()).collect(Collectors.toList());
        List<Long> instanceCategoryIds = pageObj.getContent().stream().map(v -> v.getBizTaxInstanceCategory().getId()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(companyIds) || CollectionUtils.isEmpty(instanceCategoryIds)) {
            return pagination;
        }

        CompletableFuture<Map<String, CompanyAccountDTO>> companyAccountMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getCompanyAccountDTOS(companyIds));
        CompletableFuture<Map<Long,HashMap>> reportStatesMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getReportStatesMap(instanceCategoryIds));
        CompletableFuture<Map<Long, List<BizTaxInstanceCategoryDeduct>>> deductsMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getDeductsMap(instanceCategoryIds));
        CompletableFuture<Void> allOf = CompletableFuture.allOf(companyAccountMapFuture, reportStatesMapFuture, deductsMapFuture).whenComplete((v, th) -> {
            if (th != null) {
                throw new ServiceTaxException(th.getMessage());
            }
        });
        allOf.join();

        Map<String, CompanyAccountDTO> companyAccountMap = companyAccountMapFuture.join();
        Map<Long, HashMap> reportStatesMap = reportStatesMapFuture.join();
        Map<Long, List<BizTaxInstanceCategoryDeduct>> deductsMap = deductsMapFuture.join();


        //6 机构客户状态
        Map<Long, BizMdInstClient> bizMdInstClientsMap = new HashMap<Long, BizMdInstClient>() {{
            bizMdInstClients.forEach(e -> {
                if (e != null) {
                    put(e.getId(), e);
                }
            });
        }};

        //6 组织数据
        pagination.setContent(new ArrayList() {{
            pageObj.getContent().forEach((v) -> {
                add(new HashMap<String, Object>() {{
                    //todo 公共数据
                    putAll(pageCommon.getCommonData(v.getBizTaxInstanceCategory()));

                    //todo 组织数据
                    putAll(pageCommon.getMapCommonData(companyAccountMap,bizMdInstClientsMap,reportStatesMap,deductsMap,null,v.getBizTaxInstanceCategory()));

                    //todo 增值税 一般纳税人
                    put("id", v.getId());//增值税ID
                    put("selfAmount", v.getSelfAmount());//自开金额
                    put("nobillAmount", v.getNobillAmount());//无票金额
                    put("ticketCheckState", v.getTicketCheckState());//票表核对状态
                    put("ticketCheckMessage", v.getTicketCheckMessage());//票表核对消息
                    put("ticketCheckDate", v.getTicketCheckDate());//票表核对时间

                }});
            });
        }});


        return pagination;

    }

}

