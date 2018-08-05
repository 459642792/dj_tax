package com.yun9.service.tax.core.impl;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.bill.domain.enums.Category;
import com.yun9.biz.md.*;
import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.entity.BizMdAccountCycle;
import com.yun9.biz.md.domain.entity.BizMdCompany;
import com.yun9.biz.md.domain.entity.BizMdCompanyAccount;
import com.yun9.biz.md.domain.entity.BizMdInstClient;
import com.yun9.biz.md.enums.CycleType;
import com.yun9.biz.tax.*;
import com.yun9.biz.tax.domain.entity.*;
import com.yun9.biz.tax.enums.DeclareType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.exception.BizException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.TaxInstanceCategoryPersonalBusinessFactory;
import com.yun9.service.tax.core.dto.*;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.ft.TaxMdCategoryHandler;
import com.yun9.service.tax.core.utils.ExportExcelUtil;
import com.yun9.service.tax.core.utils.FileParse;
import com.yun9.service.tax.core.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Created by zhengzb on 2018/5/7.
 */
@Component
public class TaxInstanceCategoryPersonalBusinessFactoryImpl implements TaxInstanceCategoryPersonalBusinessFactory {

    public static final Logger logger = LoggerFactory.getLogger(TaxInstanceCategoryPersonalBusinessFactoryImpl.class);


    @Autowired
    BizMdInstOrgTreeClientService bizMdInstOrgTreeClientService;

    @Autowired
    BizMdCompanyAccountService bizMdCompanyAccountService;


    @Autowired
    BizTaxInstanceCategoryPersonalBusinessService bizTaxInstanceCategoryPersonalBusinessService;

    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    PageCommon pageCommon;

    @Autowired
    BizTaxInstanceCategoryPersonalBusinessItemService bizTaxInstanceCategoryPersonalBusinessItemService;
    @Autowired
    BizTaxInstanceCategoryVatSmallService bizTaxInstanceCategoryVatSmallService;
    @Autowired
    BizMdAccountCycleService bizMdAccountCycleService;
    @Autowired
    BizMdCompanyService bizMdCompanyService;
    @Autowired
    BizMdInstClientService bizMdInstClientService;
    @Autowired
    BizTaxInstanceService bizTaxInstanceService;
    @Autowired
    TaxMdCategoryHandler taxMdCategoryHandler;

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
        Pagination<BizTaxInstanceCategoryPersonalBusiness> pageObj = bizTaxInstanceCategoryPersonalBusinessService.pageByState(accountCycleIds, instClientIds, state, page, limit, params);
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

        List<BizTaxInstanceCategoryPersonalBusinessItem> items = bizTaxInstanceCategoryPersonalBusinessItemService.findByBizTaxInstanceCategoryPersonalBusinessIdIn(ids);
        Map<Long, List<BizTaxInstanceCategoryPersonalBusinessItem>> itemsMap = new HashMap<Long, List<BizTaxInstanceCategoryPersonalBusinessItem>>() {{
            if (items != null) {
                items.forEach(e -> {
                    List<BizTaxInstanceCategoryPersonalBusinessItem> businessItems = get(e.getBizTaxInstanceCategoryPersonalBusinessId());
                    if (businessItems == null) {
                        businessItems = new ArrayList();
                    }
                    businessItems.add(e);
                    put(e.getBizTaxInstanceCategoryPersonalBusinessId(), businessItems);
                });
            }
        }};

        pagination.setContent(new ArrayList() {
            {
                pageObj.getContent().forEach((v) -> {
                    add(new HashMap() {
                        {
                            //todo 公共数据
                            putAll(pageCommon.getCommonData(v.getBizTaxInstanceCategory()));


                            //todo 组织数据
                            putAll(pageCommon.getMapCommonData(companyAccountMap, bizMdInstClientsMap, reportStatesMap, deductsMap, banksMap, v.getBizTaxInstanceCategory()));

                            //todo 生产经营 （未完整）
                            put("id", v.getId());
                            put("incomeAmount", v.getIncomeAmount());//收入
                            put("buyAmount", v.getBuyAmount());//成本
                            put("profitAmount", v.getProfitAmount());//利润

                            List<BizTaxInstanceCategoryPersonalBusinessItem> businessItems = itemsMap.get(v.getId());
                            put("items", new ArrayList() {{
                                if (businessItems != null) {
                                    businessItems.forEach(v -> {
                                        add(new HashMap() {{
                                            put("shareholderName", v.getShareholderName());//股东名字
                                            put("shareholderRate", v.getShareholderRate());//分配比例
                                            put("declareCheckState", v.getDeclareType() == DeclareType.none ? 0 : 1);//是否申报
                                        }});
                                    });
                                }

                            }});
                        }
                    });
                });
            }
        });
        return pagination;

    }


    @Override
    public void downloadExcel(HttpServletRequest request, HttpServletResponse response, List<Long> categoryBussinessIds) {

        logger.debug("下载文件开始");
        List<BizTaxInstanceCategoryPersonalBusiness> listBusiness = Optional.ofNullable(bizTaxInstanceCategoryPersonalBusinessService.findByIdIn(categoryBussinessIds))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误，没有公司对象"));


        List<BizTaxInstanceCategoryPersonalBusiness> list = listBusiness.stream()
                .filter(k -> BizTaxInstanceCategory.State.send.equals(k.getBizTaxInstanceCategory().getState()) && !BizTaxInstanceCategory.ProcessState.process.equals(k.getBizTaxInstanceCategory().getState()))
                .distinct().collect(Collectors.toList());

        List<Map<String, Object>> datas = new ArrayList<>();
        if (CollectionUtils.isEmpty(list)) {
            throw BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误，没有公司对象");
        }
        if (CollectionUtils.isNotEmpty(list)) {
            list.forEach(k -> {
                Map<String, Object> map = new HashMap<>();
                map.put(BizBusinessExcelDTO.FiledName.mdCompanyName.toString(), k.getBizTaxInstanceCategory().getBizTaxInstance().getMdCompanyName());
                map.put(BizBusinessExcelDTO.FiledName.taxDeadline.toString(), BizTaxBusinessSheetDTO.TaxDeadline.getName(null == k.getBizTaxInstanceCategory().getCycleType() ? "" : k.getBizTaxInstanceCategory().getCycleType().toString()).getMessage());
                final BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatSmall = bizTaxInstanceCategoryVatSmallService.findByBizTaxInstanceCategoryId(k.getBizTaxInstanceCategoryId());
                map.put(BizBusinessExcelDTO.FiledName.incomeAmount.toString(), 0);
                if (StringUtils.isNotEmpty(bizTaxInstanceCategoryVatSmall)) {
                    BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryVatSmall.getBizTaxInstanceCategory();
                    if (BizTaxInstanceCategory.State.complete.equals(bizTaxInstanceCategory.getState()) && BizTaxInstanceCategory.ProcessState.success.equals(bizTaxInstanceCategory.getProcessState())) {
                        map.put(BizBusinessExcelDTO.FiledName.incomeAmount.toString(), bizTaxInstanceCategoryVatSmall.getSelfAmount().add(bizTaxInstanceCategoryVatSmall.getAgentAmount()).add(bizTaxInstanceCategoryVatSmall.getNobillAmount()));
                    }
                }
                map.put(BizBusinessExcelDTO.FiledName.buyAmount.toString(), StringUtils.isEmpty(k.getBuyAmount()) ? 0 : k.getBuyAmount());
                map.put(BizBusinessExcelDTO.FiledName.deductionAmount.toString(), StringUtils.isEmpty(k.getDeductionAmount()) ? 0 : k.getDeductionAmount());
                datas.add(map);
            });
        }
        try {
            ExportFileDTO exportFileDTO = new ExportFileDTO(request, response, datas, new HashMap<Integer, Short>() {{
                put(0, (short) 1200);
            }}, BizBusinessExcelDTO.bulidExportFileDTOs(), BizBusinessExcelDTO.build(), 4, "个税生产经营", "个税生产经营-利润核算导入模板");
            ExportExcelUtil.downloadExcel(exportFileDTO);
        } catch (Exception e) {
            throw BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "下载错误");
        }
    }


    @Override
    public Object parseVatExcel(BizTaxVatImportSheetDTO bizTaxVatImportSheetDTO) {
        logger.debug("生成经营文件开始");

        if (null == bizTaxVatImportSheetDTO.getFileData() || "".equals(bizTaxVatImportSheetDTO.getFileData())) {
            throw BizTaxException.build(BizTaxException.Codes.Biz_Vat_Import_File_Error);
        }

        BizMdAccountCycle bizMdAccountCycle = Optional.ofNullable(bizMdAccountCycleService.findById(bizTaxVatImportSheetDTO.getMdAccountCycleId()))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.TAX_VAT_CYCLE_ERRORS, "申报月份不存在"));


        String str = FileUtil.importFile(bizTaxVatImportSheetDTO.getFileUploadPath(),
                bizTaxVatImportSheetDTO.getFileData(), bizTaxVatImportSheetDTO.getFileOriginalName());
        File file = new File(str);
        List<BizTaxBusinessSheetDTO> sheetData = BizTaxBusinessSheetDTO.buildSheet(file, BizBusinessExcelDTO.build(), 2, 4);
        logger.debug("解析excelc成功,公有多少行数据====",CollectionUtils.isNotEmpty(sheetData)?sheetData.size():0);
        if (null != sheetData && sheetData.size() != 0) {
            for (BizTaxBusinessSheetDTO bizTaxBusinessSheetDTO : sheetData) {
                this.build(bizTaxBusinessSheetDTO, bizTaxVatImportSheetDTO.getInstId(), bizMdAccountCycle);
            }
        } else {
            throw BizTaxException.build(BizTaxException.Codes.DateError, "没有解析到任何数据");
        }
        return sheetData;
    }

    private void build(BizTaxBusinessSheetDTO data, long instId, BizMdAccountCycle bizMdAccountCycle) {
        data.setErrorType(BizTaxBusinessSheetDTO.ERROR.error);
//        BizMdCompany bizMdCompany = bizMdCompanyService.findByFullName(data.getMdCompanyName());
//        if (null == bizMdCompany) {
//            data.setError("该公司信息不存在");
//            return;
//        }
//        BizMdInstClient bizMdInstClient = bizMdInstClientService.findByCompanyIdAndInstId(bizMdCompany.getId(), instId);
//        if (null == bizMdInstClient) {
//            data.setError("该公司机构客户信息不存在");
//            return;
//        }
        Map<String, Long> mapCompany =   bizMdCompanyService.findByCompanyIdAndClientId(data.getMdCompanyName(),instId);
        if (null == mapCompany || mapCompany.size() ==0) {
            data.setError("该公司信息不存在");
            return;
        }
//        BizTaxInstance bizTaxInstance = bizTaxInstanceService.currentTaxOfficeInstClientInstance(bizMdInstClient.getId(), bizMdCompany.getId(), bizMdAccountCycle.getId(), TaxOffice.ds, bizMdCompany.getTaxAreaId());
//        if (null == bizTaxInstance) {
//            data.setError("该公司税种不存在");
//            return;
//        }
//        BizTaxInstanceCategoryPersonalBusiness bizTaxInstanceCategoryPersonalBusiness = bizTaxInstanceCategoryPersonalBusinessService.findByTaxInstanceId(bizTaxInstance.getId());
//        if (null == bizTaxInstanceCategoryPersonalBusiness) {
//            data.setError("该公司生产经营所得不存在");
//            return;
//        }
        Map<String, Long> mapBusiness =   bizTaxInstanceCategoryPersonalBusinessService.findByPersonalBsiness(mapCompany.get("instClientId"),mapCompany.get("companyId"),bizMdAccountCycle.getId(),TaxOffice.ds.name(),mapCompany.get("taxAreaId"));
        if (null == mapBusiness || mapBusiness.size() ==0) {
            data.setError("该公司生产经营所得不存在");
            return;
        }

        String errorStr = "";

        BigDecimal incomeAmount = null;
        try {
            incomeAmount = new BigDecimal(data.getIncomeAmountStr()).setScale(2, BigDecimal.ROUND_HALF_UP);
            if (incomeAmount.compareTo(BigDecimal.ZERO) == -1) {
                errorStr += "第3列不能小于0";
            }
        } catch (Exception e) {
            errorStr += "第3列格式错误";
        }
        BigDecimal buyAmount = null;
        try {
            buyAmount = new BigDecimal(data.getBuyAmountStr()).setScale(2, BigDecimal.ROUND_HALF_UP);
            if (buyAmount.compareTo(BigDecimal.ZERO) == -1) {
                errorStr += "第4列不能小于0";
            }
        } catch (Exception e) {
            errorStr += "第4列格式错误";
        }
        BigDecimal deductionAmount = null;
        try {
            deductionAmount = new BigDecimal(data.getDeductionAmountStr()).setScale(2, BigDecimal.ROUND_HALF_UP);
            if (deductionAmount.compareTo(BigDecimal.ZERO) == -1) {
                errorStr += "第5列不能小于0";
            }
        } catch (Exception e) {
            errorStr += "第5列格式错误";
        }
        if (!"".equals(errorStr)) {
            data.setError(errorStr);
            return;
        }
        data.setIncomeAmount(incomeAmount);
        data.setBuyAmount(buyAmount);
        data.setDeductionAmount(deductionAmount);
        data.setMdAccountCycleId(bizMdAccountCycle.getId());
//        data.setInstanceCategoryBusinessId(bizTaxInstanceCategoryPersonalBusiness.getId());
//        data.setTaxInstanceId(bizTaxInstance.getId());
//        data.setMdCompanyId(bizMdCompany.getId());
//        data.setInstanceCategoryId(bizTaxInstanceCategoryPersonalBusiness.getBizTaxInstanceCategoryId());
        data.setInstanceCategoryBusinessId(mapBusiness.get("personalId"));
        data.setTaxInstanceId(mapBusiness.get("instanceId"));
        data.setMdCompanyId(mapCompany.get("companyId"));
        data.setInstanceCategoryId(mapBusiness.get("categoryId"));
        data.setErrorType(BizTaxBusinessSheetDTO.ERROR.success);
        Long audit = mapBusiness.get("audit");
        if ( audit == 1) {
            data.setErrorType(BizTaxBusinessSheetDTO.ERROR.warning);
            data.setError("该公司已审核.");
        }
    }

    @Override
    public void save(BizTaxBusinessSheetDTO bizTaxBusinessSheetDTO, long id, int state, long userId) {


        BizTaxInstanceCategoryPersonalBusiness bizTaxInstanceCategoryPersonalBusiness = Optional.ofNullable(bizTaxInstanceCategoryPersonalBusinessService.findById(bizTaxBusinessSheetDTO.getInstanceCategoryBusinessId()))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.DateError, "税种不存在"));


        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryPersonalBusiness.getBizTaxInstanceCategory();
        if (BizTaxInstanceCategory.State.send.equals(bizTaxInstanceCategory.getState()) && !BizTaxInstanceCategory.ProcessState.process.equals(bizTaxInstanceCategory.getProcessState())) {
            if (1 == state) {
                taxMdCategoryHandler.cancelOfAudit(bizTaxInstanceCategory.getId(), userId);
            }
            bizTaxInstanceCategoryPersonalBusiness.setBuyAmount(bizTaxBusinessSheetDTO.getBuyAmount());
            bizTaxInstanceCategoryPersonalBusiness.setIncomeAmount(bizTaxBusinessSheetDTO.getIncomeAmount());
            bizTaxInstanceCategoryPersonalBusiness.setDeductionAmount(bizTaxBusinessSheetDTO.getDeductionAmount());
            bizTaxInstanceCategoryPersonalBusinessService.update(bizTaxInstanceCategoryPersonalBusiness,userId);
            logger.debug("保存生成经营陈宫{}",bizTaxInstanceCategoryPersonalBusiness);
            taxMdCategoryHandler.audit(bizTaxInstanceCategory, userId, new TaxMdCategoryHandler.AuditCallback() {
                @Override
                public void success() {
                }
                @Override
                public void exception(BizException ex) {
                }
            });
        } else {
            throw BizTaxException.build(BizTaxException.Codes.DateError, "该税种状态不是发起申报状态");
        }
    }
}
