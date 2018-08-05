package com.yun9.service.tax.core.impl;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.bill.BizBillInvoiceCalculateService;
import com.yun9.biz.bill.BizBillInvoiceItemService;
import com.yun9.biz.bill.BizBillInvoiceService;
import com.yun9.biz.bill.domain.bo.BizBillInvoiceAgentInvoiceDto;
import com.yun9.biz.bill.domain.bo.CalculateAgentDto;
import com.yun9.biz.bill.domain.bo.CalculateNoBillDto;
import com.yun9.biz.bill.domain.bo.CalculateOutputDto;
import com.yun9.biz.bill.domain.bo.InvoiceCountDto;
import com.yun9.biz.bill.domain.entity.BizBillInvoice;
import com.yun9.biz.bill.domain.entity.BizBillInvoiceItem;
import com.yun9.biz.bill.domain.enums.Category;
import com.yun9.biz.bill.exception.BizBillException;
import com.yun9.biz.md.*;
import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.entity.BizMdAccountCycle;
import com.yun9.biz.md.domain.entity.BizMdCompany;
import com.yun9.biz.md.domain.entity.BizMdCompanyAccount;
import com.yun9.biz.md.domain.entity.BizMdInstClient;
import com.yun9.biz.md.enums.CycleType;
import com.yun9.biz.md.exception.BizMdException;
import com.yun9.biz.report.BizReportService;
import com.yun9.biz.tax.*;
import com.yun9.biz.tax.domain.entity.*;
import com.yun9.biz.tax.enums.BillingType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.exception.BizException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.DateUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.TaxInstanceCategoryVatQFactory;
import com.yun9.service.tax.core.dto.*;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.ft.TaxMdCategoryHandler;
import com.yun9.service.tax.core.report.ReportFactory;
import com.yun9.service.tax.core.report.sz.vat_small.report.Report001;
import com.yun9.service.tax.core.utils.ExportExcelUtil;
import com.yun9.service.tax.core.utils.FileParse;
import com.yun9.service.tax.core.utils.FileUtil;
import org.apache.commons.collections.map.HashedMap;
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
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @program: service-tax
 * @description: ${description}
 * @author: leigm
 * @create: 2018-05-19 16:48
 **/
@Component
public class TaxInstanceCategoryVatQFactoryImpl implements TaxInstanceCategoryVatQFactory {
    public static final Logger logger = LoggerFactory.getLogger(TaxInstanceCategoryVatQFactoryImpl.class);


    @Autowired
    BizTaxInstanceCategoryVatSmallService bizTaxInstanceCategoryVatSmallService;

    @Autowired
    BizMdInstOrgTreeClientService bizMdInstOrgTreeClientService;

    @Autowired
    BizMdCompanyAccountService bizMdCompanyAccountService;

    @Autowired
    BizTaxInstanceCategoryReportService bizTaxInstanceCategoryReportService;

    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    BizTaxInstanceCategoryAttachmentService bizTaxInstanceCategoryAttachmentService;

    @Autowired
    BizTaxMdOfficeCategoryCycleService bizTaxMdOfficeCategoryCycleService;

    @Autowired
    BizMdAccountCycleService bizMdAccountCycleService;
    @Autowired
    BizMdCompanyService bizMdCompanyService;
    @Autowired
    BizMdInstClientService bizMdInstClientService;

    @Autowired
    BizBillInvoiceService bizBillInvoiceService;
    @Autowired
    BizBillInvoiceItemService bizBillInvoiceItemService;
    @Autowired
    BizTaxInstanceService bizTaxInstanceService;

    @Autowired
    BizTaxCompanyCategoryService bizTaxCompanyCategoryService;

    @Autowired
    BizTaxInstanceCategoryDeductService bizTaxInstanceCategoryDeductService;

    @Autowired
    private BizTaxInstanceCategoryDataService bizTaxInstanceCategoryDataService;

    @Autowired
    private BizBillInvoiceCalculateService bizBillInvoiceCalculateService;

    @Autowired
    PageCommon pageCommon;

    @Autowired
    TaxMdCategoryHandler taxMdCategoryHandler;


    @Autowired
    private ReportFactory reportFactory;

    @Override
    public HashMap totalByDeclareType(List<Long> accountCycleIds, long orgTreeId, BizTaxInstanceCategory.State state, Map<String, Object> params) {

        //根据组织ID获取 机构实例ID
        List<Long> instClientIds = bizMdInstOrgTreeClientService.findInstClientIdsByOrgTreeId(orgTreeId);

        //判断机构实例ID集合，如果为NULL 则直接返回统计结果为0
        if (CollectionUtils.isEmpty(instClientIds)) {
            return new HashMap() {{
                put(BizTaxInstanceCategoryVatSmall.VatDeclareType.zero, 0);
                put(BizTaxInstanceCategoryVatSmall.VatDeclareType.taxable, 0);
                put(BizTaxInstanceCategoryVatSmall.VatDeclareType.taxfree, 0);
            }};
        }
        //获取统计结果
        return bizTaxInstanceCategoryVatSmallService.totalByDeclareType(accountCycleIds, instClientIds, state, params);
    }

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
        Pagination<BizTaxInstanceCategoryVatSmall> pageObj = bizTaxInstanceCategoryVatSmallService.pageByState(accountCycleIds, instClientIds, state, page, limit, params);
        pagination.setTotalElements(pageObj.getTotalElements());
        pagination.setTotalPages(pageObj.getTotalPages());


        //组织参数
        List<Long> companyIds = pageObj.getContent().stream().map(v -> v.getBizTaxInstanceCategory().getBizTaxInstance().getMdCompanyId()).collect(Collectors.toList());
        List<Long> instanceCategoryIds = pageObj.getContent().stream().map(v -> v.getBizTaxInstanceCategory().getId()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(companyIds) || CollectionUtils.isEmpty(instanceCategoryIds)) {
            return pagination;
        }

        List<Long> tempAccountCycleIds = new ArrayList();
        for (int i = 0; i < accountCycleIds.size(); i++) {
            List<BizMdAccountCycle> bizMdAccountCycles = bizMdAccountCycleService.findContainsMonth(accountCycleIds.get(i));
            if (CollectionUtils.isNotEmpty(bizMdAccountCycles)) {
                tempAccountCycleIds.addAll(bizMdAccountCycles.stream().map(v -> v.getId()).collect(Collectors.toList()));
            }
        }


        CompletableFuture<Map<String, InvoiceCountDto>> invoiceCountDtosMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getInvoiceCountDtosMap(companyIds, tempAccountCycleIds));
        CompletableFuture<Map<String, CompanyAccountDTO>> companyAccountMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getCompanyAccountDTOS(companyIds));
        CompletableFuture<Map<Long, HashMap>> reportStatesMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getReportStatesMap(instanceCategoryIds));
        CompletableFuture<Map<Long, List<BizTaxInstanceCategoryDeduct>>> deductsMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getDeductsMap(instanceCategoryIds));
        CompletableFuture<Map<String, List<BizTaxCompanyBank>>> banksMapFuture = CompletableFuture.supplyAsync(() -> pageCommon.getBanksMap(companyIds));
        CompletableFuture<Void> allOf = CompletableFuture.allOf(invoiceCountDtosMapFuture, companyAccountMapFuture, reportStatesMapFuture, deductsMapFuture, banksMapFuture).whenComplete((v, th) -> {
            if (th != null) {
                throw new ServiceTaxException(th.getMessage());
            }
        });
        allOf.join();

        Map<String, InvoiceCountDto> invoiceCountDtoHashMap = invoiceCountDtosMapFuture.join();
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

        //6 组织数据
        pagination.setContent(new ArrayList() {{
            pageObj.getContent().forEach((v) -> {
                add(new HashMap() {{
                    //todo 公共数据
                    putAll(pageCommon.getCommonData(v.getBizTaxInstanceCategory()));

                    //todo 组织数据
                    putAll(pageCommon.getMapCommonData(companyAccountMap, bizMdInstClientsMap, reportStatesMap, deductsMap, banksMap, v.getBizTaxInstanceCategory()));

                    //todo 增值税
                    put("id", v.getId());//增值税ID


                    put("agentAmount", BigDecimal.ZERO.doubleValue());
                    put("selfAmount", BigDecimal.ZERO.doubleValue());
                    put("nobillAmount", BigDecimal.ZERO.doubleValue());
                    put("prepayTaxAmount", BigDecimal.ZERO.doubleValue());


                    InvoiceCountDto invoiceCountDto = null;
                    BizTaxInstance bizTaxInstance = v.getBizTaxInstanceCategory().getBizTaxInstance();
                    put("prepayTaxSource", v.getPrepayTaxSource());//预缴来源[agent审核预缴来自代开][taxoffice审核预缴来自税局累计]',

                    if (v.getPrepayTaxSource() == BizTaxInstanceCategoryVatSmall.PrepayTaxSource.agent) {
                        invoiceCountDto = invoiceCountDtoHashMap.get(bizTaxInstance.getMdCompanyId() + "_" + BizTaxInstanceCategoryVatSmall.BillType.agent.toString());
                        if (invoiceCountDto != null) {
                            put("prepayTaxAmount", Optional.ofNullable(invoiceCountDto.getTotalDeclareprepaid()).orElse(BigDecimal.ZERO).doubleValue());
                        }
                    } else if (v.getPrepayTaxSource() == BizTaxInstanceCategoryVatSmall.PrepayTaxSource.taxoffice) {
                        put("prepayTaxAmount", v.getPrepayTaxServiceDeclareamount().add(v.getPrepayTaxCargoDeclareamount()));//预缴金额
                    }


                    invoiceCountDto = invoiceCountDtoHashMap.get(bizTaxInstance.getMdCompanyId() + "_" + BizTaxInstanceCategoryVatSmall.BillType.agent.toString());
                    if (invoiceCountDto != null) {
                        put("agentAmount", Optional.ofNullable(invoiceCountDto.getTotalDeclareamount()).orElse(BigDecimal.ZERO).doubleValue());//代开金额
                    }
                    invoiceCountDto = invoiceCountDtoHashMap.get(bizTaxInstance.getMdCompanyId() + "_" + BizTaxInstanceCategoryVatSmall.BillType.output.toString());
                    if (invoiceCountDto != null) {
                        put("selfAmount", Optional.ofNullable(invoiceCountDto.getTotalDeclareamount()).orElse(BigDecimal.ZERO).doubleValue());//自开金额
                    }
                    invoiceCountDto = invoiceCountDtoHashMap.get(bizTaxInstance.getMdCompanyId() + "_" + BizTaxInstanceCategoryVatSmall.BillType.nobill.toString());
                    if (invoiceCountDto != null) {
                        put("nobillAmount", Optional.ofNullable(invoiceCountDto.getTotalDeclareamount()).orElse(BigDecimal.ZERO).doubleValue());//无票金额
                    }

                    put("ticketCheckState", v.getTicketCheckState());//票表核对状态
                    put("ticketCheckMessage", v.getTicketCheckMessage());//票表核对消息
                    put("ticketCheckDate", v.getTicketCheckDate());//票表核对时间


                }});
            });


        }});


        return pagination;

    }

    @Override
    public ArrayList findById(long id) {
        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatQ = Optional.ofNullable(bizTaxInstanceCategoryVatSmallService.findById(id))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误，没有增值税对象"));

        return new ArrayList() {{
            add(new HashMap() {{
                put("category", BizTaxInstanceCategoryVatSmall.Category.service);
                put("totalAmount", Optional.ofNullable(bizTaxInstanceCategoryVatQ.getPrepayTaxServiceTotalamount()).orElse(BigDecimal.ZERO));
                put("declareAmount", Optional.ofNullable(bizTaxInstanceCategoryVatQ.getPrepayTaxServiceDeclareamount()).orElse(BigDecimal.ZERO));
            }});

            add(new HashMap() {{
                put("category", BizTaxInstanceCategoryVatSmall.Category.cargo);
                put("totalAmount", Optional.ofNullable(bizTaxInstanceCategoryVatQ.getPrepayTaxCargoTotalamount()).orElse(BigDecimal.ZERO));
                put("declareAmount", Optional.ofNullable(bizTaxInstanceCategoryVatQ.getPrepayTaxCargoDeclareamount()).orElse(BigDecimal.ZERO));
            }});

        }};
    }


    @Override
    public void downloadExcel(HttpServletRequest request, HttpServletResponse response, List<Long> categoryIds, long mdAccountCycleId) {
        logger.debug("下载文件开始");
        List<Map<String, Object>> datas = new ArrayList<>();
        List<BizTaxInstanceCategoryVatSmall> listCategoryVats = bizTaxInstanceCategoryVatSmallService.findByIdIn(categoryIds);
        if (CollectionUtils.isNotEmpty(listCategoryVats)) {
            List<BizTaxInstanceCategory> listCategorys = listCategoryVats.stream().map(BizTaxInstanceCategoryVatSmall::getBizTaxInstanceCategory).collect(Collectors.toList());
            List<BizTaxInstance> list = listCategorys.stream()
                    .filter(k -> BizTaxInstanceCategory.State.send.equals(k.getState()) && !BizTaxInstanceCategory.ProcessState.process.equals(k.getProcessState()))
                    .map(BizTaxInstanceCategory::getBizTaxInstance).distinct().collect(Collectors.toList());

            BizMdAccountCycle bizMdAccountCycle = Optional.ofNullable(bizMdAccountCycleService.findById(mdAccountCycleId))
                    .orElseThrow(() -> ServiceTaxException.build(ServiceTaxException.Codes.EXPORT_EXCEL_ERROR, "申报月份不存在"));

            if (CollectionUtils.isNotEmpty(list)) {
                list.forEach(k -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put(BizExcelDTO.FiledName.mdCompanyName.toString(), k.getMdCompanyName());
                    map.put(BizExcelDTO.FiledName.mdAccountCycle.toString(), bizMdAccountCycle.getSn());
                    map.put(BizExcelDTO.FiledName.billType.toString(), StringUtils.isNotEmpty(k.getBillingType()) ? Category.valueOf(k.getBillingType().toString()).getName() : "");
                    if (StringUtils.isNotEmpty(k.getBillingType())) {
                        if (k.getBillingType().toString().equals(Category.service.getValue())) {
                            map.put(BizExcelDTO.FiledName.cargo.toString(), "--");
                        }
                        if (k.getBillingType().toString().equals(Category.cargo.getValue())) {
                            map.put(BizExcelDTO.FiledName.service.toString(), "--");
                        }
                    }
                    datas.add(map);
                });
            }
        }

        try {
            ExportFileDTO exportFileDTO = new ExportFileDTO(request, response, datas, new HashMap<Integer, Short>() {{
                put(0, (short) 1200);
            }}, BizExcelDTO.bulidExportFileDTOs(), BizExcelDTO.build(), 13, "导入模板", "无票收入及自开导入模板");
            ExportExcelUtil.downloadExcel(exportFileDTO);
        } catch (Exception e) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.EXPORT_EXCEL_ERROR, "下载错误");
        }
    }


    @Override
    public void savePrepay(long id, BizTaxInstanceCategoryVatSmall.PrepayTaxSource prepayTaxSource, long userId) {
        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatQ = Optional.ofNullable(bizTaxInstanceCategoryVatSmallService.findById(id))
                .orElseThrow(() -> ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "税种不存在"));
        List<BizMdAccountCycle> list = bizMdAccountCycleService.findContainsMonth(bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getBizTaxInstance().getMdAccountCycleId());
        if (CollectionUtils.isEmpty(list)) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "会计区间id不存在");
        }
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory();
        if (BizTaxInstanceCategory.State.send.equals(bizTaxInstanceCategory.getState()) && !BizTaxInstanceCategory.ProcessState.process.equals(bizTaxInstanceCategory.getProcessState())) {
            if (bizTaxInstanceCategory.isAudit()) {
                throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "该公司已经发票已经审核,请先取消审核");
            }
            bizTaxInstanceCategoryVatSmallService.savePrepay(id, prepayTaxSource, userId);

        } else {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "该税种状态不是发起申报状态");
        }
    }

    @Override
    public void savePrepay(long id, BigDecimal cargoAmount, BigDecimal serviceAmount, long userId) {
        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatQ = Optional.ofNullable(bizTaxInstanceCategoryVatSmallService.findById(id))
                .orElseThrow(() -> ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "税种不存在"));
        List<BizMdAccountCycle> list = bizMdAccountCycleService.findContainsMonth(bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getBizTaxInstance().getMdAccountCycleId());
        if (CollectionUtils.isEmpty(list)) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "会计区间id不存在");
        }
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory();
        if (BizTaxInstanceCategory.State.send.equals(bizTaxInstanceCategory.getState()) && !BizTaxInstanceCategory.ProcessState.process.equals(bizTaxInstanceCategory.getProcessState())) {
            if (bizTaxInstanceCategory.isAudit()) {
                throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "该公司已经发票已经审核,请先取消审核");
            }
            bizTaxInstanceCategoryVatSmallService.savePrepay(id, cargoAmount, serviceAmount, userId);
        } else {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "该税种状态不是发起申报状态");
        }
    }

    @Override
    public void saveBill(List<BillDTO> billDTOs, long instanceCategoryVatQId, int state, long userId) {
        if (CollectionUtils.isEmpty(billDTOs)) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "保存发票不能为空");
        }
        logger.debug("保存发票明细{}", billDTOs);
        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatQ = Optional.ofNullable(bizTaxInstanceCategoryVatSmallService.findById(instanceCategoryVatQId))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.DateError, "税种不存在"));

        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory();
        Map<Long, BizMdAccountCycle> map = new HashMap<>();
        if (BizTaxInstanceCategory.State.send.equals(bizTaxInstanceCategory.getState()) && !BizTaxInstanceCategory.ProcessState.process.equals(bizTaxInstanceCategory.getProcessState())) {
            if (1 == state) {
                taxMdCategoryHandler.cancelOfAudit(bizTaxInstanceCategory.getId(), userId);
            }
            List<BizBillInvoiceAgentInvoiceDto> billInvoiceAgentInvoiceDtos = new ArrayList<>();
            for (BillDTO bill : billDTOs) {
                BizBillInvoiceAgentInvoiceDto billInvoiceAgentInvoiceDto = new BizBillInvoiceAgentInvoiceDto();
                if (StringUtils.isNotEmpty(bill.getType())) {
                    billInvoiceAgentInvoiceDto.setType(BizBillInvoice.Type.valueOf(bill.getType()));
                }
                billInvoiceAgentInvoiceDto.setMdCompanyId(bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId());
                BizMdAccountCycle bizMdAccountCycle = map.get(bill.getCycleId());
                if (StringUtils.isEmpty(bizMdAccountCycle)) {
                    bizMdAccountCycle = bizMdAccountCycleService.findById(bill.getCycleId());
                    if (null == bizMdAccountCycle) {
                        throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, bill.getCycle() + "会计区间不存在");
                    } else {
                        map.put(bill.getCycleId(), bizMdAccountCycle);
                    }
                }
                billInvoiceAgentInvoiceDto.setBillingDate(bizMdAccountCycle.getEndDate());
                billInvoiceAgentInvoiceDto.setMdAccountCycleId(bill.getCycleId());
                billInvoiceAgentInvoiceDto.setTaxRate(bill.getTaxRate());
                billInvoiceAgentInvoiceDto.setDeclareAmount(bill.getDeclareAmount());
                billInvoiceAgentInvoiceDto.setAmount(bill.getDeclareAmount());
                billInvoiceAgentInvoiceDto.setBillType(BizBillInvoice.BillType.valueOf(bill.getBillType()));
                billInvoiceAgentInvoiceDto.setCategory(Category.valueOf(bill.getCategory()));
                billInvoiceAgentInvoiceDto.setSource(BizBillInvoice.Source.excel);
                billInvoiceAgentInvoiceDtos.add(billInvoiceAgentInvoiceDto);
            }
            if (CollectionUtils.isNotEmpty(billInvoiceAgentInvoiceDtos)) {

                bizBillInvoiceService.saveList(billInvoiceAgentInvoiceDtos);
                if (state == 1) {
                    List<BizMdAccountCycle> list = bizMdAccountCycleService.findContainsMonth(bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getBizTaxInstance().getMdAccountCycleId());
                    if (CollectionUtils.isEmpty(list)) {
                        throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "会计区间id不存在");
                    }
//                    List<Long> accountCycleIds = list.stream().map(BizMdAccountCycle::getId).collect(Collectors.toList());
//
//                    updateAmount(instanceCategoryVatQId, bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory(), accountCycleIds, userId);

                    taxMdCategoryHandler.audit(bizTaxInstanceCategory, userId, new TaxMdCategoryHandler.AuditCallback() {
                        @Override
                        public void success() {
                        }

                        @Override
                        public void exception(BizException ex) {
                        }
                    });
                }
            }
        } else {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "该税种状态不是发起申报状态");
        }
    }

    @Override
    public void audit(long id, long userId) {
        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatQ = Optional.ofNullable(bizTaxInstanceCategoryVatSmallService.findById(id))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.DateError, "税种不存在"));
        List<BizMdAccountCycle> list = bizMdAccountCycleService.findContainsMonth(bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getBizTaxInstance().getMdAccountCycleId());
        if (CollectionUtils.isEmpty(list)) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "会计区间id不存在");
        }
//        List<Long> accountCycleIds = list.stream().map(BizMdAccountCycle::getId).collect(Collectors.toList());

//        updateAmount(id, bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory(), accountCycleIds, userId);

        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory();
        if (bizTaxInstanceCategory.isAudit()) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "该公司已经发票已经审核,无需审核");
        }
        taxMdCategoryHandler.audit(bizTaxInstanceCategory, userId, new TaxMdCategoryHandler.AuditCallback() {
            @Override
            public void success() {
            }

            @Override
            public void exception(BizException ex) {
                throw ex;
            }
        });
    }


    @Override
    public void cancelAudit(long id, long userId) {
        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatQ = Optional.ofNullable(bizTaxInstanceCategoryVatSmallService.findById(id))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.DateError, "税种不存在"));

        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory();
        if (!bizTaxInstanceCategory.isAudit()) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "该公司已经取消审核,无需取消审核");
        }

        taxMdCategoryHandler.cancelOfAudit(bizTaxInstanceCategory.getId(), userId);
    }

    @Override
    public Object parseVatExcel(BizTaxVatImportSheetDTO bizTaxVatImportSheetDTO) {
        logger.debug("解析增值税文件开始");

        if (null == bizTaxVatImportSheetDTO.getFileData() || "".equals(bizTaxVatImportSheetDTO.getFileData())) {
            throw BizTaxException.build(BizTaxException.Codes.Biz_Vat_Import_File_Error);
        }
        BizMdAccountCycle bizMdAccountCycle = Optional.ofNullable(bizMdAccountCycleService.findById(bizTaxVatImportSheetDTO.getMdAccountCycleId()))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.TAX_VAT_CYCLE_ERRORS, "申报月份不存在"));


        String str = FileUtil.importFile(bizTaxVatImportSheetDTO.getFileUploadPath(),
                bizTaxVatImportSheetDTO.getFileData(), bizTaxVatImportSheetDTO.getFileOriginalName());
        File file = new File(str);
        Map<Integer, List<BizTaxBillInvoiceSheetDTO>> sheetData = BizTaxBillInvoiceSheetDTO.buildSheet(file, BizExcelDTO.build(), 3, 13);
        Map<Integer, List<BizTaxBillInvoiceSheetDTO>> sheets = new HashMap<>();
        if (null != sheetData && sheetData.size() != 0) {
            for (Integer row : sheetData.keySet()) {
                this.build(sheets, row, sheetData.get(row), bizTaxVatImportSheetDTO.getInstId(), bizMdAccountCycle);
            }
        } else {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "没有解析到任何数据");
        }
        return sheets;
    }


    @Override
    public void createBill(long id, BizBillInvoice.BillType billType, BizBillInvoiceAgentInvoiceDto bizBillInvoiceAgentInvoiceDto, long userId) {
        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatQ = Optional.ofNullable(bizTaxInstanceCategoryVatSmallService.findById(id))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.DateError, "税种不存在"));

        this.isAudit(bizTaxInstanceCategoryVatQ, bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getBizTaxInstance().getMdCompanyId());
        Optional.ofNullable(bizBillInvoiceAgentInvoiceDto).orElseThrow(() -> BizBillException.throwException(BizBillException.Codes.BizBillException, "发票信息不能为空"));

        if (Category.service.getValue().equals(bizBillInvoiceAgentInvoiceDto.getCategory().toString())
                && Category.cargo.getValue().equals(bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getBizTaxInstance().getBillingType())) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "公司类型为劳务类,不能新增服务类型发票.");
        }
        if (Category.cargo.getValue().equals(bizBillInvoiceAgentInvoiceDto.getCategory().toString())
                && Category.service.getValue().equals(bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getBizTaxInstance().getBillingType())) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "公司类型为服务类,不能新增劳务类型发票.");
        }

        //验证公司是否存在
        Optional.ofNullable(bizMdCompanyService.findById(bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getBizTaxInstance().getMdCompanyId())).orElseThrow(() -> BizMdException.throwException(BizMdException.Codes.BizMdCompanyNotFound, bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getBizTaxInstance().getMdCompanyId()));
        //会计区间id不存在则根据发票日期生成会计区间
        if (bizBillInvoiceAgentInvoiceDto.getMdAccountCycleId() == null) {
            String sn = DateUtils.longToDateString(bizBillInvoiceAgentInvoiceDto.getBillingDate() * 1000L, "yyyyMM");
            BizMdAccountCycle bizMdAccountCycle = bizMdAccountCycleService.findBySnAndType(sn, CycleType.m);
            Optional.ofNullable(bizMdAccountCycle).orElseThrow(() -> BizBillException.throwException(BizBillException.Codes.BizBillException, "开票日期对应的会计区间不存在"));
            bizBillInvoiceAgentInvoiceDto.setMdAccountCycleId(bizMdAccountCycle.getId());
        }
        bizBillInvoiceAgentInvoiceDto.setSource(BizBillInvoice.Source.edit);
        List<Long> accountCycleIds = new ArrayList() {{
            add(bizBillInvoiceAgentInvoiceDto.getMdAccountCycleId());
        }};
        List<BizBillInvoiceAgentInvoiceDto> invoices = new ArrayList() {{
            add(bizBillInvoiceAgentInvoiceDto);
        }};
        bizBillInvoiceService.batchCreate(bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getBizTaxInstance().getMdCompanyId(), BizBillInvoice.Source.edit,
                billType,
                false,
                accountCycleIds,
                invoices);
        List<BizMdAccountCycle> list = bizMdAccountCycleService.findContainsMonth(bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getBizTaxInstance().getMdAccountCycleId());
        if (CollectionUtils.isEmpty(list)) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "会计区间id不存在");
        }
        List<Long> ids = list.stream().map(BizMdAccountCycle::getId).collect(Collectors.toList());

        updateAmount(id, bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory(), ids, userId);
    }

    @Override
    public void resetBill(List<BillDTO> billDTOs, BizMdAccountCycle bizMdAccountCycle, long instanceCategoryVatQId, long userId, boolean audit) {
        if (CollectionUtils.isEmpty(billDTOs)) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "保存发票不能为空");
        }
        logger.debug("保存发票明细{}", billDTOs);
        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatQ = Optional.ofNullable(bizTaxInstanceCategoryVatSmallService.findById(instanceCategoryVatQId))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.DateError, "税种不存在"));
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory();
        if (bizTaxInstanceCategory.isAudit()) {
            ServiceTaxException.throwException(ServiceTaxException.Codes.BILL_AUDIT_ERROR, "增值税已审核，不允许修改");
        }
        if (!bizTaxInstanceCategory.isSend()) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "该税种状态不是发起申报状态");
        }
        List<BizBillInvoiceAgentInvoiceDto> billInvoiceAgentInvoiceDtos = new ArrayList<>();
        for (BillDTO bill : billDTOs) {
            BizBillInvoiceAgentInvoiceDto billInvoiceAgentInvoiceDto = new BizBillInvoiceAgentInvoiceDto();
            if (StringUtils.isNotEmpty(bill.getType())) {
                billInvoiceAgentInvoiceDto.setType(BizBillInvoice.Type.valueOf(bill.getType()));
            }
            billInvoiceAgentInvoiceDto.setMdCompanyId(bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId());
            billInvoiceAgentInvoiceDto.setBillingDate(bizMdAccountCycle.getEndDate());
            billInvoiceAgentInvoiceDto.setMdAccountCycleId(bill.getCycleId());
            billInvoiceAgentInvoiceDto.setTaxRate(bill.getTaxRate());
            billInvoiceAgentInvoiceDto.setDeclareAmount(bill.getDeclareAmount());
            billInvoiceAgentInvoiceDto.setAmount(bill.getDeclareAmount());
            billInvoiceAgentInvoiceDto.setFullamount(bill.getDeclareAmount().multiply(bill.getTaxRate().add(new BigDecimal(1))));
            billInvoiceAgentInvoiceDto.setTaxAmount(billInvoiceAgentInvoiceDto.getDeclareAmount().multiply(billInvoiceAgentInvoiceDto.getTaxRate()));
            billInvoiceAgentInvoiceDto.setBillType(BizBillInvoice.BillType.valueOf(bill.getBillType()));
            billInvoiceAgentInvoiceDto.setCategory(Category.valueOf(bill.getCategory()));
            billInvoiceAgentInvoiceDto.setSource(BizBillInvoice.Source.api);
            billInvoiceAgentInvoiceDtos.add(billInvoiceAgentInvoiceDto);
        }
        if (CollectionUtils.isEmpty(billInvoiceAgentInvoiceDtos)) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "无法创建发票数据");
        }
        bizBillInvoiceService.resetBill(billInvoiceAgentInvoiceDtos);
        if (audit) {
            List<BizMdAccountCycle> list = bizMdAccountCycleService.findContainsMonth(bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getBizTaxInstance().getMdAccountCycleId());
            if (CollectionUtils.isEmpty(list)) {
                throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "会计区间id不存在");
            }
            taxMdCategoryHandler.audit(bizTaxInstanceCategory, userId, new TaxMdCategoryHandler.AuditCallback() {
                @Override
                public void success() {
                    logger.debug("机构导入增值税数据，审核成功");
                }

                @Override
                public void exception(BizException ex) {
                    logger.error("机构导入增值税数据，审核失败", ex);
                }
            });
        }
    }

    @Override
    public void updateBill(long id, long billId, BizBillInvoice bizBillInvoice, long userId) {
        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatQ = Optional.ofNullable(bizTaxInstanceCategoryVatSmallService.findById(id))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.DateError, "税种不存在"));

        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory();
        if (BizTaxInstanceCategory.State.send.equals(bizTaxInstanceCategory.getState()) && !BizTaxInstanceCategory.ProcessState.process.equals(bizTaxInstanceCategory.getProcessState())) {
            if (bizTaxInstanceCategory.isAudit()) {
                throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "该公司已经发票已经审核,请先取消审核");
            }
        } else {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "该税种状态不是发起申报状态");
        }
        bizBillInvoiceService.update(billId, bizBillInvoice);
        List<BizMdAccountCycle> list = bizMdAccountCycleService.findContainsMonth(bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getBizTaxInstance().getMdAccountCycleId());
        if (CollectionUtils.isEmpty(list)) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "会计区间id不存在");
        }
        List<Long> accountCycleIds = list.stream().map(BizMdAccountCycle::getId).collect(Collectors.toList());
        updateAmount(id, bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory(), accountCycleIds, userId);
    }

    @Override
    public void deleteById(long id, long billId, long userId) {
        BizBillInvoice bizBillInvoice = bizBillInvoiceService.findById(billId);
        if (null == bizBillInvoice) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "发票不存在");
        }

        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatQ = Optional.ofNullable(bizTaxInstanceCategoryVatSmallService.findById(id))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.DateError, "税种不存在"));

        this.isAudit(bizTaxInstanceCategoryVatQ, bizBillInvoice.getMdCompanyId());
        bizBillInvoiceService.deleteById(billId);

        List<BizMdAccountCycle> list = bizMdAccountCycleService.findContainsMonth(bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getBizTaxInstance().getMdAccountCycleId());
        if (CollectionUtils.isEmpty(list)) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "会计区间id不存在");
        }
        List<Long> accountCycleIds = list.stream().map(BizMdAccountCycle::getId).collect(Collectors.toList());

        updateAmount(id, bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory(), accountCycleIds, userId);
    }

    @Override
    public Map queryPayTaxAmountById(long id, String type, long userId) {

        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatQ = Optional.ofNullable(bizTaxInstanceCategoryVatSmallService.findById(id))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.DateError, "税种不存在"));
        //历史申报数据
        BizTaxInstanceCategoryData bizTaxInstanceCategoryDataHistory = bizTaxInstanceCategoryDataService.findByTypeAndTaxCategoryId(BizTaxInstanceCategoryData.DataType.history, bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getId());
        BizTaxInstanceCategoryData bizTaxInstanceCategoryDataCurrent = bizTaxInstanceCategoryDataService.findByTypeAndTaxCategoryId(BizTaxInstanceCategoryData.DataType.current, bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getId());
        if ((null == bizTaxInstanceCategoryDataHistory || "[]".equals(bizTaxInstanceCategoryDataHistory.getJson())) || (null == bizTaxInstanceCategoryDataCurrent || "[]".equals(bizTaxInstanceCategoryDataCurrent.getJson()))) {
            BizTaxException.throwException(BizTaxException.Codes.DateError, "税种异常,获取客户申报历史数据失败!");
        }
        final Map<String, String> history = bizTaxInstanceCategoryDataHistory.toMap();
        //当前数据
        final Map<String, String> current = bizTaxInstanceCategoryDataCurrent.toMap();

        Report001.CalculationValue calculationValue = new Report001.CalculationValue();

        if (new BigDecimal(current.get("YSHWYNZSL")).compareTo(new BigDecimal("0.00")) != 0 && (current.get("SZLBDM").equals("01") || current.get("SZLBDM").equals("03"))) {
            calculationValue.labourstate = true;
        }
        if (new BigDecimal(current.get("YSFWYNZSL")).compareTo(new BigDecimal("0.00")) != 0 && (current.get("SZLBDM").equals("01") || current.get("SZLBDM").equals("03"))) {
            calculationValue.servicestate = true;
        }
        if (current.get("GTHBZ").equals("1")) {
            calculationValue.personalstate = true;
        }
        //获取开票金额
        if (StringUtils.isNotEmpty(current.get("DKFPJE"))) {
            calculationValue.agentTotalAmount = new BigDecimal(current.get("DKFPJE"));
        }
        final BizTaxInstance bizTaxInstance = bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getBizTaxInstance();
        List<Long> _accountCycleIds = bizMdAccountCycleService.findContainsMonth(bizTaxInstance.getMdAccountCycleId()).stream().map(BizMdAccountCycle::getId).collect(Collectors.toList());
        final long[] accountCycleIds = bizMdAccountCycleService.findContainsMonth(bizTaxInstance.getMdAccountCycleId()).stream().map(BizMdAccountCycle::getId).mapToLong(Long::longValue).toArray();
        //计算代开金额
        CalculateAgentDto calculateAgentDto = bizBillInvoiceCalculateService.calculateAgent(bizTaxInstance.getMdCompanyId(), accountCycleIds);
        if (null != calculateAgentDto) {
            calculationValue.sstAgentAmount = calculateAgentDto.getSstAgentAmount();
            calculationValue.sptAgentAmount = calculateAgentDto.getSptAgentAmount();
            calculationValue.ssfAgentAmount = calculateAgentDto.getSsfAgentAmount();
            calculationValue.spfAgentAmount = calculateAgentDto.getSpfAgentAmount();
            calculationValue.lstAgentAmount = calculateAgentDto.getLstAgentAmount();
            calculationValue.lptAgentAmount = calculateAgentDto.getLptAgentAmount();
            calculationValue.agentAmount = calculateAgentDto.getAgentAmount();
        }

        //   logger.debug("公司{},会计期间{}代开发票信息{}", bizTaxInstance.getMdCompanyId(), accountCycleIds, JSON.toJSONString(calculateAgentDto));
        //计算不开票
        CalculateNoBillDto calculateNoBillDto = bizBillInvoiceCalculateService.calculateNoBill(bizTaxInstance.getMdCompanyId(), accountCycleIds);
        if (null != calculateNoBillDto) {
            calculationValue.lnNobillAmount = calculateNoBillDto.getLnNobillAmount();
            calculationValue.sntNobillAmount = calculateNoBillDto.getSntNobillAmount();
            calculationValue.snfNobillAmount = calculateNoBillDto.getSnfNobillAmount();
        }
        //  logger.debug("公司{},会计期间{}无票信息{}", bizTaxInstance.getMdCompanyId(), accountCycleIds, JSON.toJSONString(calculateNoBillDto));
        //计算自开发票
        CalculateOutputDto calculateOutputDto = bizBillInvoiceCalculateService.calculateOutput(bizTaxInstance.getMdCompanyId(), accountCycleIds);
        if (null != calculateOutputDto) {
            calculationValue.lnOutputAmount = calculateOutputDto.getLnOutputAmount();
            calculationValue.lstAgentAmount = calculateOutputDto.getLstOutputAmount().add(calculateAgentDto.getLstAgentAmount());
            calculationValue.ltOutputAmount = calculateOutputDto.getLtOutputAmount();
            calculationValue.snOutputAmount = calculateOutputDto.getSnOutputAmount();
            calculationValue.stOutputAmount = calculateOutputDto.getStOutputAmount();
            calculationValue.sfOutputAmount = calculateOutputDto.getSfOutputAmount();
            calculationValue.sstAgentAmount = calculateOutputDto.getSstOutputAmount().add(calculateAgentDto.getSstAgentAmount());
            calculationValue.ssfAgentAmount = calculateOutputDto.getSsfOutputAmount().add(calculateAgentDto.getSsfAgentAmount());
        }
        final Map<String, Object> result = new HashedMap();
        if (StringUtils.isNotEmpty(type) && type.equals("taxOffice")) {
            calculationValue.ltPrepaidAmount = bizTaxInstanceCategoryVatQ.getPrepayTaxCargoDeclareamount();
            calculationValue.stPrepaidAmount = bizTaxInstanceCategoryVatQ.getPrepayTaxServiceDeclareamount();
            calculationValue.ltPrepaidAmounttotal = bizTaxInstanceCategoryVatQ.getPrepayTaxCargoTotalamount();
            calculationValue.stPrepaidAmounttotal = bizTaxInstanceCategoryVatQ.getPrepayTaxServiceTotalamount();
            Map<String, Object> agentReport001 = new Report001().generate(calculationValue, history).toMap();
            result.put("taxOffice", new BigDecimal(agentReport001.get("a22").toString()).add(new BigDecimal(agentReport001.get("b22").toString())));
        } else if (StringUtils.isNotEmpty(type) && type.equals("agent")) {
            HashMap<String, BigDecimal> amounts = bizBillInvoiceService.countAmountByCompanyId(bizTaxInstance.getMdCompanyId(), _accountCycleIds);
            calculationValue.ltPrepaidAmount = amounts.get("cargo");// bizTaxInstanceCategoryVatSmall.getPrepayTaxCargoDeclareamount();
            calculationValue.stPrepaidAmount = amounts.get("service");//bizTaxInstanceCategoryVatSmall.getPrepayTaxServiceDeclareamount();
            calculationValue.ltPrepaidAmounttotal = bizTaxInstanceCategoryVatQ.getPrepayTaxCargoTotalamount();
            calculationValue.stPrepaidAmounttotal = bizTaxInstanceCategoryVatQ.getPrepayTaxServiceTotalamount();
            Map<String, Object> taxOfficeReport001 = new Report001().generate(calculationValue, history).toMap();
            result.put("agent", new BigDecimal(taxOfficeReport001.get("a22").toString()).add(new BigDecimal(taxOfficeReport001.get("b22").toString())));
        } else if (StringUtils.isNotEmpty(type) && type.equals("all")) {
            calculationValue.ltPrepaidAmount = bizTaxInstanceCategoryVatQ.getPrepayTaxCargoDeclareamount();
            calculationValue.stPrepaidAmount = bizTaxInstanceCategoryVatQ.getPrepayTaxServiceDeclareamount();
            calculationValue.ltPrepaidAmounttotal = bizTaxInstanceCategoryVatQ.getPrepayTaxCargoTotalamount();
            calculationValue.stPrepaidAmounttotal = bizTaxInstanceCategoryVatQ.getPrepayTaxServiceTotalamount();
            Map<String, Object> agentReport001 = new Report001().generate(calculationValue, history).toMap();
            result.put("taxOffice", new BigDecimal(agentReport001.get("a22").toString()).add(new BigDecimal(agentReport001.get("b22").toString())));
            HashMap<String, BigDecimal> amounts = bizBillInvoiceService.countAmountByCompanyId(bizTaxInstance.getMdCompanyId(), _accountCycleIds);
            calculationValue.ltPrepaidAmount = amounts.get("cargo");// bizTaxInstanceCategoryVatSmall.getPrepayTaxCargoDeclareamount();
            calculationValue.stPrepaidAmount = amounts.get("service");//bizTaxInstanceCategoryVatSmall.getPrepayTaxServiceDeclareamount();
            calculationValue.ltPrepaidAmounttotal = bizTaxInstanceCategoryVatQ.getPrepayTaxCargoTotalamount();
            calculationValue.stPrepaidAmounttotal = bizTaxInstanceCategoryVatQ.getPrepayTaxServiceTotalamount();
            Map<String, Object> taxOfficeReport001 = new Report001().generate(calculationValue, history).toMap();
            result.put("agent", new BigDecimal(taxOfficeReport001.get("a22").toString()).add(new BigDecimal(taxOfficeReport001.get("b22").toString())));
        } else {
            ServiceTaxException.throwException(ServiceTaxException.Codes.prepaid_state_error, "预缴类型参数错误");
        }
        return result;
    }

    @Override
    public void updateBillItem(long id, long itemId, BizBillInvoiceItem bizBillInvoiceItem, long userId) {
        BizBillInvoiceItem billInvoiceItem = bizBillInvoiceItemService.findById(itemId);
        if (null == billInvoiceItem) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "发票明细不存在");
        }
        BizBillInvoice bizBillInvoice = bizBillInvoiceService.findById(billInvoiceItem.getBizBillInvoiceId());
        if (null == bizBillInvoice) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "发票不存在");
        }
        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatQ = Optional.ofNullable(bizTaxInstanceCategoryVatSmallService.findById(id))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.DateError, "税种不存在"));

        this.isAudit(bizTaxInstanceCategoryVatQ, bizBillInvoice.getMdCompanyId());
        bizBillInvoiceItemService.update(itemId, bizBillInvoiceItem);

        List<BizMdAccountCycle> list = bizMdAccountCycleService.findContainsMonth(bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().getBizTaxInstance().getMdAccountCycleId());
        if (CollectionUtils.isEmpty(list)) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "会计区间id不存在");
        }
        List<Long> accountCycleIds = list.stream().map(BizMdAccountCycle::getId).collect(Collectors.toList());

        updateAmount(id, bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory(), accountCycleIds, userId);

    }

    /**
     * 验证增值税
     */
    private void isAudit(BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatQ, long companyId) {

        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory();
        if (BizTaxInstanceCategory.State.send.equals(bizTaxInstanceCategory.getState()) && !BizTaxInstanceCategory.ProcessState.process.equals(bizTaxInstanceCategory.getProcessState())) {
            if (bizTaxInstanceCategory.isAudit()) {
                throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "该公司已经发票已经审核,请先取消审核");
            }
            if (bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId() != companyId) {
                throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "公司id不正确");
            }
        } else {
            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "该税种状态不是发起申报状态");
        }
    }

    public void updateAmount(long id, BizTaxInstanceCategory bizTaxInstanceCategory, List<Long> accountCycleIds, long userId) {
        logger.debug("开始修改bill 数据{}" + bizTaxInstanceCategory.getId());
        Map<String, BigDecimal> map = bizBillInvoiceService.countAmountByCompanyId(bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId(), accountCycleIds);
        logger.debug("开始修改bill 数据{}" + map.toString());
        bizTaxInstanceCategoryVatSmallService.updateByAmount(id, map.get(BizBillInvoice.BillType.output.name()), map.get(BizBillInvoice.BillType.agent.name()), map.get(BizBillInvoice.BillType.nobill.name()), userId);
        logger.debug("是否成功");
    }

    private void build(Map<Integer, List<BizTaxBillInvoiceSheetDTO>> sheet, Integer row, List<BizTaxBillInvoiceSheetDTO> datas, long instId, BizMdAccountCycle bizMdAccountCycle) {
//        BizTaxInstance bizTaxInstance = null;
//        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatQ = null;
        Map<String, Object> map = new HashMap<>();
        //状态 1 代表未确认  2 代表 确认  3 代表错误
        if (CollectionUtils.isNotEmpty(datas)) {
            for (BizTaxBillInvoiceSheetDTO bizTaxBillInvoiceSheetDTO : datas) {
//                this.build(bizTaxBillInvoiceSheetDTO, bizTaxInstance, bizTaxInstanceCategoryVatQ, instId, bizMdAccountCycle);
                this.build(bizTaxBillInvoiceSheetDTO, map, instId, bizMdAccountCycle);
            }
            sheet.put(row, datas);
        }
    }

    private void build(BizTaxBillInvoiceSheetDTO bizTaxBillInvoiceSheet, Map<String, Object> map, long instId, BizMdAccountCycle bizMdAccountCycle) {
        bizTaxBillInvoiceSheet.setErrorType(BizTaxBillInvoiceSheetDTO.ERROR.error);
//        if (null == bizTaxInstance) {
//            BizMdCompany bizMdCompany = bizMdCompanyService.findByFullName(bizTaxBillInvoiceSheet.getMdCompanyName());
//            if (null == bizMdCompany) {
//                bizTaxBillInvoiceSheet.setError("该公司信息不存在");
//                return;
//            }
//            BizMdInstClient bizMdInstClient = bizMdInstClientService.findByCompanyIdAndInstId(bizMdCompany.getId(), instId);
//            if (null == bizMdInstClient) {
//                bizTaxBillInvoiceSheet.setError("该公司机构客户信息不存在");
//                return;
//            }
//            bizTaxInstance = bizTaxInstanceService.currentTaxOfficeInstClientInstance(bizMdInstClient.getId(), bizMdCompany.getId(), bizMdAccountCycle.getId(), TaxOffice.gs, bizMdCompany.getTaxAreaId());
//            if (null == bizTaxInstance) {
//                bizTaxBillInvoiceSheet.setError("该公司税种不存在");
//                return;
//            }
//        }
//        if (null == bizTaxInstanceCategoryVatQ) {
//            bizTaxInstanceCategoryVatQ = bizTaxInstanceCategoryVatSmallService.findByTaxInstanceId(bizTaxInstance.getId());
//            if (null == bizTaxInstanceCategoryVatQ) {
//                bizTaxBillInvoiceSheet.setError("该公司增值税不存在");
//                return;
//            }
//        }
        if (map.size() == 0) {
            Map<String, Long> mapCompany = bizMdCompanyService.findByCompanyIdAndClientId(bizTaxBillInvoiceSheet.getMdCompanyName(), instId);
            if (null == mapCompany || mapCompany.size() == 0) {
                bizTaxBillInvoiceSheet.setError("该公司信息不存在");
                return;
            }
            Map<String, Object> mapVat = bizTaxInstanceCategoryVatSmallService.findByVatSmall(mapCompany.get("instClientId"), mapCompany.get("companyId"), bizMdAccountCycle.getId(), TaxOffice.gs.name(), mapCompany.get("taxAreaId"));
            if (null == mapVat || mapVat.size() == 0) {
                bizTaxBillInvoiceSheet.setError("该公司增值税不存在");
                return;
            } else {
                map.putAll(mapVat);
            }
        }


//        //判断服务类型是否正确
//        if (!Objects.equals(bizTaxInstance.getBillingType().toString(), Category.getValue(bizTaxBillInvoiceSheet.getCompanyType()).getValue())) {
//        bizTaxBillInvoiceSheet.setErrorType(BizTaxBillInvoiceSheetDTO.ERROR.error);
//            bizTaxBillInvoiceSheet.setError("项目类型应该为" + Category.getName(bizTaxInstance.getBillingType().toString()).getName());
//            return ;
//        }
        BillingType billingType = StringUtils.isEmpty(map.get("billType")) ? BillingType.none : BillingType.valueOf(map.get("billType").toString());
        if (Category.service.getValue().equals(billingType.toString()) && Category.cargo.getValue().equals(bizTaxBillInvoiceSheet.getCategory())) {
            bizTaxBillInvoiceSheet.setError("不能填写");
            return;
        }
        if (Category.cargo.getValue().equals(billingType.toString()) && Category.service.getValue().equals(bizTaxBillInvoiceSheet.getCategory())) {
            bizTaxBillInvoiceSheet.setError("不能填写");
            return;
        }

        BigDecimal declareAmount = null;
        try {
            declareAmount = new BigDecimal(bizTaxBillInvoiceSheet.getDeclareAmount()).setScale(2, BigDecimal.ROUND_HALF_UP);
        } catch (Exception e) {
            bizTaxBillInvoiceSheet.setError("格式错误");
            return;
        }
        if (declareAmount.compareTo(BigDecimal.ZERO) == -1) {
            bizTaxBillInvoiceSheet.setError("必须大于0,或者不填");
            return;
        }
        BizMdAccountCycle mdAccountCycle = bizMdAccountCycleService.findBySnAndType(bizTaxBillInvoiceSheet.getMdAccountCycle(), CycleType.m);
        if (null == mdAccountCycle) {
            bizTaxBillInvoiceSheet.setError("季度(月)会计区间错误");
            return;
        }
        if (!(mdAccountCycle.getBeginDate() >= bizMdAccountCycle.getBeginDate() && mdAccountCycle.getEndDate() <= bizMdAccountCycle.getEndDate())) {
            bizTaxBillInvoiceSheet.setError("季度(月)会计区间只能是" + getMonth(bizMdAccountCycle.getSn()).keySet());
            return;
        }
        bizTaxBillInvoiceSheet.setMdAccountCycleId(mdAccountCycle.getId());
//        bizTaxBillInvoiceSheet.setInstanceCategoryVatQId(bizTaxInstanceCategoryVatQ.getId());
//        bizTaxBillInvoiceSheet.setTaxInstanceId(bizTaxInstance.getId());
//        bizTaxBillInvoiceSheet.setMdCompanyId(bizTaxInstance.getMdCompanyId());
//        bizTaxBillInvoiceSheet.setInstanceCategoryId(bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategoryId());
        Long vatId = StringUtils.isEmpty(map.get("vatId")) ? 0 : Long.parseLong(map.get("vatId").toString());
        if (vatId == 0) {
            bizTaxBillInvoiceSheet.setError("该公司增值税不存在");
            return;
        }
        Long categoryId = StringUtils.isEmpty(map.get("categoryId")) ? 0 : Long.parseLong(map.get("categoryId").toString());
        if (categoryId == 0) {
            bizTaxBillInvoiceSheet.setError("该公司增值税不存在");
            return;
        }
        Long instanceId = StringUtils.isEmpty(map.get("instanceId")) ? 0 : Long.parseLong(map.get("instanceId").toString());
        if (instanceId == 0) {
            bizTaxBillInvoiceSheet.setError("该公司增值税不存在");
            return;
        }
        Long companyId = StringUtils.isEmpty(map.get("companyId")) ? 0 : Long.parseLong(map.get("companyId").toString());
        if (companyId == 0) {
            bizTaxBillInvoiceSheet.setError("该公司信息不存在");
            return;
        }
        bizTaxBillInvoiceSheet.setInstanceCategoryVatQId(vatId);
        bizTaxBillInvoiceSheet.setTaxInstanceId(instanceId);
        bizTaxBillInvoiceSheet.setMdCompanyId(companyId);
        bizTaxBillInvoiceSheet.setInstanceCategoryId(categoryId);
        bizTaxBillInvoiceSheet.setErrorType(BizTaxBillInvoiceSheetDTO.ERROR.success);
        Long audit = StringUtils.isEmpty(map.get("audit")) ? 0 : Long.parseLong(map.get("audit").toString());
//        if (bizTaxInstanceCategoryVatQ.getBizTaxInstanceCategory().isAudit()) {
//            bizTaxBillInvoiceSheet.setErrorType(BizTaxBillInvoiceSheetDTO.ERROR.warning);
//            bizTaxBillInvoiceSheet.setError("该公司发票已审核.");
//        }
        if (audit == 1) {
            bizTaxBillInvoiceSheet.setErrorType(BizTaxBillInvoiceSheetDTO.ERROR.warning);
            bizTaxBillInvoiceSheet.setError("该公司发票已审核.");
        }
    }

    private static Map<String, Boolean> getMonth(String date) {
        Map<String, Boolean> map = new HashMap<>();
        if (date.length() != 6){
            return map;
        }
        String str = date.substring(4, date.length());
        String year = date.substring(0,4);
        if ("03".equals(str)) {
            map.put(year+"01", true);
            map.put(year+"02", true);
            map.put(year+"03", true);
        } else if ("06".equals(str)) {
            map.put(year+"04", true);
            map.put(year+"05", true);
            map.put(year+"06", true);
        } else if ("09".equals(str)) {
            map.put(year+"07", true);
            map.put(year+"08", true);
            map.put(year+"09", true);
        } else if ("12".equals(str)) {
            map.put(year+"10", true);
            map.put(year+"11", true);
            map.put(year+"12", true);
        }

        return map;
    }
}

