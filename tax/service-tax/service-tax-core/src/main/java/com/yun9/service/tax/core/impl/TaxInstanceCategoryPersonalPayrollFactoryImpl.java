package com.yun9.service.tax.core.impl;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.md.BizMdInstClientService;
import com.yun9.biz.md.BizMdInstOrgTreeClientService;
import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.entity.BizMdCompanyAccount;
import com.yun9.biz.md.domain.entity.BizMdInstClient;
import com.yun9.biz.tax.BizTaxInstanceCategoryHistoryService;
import com.yun9.biz.tax.BizTaxInstanceCategoryPersonalPayrollItemService;
import com.yun9.biz.tax.BizTaxInstanceCategoryPersonalPayrollService;
import com.yun9.biz.tax.domain.entity.*;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.TaxInstanceCategoryPersonalPayrollFactory;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Created by zhengzb on 2018/5/7.
 */
@Component
public class TaxInstanceCategoryPersonalPayrollFactoryImpl implements TaxInstanceCategoryPersonalPayrollFactory {

    public static final Logger logger = LoggerFactory.getLogger(TaxInstanceCategoryPersonalPayrollFactoryImpl.class);

    @Autowired
    BizMdInstOrgTreeClientService bizMdInstOrgTreeClientService;

    @Autowired
    BizTaxInstanceCategoryPersonalPayrollService bizTaxInstanceCategoryPersonalPayrollService;

    @Autowired
    PageCommon pageCommon;

    @Autowired
    BizTaxInstanceCategoryHistoryService bizTaxInstanceCategoryHistoryService;

    @Autowired
    BizMdInstClientService bizMdInstClientService;

    @Autowired
    BizTaxInstanceCategoryPersonalPayrollItemService bizTaxInstanceCategoryPersonalPayrollItemService;

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
        Pagination<BizTaxInstanceCategoryPersonalPayroll> pageObj = bizTaxInstanceCategoryPersonalPayrollService.pageByState(accountCycleIds, instClientIds, state, page, limit, params);
        pagination.setTotalElements(pageObj.getTotalElements());
        pagination.setTotalPages(pageObj.getTotalPages());

        //组织参数
        List<Long> companyIds = pageObj.getContent().stream().map(v -> v.getBizTaxInstanceCategory().getBizTaxInstance().getMdCompanyId()).collect(Collectors.toList());
        List<Long> instanceCategoryIds = pageObj.getContent().stream().map(v -> v.getBizTaxInstanceCategory().getId()).collect(Collectors.toList());
        List<Long> ids = pageObj.getContent().stream().map(v -> v.getId()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(companyIds) || CollectionUtils.isEmpty(instanceCategoryIds)) {
            return pagination;
        }

        CompletableFuture<Map<String, CompanyAccountDTO>> companyAccountMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getCompanyAccountDTOS(companyIds));
        CompletableFuture<Map<Long, List<BizTaxInstanceCategoryDeduct>>> deductsMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getDeductsMap(instanceCategoryIds));
        CompletableFuture<Map<String, List<BizTaxCompanyBank>>> banksMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getBanksMap(companyIds));
        CompletableFuture<Void> allOf = CompletableFuture.allOf(companyAccountMapFuture, deductsMapFuture, banksMapFuture).whenComplete((v, th) -> {
            if (th != null) {
                throw new ServiceTaxException(th.getMessage());
            }
        });
        allOf.join();


        Map<String, CompanyAccountDTO> companyAccountMap = companyAccountMapFuture.join();
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
        //7 获取收入所属期
        HashMap<Long, List<BizTaxInstanceCategoryPersonalPayrollItem>> incomesMap = pageCommon.getIncomeCycle(ids);

        //7 获取reportCheckStates
        List<BizTaxInstanceCategoryPersonalPayrollItem>items = bizTaxInstanceCategoryPersonalPayrollItemService.findByCategoryPersonalPayrollIdIn(ids);
        Map<Long, List<BizTaxInstanceCategoryPersonalPayrollItem>> itemsMap = new HashMap<Long, List<BizTaxInstanceCategoryPersonalPayrollItem>>() {{
            if (items != null) {
                items.forEach(e -> {
                    List<BizTaxInstanceCategoryPersonalPayrollItem> businessItems = get(e.getBizTaxInstanceCategoryPersonalPayrollId());
                    if (businessItems == null) {
                        businessItems = new ArrayList();
                    }
                    businessItems.add(e);
                    put(e.getBizTaxInstanceCategoryPersonalPayrollId(), businessItems);
                });
            }
        }};

        //6 组织数据
        pagination.setContent(new ArrayList() {{
            pageObj.getContent().forEach((v) -> {
                add(new HashMap() {{
                    //todo 公共数据
                    putAll(pageCommon.getCommonData(v.getBizTaxInstanceCategory()));


                    //todo 组织数据
                    putAll(pageCommon.getMapCommonData(companyAccountMap,bizMdInstClientsMap,null,deductsMap,banksMap,v.getBizTaxInstanceCategory()));
                    if (itemsMap.get(v.getId()) != null) {
                        put("reportCheckState", Optional.ofNullable(itemsMap.get(v.getId()) ).map((value) -> 1).orElse(0));//申报表
                    }else{
                        put("reportCheckState",0);//申报表
                    }

                    //todo 工资薪金个人所得税 （未完整）
                    put("sourceType", v.getSourceType());//申报类型[first首次申报][last税局上月][sso社保局][hand手动]
                    put("id", v.getId()); //工资薪金个人所得税ID
                    put("popleNum", v.getPopleNum());//人数
                    put("taxAmount", v.getTaxAmount());//纳税金额
                    put("incomeAccountCycleId", v.getIncomeAccountCycleId());//纳税金额
                    put("incomeAccountCycles", JSON.toJSON(incomesMap.get(v.getId())));//收入所属期

                }});
            });
        }});
        return pagination;

    }

    @Override
    public void changeIncomeAccountCycle(long id, long accountCycleId, long processBy) {
        //1 根据ID 获取个税工资薪金对象
        //2 检查状态
        //2.1 检查状态是否是申报{state = send} 操作失败,税种状态不是"发送(Send)"
        //2.2 检查申报类型是否是"按上月" {sourceType == last}，不是提示"操作失败,工资薪金申报类型不是"按上月""
        //3 进行修改
        //4 记录日志

        BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = Optional.ofNullable(bizTaxInstanceCategoryPersonalPayrollService.findById(id))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.IllegalArgumentError, "该个税工资薪金不存在"));

        if (bizTaxInstanceCategoryPersonalPayroll.getBizTaxInstanceCategory().getState() != BizTaxInstanceCategory.State.send) {
            throw BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "操作失败,税种状态不是\"发送(Send)\"");
        }

        if (bizTaxInstanceCategoryPersonalPayroll.getSourceType() != BizTaxInstanceCategoryPersonalPayroll.SourceType.last) {
            throw BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "操作失败,工资薪金申报类型不是\"按上月\"");
        }

        bizTaxInstanceCategoryPersonalPayroll.setIncomeAccountCycleId(accountCycleId);
        bizTaxInstanceCategoryPersonalPayrollService.create(bizTaxInstanceCategoryPersonalPayroll);

        bizTaxInstanceCategoryHistoryService.log(bizTaxInstanceCategoryPersonalPayroll.getBizTaxInstanceCategoryId(), processBy, BizTaxInstanceCategoryHistory.Type.send, "修改工资薪金收入所属期");

    }
}
