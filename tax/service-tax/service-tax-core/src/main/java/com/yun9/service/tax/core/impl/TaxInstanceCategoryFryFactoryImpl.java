package com.yun9.service.tax.core.impl;

import com.yun9.biz.md.BizMdAccountCycleService;
import com.yun9.biz.md.BizMdCompanyAccountService;
import com.yun9.biz.md.BizMdInstClientService;
import com.yun9.biz.md.BizMdInstOrgTreeClientService;
import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.entity.BizMdInstClient;
import com.yun9.biz.report.BizReportService;
import com.yun9.biz.tax.BizTaxInstanceCategoryFrService;
import com.yun9.biz.tax.BizTaxInstanceCategoryHistoryService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.BizTaxMdCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyBank;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryDeduct;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryFr;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.TaxInstanceCategoryFryFactory;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class TaxInstanceCategoryFryFactoryImpl implements TaxInstanceCategoryFryFactory {
    public static final Logger logger = LoggerFactory.getLogger(TaxInstanceCategoryFryFactoryImpl.class);


    @Autowired
    private BizTaxInstanceCategoryFrService bizTaxInstanceCategoryFrService;

    @Autowired
    private BizReportService bizReportService;
    @Autowired
    private BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    BizMdCompanyAccountService bizMdCompanyAccountService;


    @Autowired
    private BizTaxInstanceCategoryHistoryService bizTaxInstanceCategoryHistoryService;

    @Autowired
    private BizTaxMdCategoryService bizTaxMdCategoryService;

    @Autowired
    private BizMdInstOrgTreeClientService bizMdInstOrgTreeClientService;

    @Autowired
    private BizMdInstClientService bizMdInstClientService;

    @Autowired
    BizMdAccountCycleService bizMdAccountCycleService;


    @Autowired
    PageCommon pageCommon;

//    @Override
//    public HashMap totalByState(long accountCycleId, long orgTreeId, Map<String, Object> params) {
//        HashMap<String, Object> rst = new HashMap() {{
//            put("totalStart", 0);
//            put("totalCheck", 0);
//            put("totalSend", 0);
//            put("totalComplete", 0);
//            put("totalStart", 0);
//            put("totalError", 0);
//        }};
//        List<String> cycleTypes = new ArrayList() {{
//            add(CycleType.y);
//        }};
//        if (params != null) {
//            cycleTypes = Arrays.asList((String[]) params.get("cycleTypes"));
//        }
//        if (CollectionUtils.isEmpty(cycleTypes)) {
//            throw BizTaxException.build(BizTaxException.Codes.IllegalArgumentError, "申报周期不能为空");
//        }
//        List<Long> instClientIds = bizMdInstOrgTreeClientService.findInstClientIdsByOrgTreeId(orgTreeId);
//        if (CollectionUtils.isEmpty(instClientIds)) {
//            return rst;
//        }
//
//
//        BizMdAccountCycle bizMdAccountCycle = bizMdAccountCycleService.findById(accountCycleId);
//
//
//        HashMap<String, Object> map = bizTaxInstanceCategoryFrService.totalByAllState(accountCycleId, instClientIds, params);
//        //发起申报总数
//        rst.put("totalStart", bizMdInstClientService.findUnreportedCount(orgTreeId,
//                new StringJoiner("_")
//                        .add(bizMdAccountCycle.getSn())
//                        .add(bizMdAccountCycle.getType().name())
//                        .add(TaxSn.y_fr.name()).toString(),
//                com.yun9.biz.md.enums.TaxOffice.gs,
//                cycleTypes,
//                params));
//
//        if (map != null) {
//            //确认财报
//            rst.put("totalCheck", map.get("check") == null ? 0 : map.get("check"));
//            //申报财报
//            rst.put("totalSend", map.get("send") == null ? 0 : map.get("send"));
//            //已完成
//            rst.put("totalComplete", map.get("complete") == null ? 0 : map.get("complete"));
//        }
//        //发起错误
//        rst.put("totalError", bizTaxInstanceCategoryFrService.totalByState(accountCycleId, instClientIds, BizTaxInstanceCategory.State.start, params));
//        return rst;
//    }


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
        Pagination<BizTaxInstanceCategoryFr> pageObj = bizTaxInstanceCategoryFrService.pageByState(accountCycleIds, instClientIds, state, page, limit, params);
        pagination.setTotalElements(pageObj.getTotalElements());
        pagination.setTotalPages(pageObj.getTotalPages());


        //组织参数
        List<Long> companyIds = pageObj.getContent().stream().map(v -> v.getBizTaxInstanceCategory().getBizTaxInstance().getMdCompanyId()).collect(Collectors.toList());
        List<Long> instanceCategoryIds = pageObj.getContent().stream().map(v -> v.getBizTaxInstanceCategory().getId()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(companyIds) || CollectionUtils.isEmpty(instanceCategoryIds)) {
            return pagination;
        }

        CompletableFuture<Map<String, CompanyAccountDTO>> companyAccountMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getCompanyAccountDTOS(companyIds));
        CompletableFuture<Map<Long, HashMap>> reportStatesMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getReportStatesMap(instanceCategoryIds));
        CompletableFuture<Map<Long, List<BizTaxInstanceCategoryDeduct>>> deductsMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getDeductsMap(instanceCategoryIds));
        CompletableFuture<Map<String, List<BizTaxCompanyBank>>> banksMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getBanksMap(companyIds));
        CompletableFuture<Void> allOf = CompletableFuture.allOf(companyAccountMapFuture, reportStatesMapFuture, deductsMapFuture, banksMapFuture).whenComplete((v, th) -> {
            if (th != null) {
                throw new ServiceTaxException(th.getMessage());
            }
        });
        allOf.join();


        Map<String, CompanyAccountDTO> companyAccountMap = companyAccountMapFuture.join();
        Map<Long, HashMap> reportStatesMap = reportStatesMapFuture.join();
        Map<Long, List<BizTaxInstanceCategoryDeduct>> deductsMap = deductsMapFuture.join();
        Map<String, List<BizTaxCompanyBank>> banksMap = banksMapFuture.join();


        //6 机构客户状态
        Map<Long, BizMdInstClient> bizMdInstClientsMap = new HashMap<Long, BizMdInstClient>() {{
            bizMdInstClients.forEach(e -> {
                if (e != null) {
                    put(e.getId(), e);
                }
            });
        }};

        //4 组装数据
        pagination.setContent(new ArrayList() {{
            pageObj.getContent().forEach((v) -> {
                add(new HashMap() {{

                    //todo 公共数据
                    putAll(pageCommon.getCommonData(v.getBizTaxInstanceCategory()));

                    //todo 组织数据
                    putAll(pageCommon.getMapCommonData(companyAccountMap,bizMdInstClientsMap,reportStatesMap,deductsMap,banksMap,v.getBizTaxInstanceCategory()));

                    //todo 财务报表
                    put("reportType", v.getFrType());//财报类型
                    put("taxRecord", v.getTaxOfficeFrType()); //会计备案制度
                    put("id", v.getId());
                    put("allInOne", v.getAllInOne());//同意,不处理,待处理，无通知

                    if (reportStatesMap.get(v.getBizTaxInstanceCategoryId()) != null) {
                        if (v.getFrType() == BizTaxInstanceCategoryFr.FrType.unknown) {
                            put("reportCheckState", 0);
                        } else {
                            put("reportCheckState",reportStatesMap.get(v.getBizTaxInstanceCategoryId()).get("reportCheckState"));//申报表
                        }
                        put("reportAuditState", reportStatesMap.get(v.getBizTaxInstanceCategoryId()).get("reportAuditState"));//报表状态
                    }else{
                        put("reportCheckState",0);//申报表
                        put("reportAuditState",false);//报表状态
                    }
                }});


            });


        }});


        return pagination;

    }


}
