package com.yun9.service.tax.core.impl;

import com.opencsv.CSVReader;
import com.yun9.biz.md.*;
import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.entity.BizMdAccountCycle;
import com.yun9.biz.md.domain.entity.BizMdCompany;
import com.yun9.biz.md.domain.entity.BizMdInstClient;
import com.yun9.biz.report.BizReportService;
import com.yun9.biz.task.domain.entity.BizTaskInstance;
import com.yun9.biz.tax.*;
import com.yun9.biz.tax.domain.entity.*;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryBit.AuditType;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryBit.Type;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.enums.DeclareType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.exception.BizException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.orm.commons.criteria.Pagination;
import com.yun9.service.tax.core.TaxInstanceCategoryBitFactory;
import com.yun9.service.tax.core.dto.*;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.ft.TaxMdCategoryHandler;
import com.yun9.service.tax.core.ft.ops.TaxBitQOperation;
import com.yun9.service.tax.core.report.ReportFactory;
import com.yun9.service.tax.core.utils.ExportExcelUtil;
import com.yun9.service.tax.core.utils.FileParse;
import com.yun9.service.tax.core.utils.FileUtil;
import org.apache.commons.collections.map.HashedMap;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Created by zhengzb on 2018/5/7.
 */
@Component
public class TaxInstanceCategoryBitFactoryImpl implements TaxInstanceCategoryBitFactory {

    public static final Logger logger = LoggerFactory
            .getLogger(TaxInstanceCategoryBitFactoryImpl.class);


    @Autowired
    BizMdInstOrgTreeClientService bizMdInstOrgTreeClientService;

    @Autowired
    BizMdCompanyAccountService bizMdCompanyAccountService;

    @Autowired
    BizTaxInstanceCategoryBitService bizTaxInstanceCategoryBitService;

    @Autowired
    BizTaxInstanceCategoryReportService bizTaxInstanceCategoryReportService;

    @Autowired
    BizTaxInstanceCategoryVatSmallService bizTaxInstanceCategoryVatSmallService;

    @Autowired
    BizTaxInstanceCategoryVatNormalService bizTaxInstanceCategoryVatNormalService;

    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    BizMdAccountCycleService bizMdAccountCycleService;

    @Autowired
    BizMdCompanyService bizMdCompanyService;

    @Autowired
    BizTaxInstanceService bizTaxInstanceService;

    @Autowired
    BizTaxMdCategoryService bizTaxMdCategoryService;

    @Autowired
    BizMdInstClientService bizMdInstClientService;

    @Autowired
    BizTaxInstanceCategoryHistoryService bizTaxInstanceCategoryHistoryService;

    @Autowired
    PageCommon pageCommon;
    @Autowired
    TaxMdCategoryHandler taxMdCategoryHandler;

    @Autowired
    private ReportFactory reportFactory;

    @Autowired
    private BizReportService bizReportService;

    @Autowired
    private BizTaxInstanceCategoryPersonalPayrollService bizTaxInstanceCategoryPersonalPayrollService;

    @Autowired
    private BizTaxInstanceCategoryPersonalPayrollItemService bizTaxInstanceCategoryPersonalPayrollItemService;


    @Override
    public Pagination<HashMap> pageByState(List<Long> accountCycleIds, long orgTreeId,
                                           BizTaxInstanceCategory.State state, int page, int limit, Map<String, Object> params) {

        Pagination pagination = new Pagination();
        pagination.setContent(new ArrayList());

        //1 检查组织id
        List<BizMdInstClient> bizMdInstClients = pageCommon.getBizMdInstClients(orgTreeId, params);
        if (CollectionUtils.isEmpty(bizMdInstClients)) {
            return pagination;
        }

        List<Long> instClientIds = bizMdInstClients.stream().map(v -> v.getId()).collect(Collectors.toList());
        Pagination<BizTaxInstanceCategoryBit> pageObj = bizTaxInstanceCategoryBitService.pageByState(accountCycleIds, instClientIds, state, page, limit, params);
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

        //6 企业增值税
        List<BizTaxInstanceCategoryVatSmall> bizTaxInstanceCategoryVatSmalls = bizTaxInstanceCategoryVatSmallService.findByBizTaxInstanceCategoryIdIn(instanceCategoryIds);
        Map<Long, BizTaxInstanceCategoryVatSmall> vatSmallMaps = new HashMap<Long, BizTaxInstanceCategoryVatSmall>(page) {{
            bizTaxInstanceCategoryVatSmalls.forEach(e -> {
                if (e != null) {
                    put(e.getBizTaxInstanceCategoryId(), e);
                }
            });
        }};
        //机构客户状态
        //6 机构客户状态
        Map<Long, BizMdInstClient> bizMdInstClientsMap = new HashMap<Long, BizMdInstClient>() {{
            bizMdInstClients.forEach(e -> {
                if (e != null) {
                    put(e.getId(), e);
                }
            });
        }};
        //总机构
        Map<Long, BizMdCompany> companysMap = pageCommon.getCompanysMap(companyIds);


        //7 组装数据
        pagination.setContent(new ArrayList() {{
            pageObj.getContent().forEach((v) -> {
                add(new HashMap() {{
                    //todo 公共数据
                    putAll(pageCommon.getCommonData(v.getBizTaxInstanceCategory()));

                    //todo 组织数据
                    putAll(pageCommon.getMapCommonData(companyAccountMap, bizMdInstClientsMap, reportStatesMap, deductsMap, banksMap, v.getBizTaxInstanceCategory()));


                    //todo 企业所得税
                    put("id", v.getId());
                    put("bitType", v.getType());//企业所得税类型（A类还是B类）
                    put("saleAmount", v.getSaleAmount());//销售收入
                    put("buyAmount", v.getBuyAmount());//购买成本
                    put("profitAmount", v.getProfitAmount());//利润总额
                    put("auditType", v.getAuditType());//核定类型 [none,cost, income]
                    put("amountAuditState", v.getAmountAuditState());//审核状态
                    put("amountAuditDate", v.getProfitAmount());//审核时间
                    put("auditPaytax", v.getAuditPaytax());//B类时，核定应纳税额
                    put("mechanismType", companysMap.get(v.getBizTaxInstanceCategory().getBizTaxInstance().getMdCompanyId()).getMechanismType());//总分机构
                    put("employeeNumber", v.getEmployeeNumber());//期末从业人数
                    put("technologySmallCompany", v.getTechnologySmallCompany());//是否科技型中小企业[Y,N]
                    put("highTechnologyCompany", v.getHighTechnologyCompany());//是否高新技术企业[Y,N]
                    put("technologyAdmissionMatter", v.getTechnologyAdmissionMatter());//是否技术入股递延纳税事项[Y,N]

                    //todo 增值税
                    put("vatSaleAmount", "0.00");//增值销售额
                    put("vatDeclareState", 0 + "");//增值税申报状态

                    BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatSmall = vatSmallMaps.get(v.getBizTaxInstanceCategoryId());
                    if (bizTaxInstanceCategoryVatSmall != null) {
                        put("vatDetail", new HashMap() {{
                            put("selfAmount", bizTaxInstanceCategoryVatSmall.getSelfAmount());//自开金额
                            put("agentAmount", bizTaxInstanceCategoryVatSmall.getSelfAmount());//代开金额
                            put("nobillAmount", bizTaxInstanceCategoryVatSmall.getSelfAmount());//无票金额

                        }});
                        put("vatSaleAmount", bizTaxInstanceCategoryVatSmall.getSaleAmount());//增值销售额
                        put("vatDeclareState", (bizTaxInstanceCategoryVatSmall.getBizTaxInstanceCategory().getState() != BizTaxInstanceCategory.State.complete ? 0 : 1) + "");//增值税申报状态
                    }

                }});
            });
        }});
        return pagination;

    }

    @Override
    public void downloadExcel(HttpServletRequest request, HttpServletResponse response,
                              List<Long> categoryIds, long mdAccountCycleId) {
        logger.debug("下载文件开始");

        //获取会计期间
        BizMdAccountCycle bizMdAccountCycle = Optional
                .ofNullable(bizMdAccountCycleService.findById(mdAccountCycleId))
                .orElseThrow(() -> BizTaxException
                        .throwException(BizTaxException.Codes.BIZ_TAX_ERROR, "申报月份不存在"));

        //获取企业所得税独有的A,B类型
        List<BizTaxInstanceCategoryBit> bitCategorys = bizTaxInstanceCategoryBitService
                .findByBizTaxInstanceCategoryIdIn(categoryIds);

        List<Map<String, Object>> dataMapList = new ArrayList<Map<String, Object>>();

        if (CollectionUtils.isNotEmpty(bitCategorys)) {
            bitCategorys.stream()
                    .forEach(bitCategory -> {
                        BizTaxInstanceCategory bizTaxInstanceCategory = bitCategory.getBizTaxInstanceCategory();
                        if (bizTaxInstanceCategory == null || !BizTaxInstanceCategory.State.send.equals(bizTaxInstanceCategory.getState())
                                || BizTaxInstanceCategory.ProcessState.process.equals(bizTaxInstanceCategory.getProcessState())) {
                            return;
                        }
                        BizTaxInstance bizTaxInstance = bizTaxInstanceCategory.getBizTaxInstance();
                        if (bizTaxInstance == null) {
                            return;
                        }

                        //组装excel需要的数据
                        Map<String, Object> dataMap = new HashMap<String, Object>() {{
                            put("mdCompanyName", bizTaxInstance.getMdCompanyName());
                            put("mdAccountCycle", bizMdAccountCycle.getSn());
                            put("type", bitCategory.getType() == null ? "" : bitCategory.getType());
                            String auditType = "";
                            if (bitCategory != null) {
                                switch (bitCategory.getAuditType()) {
                                    case cost:
                                        auditType = "核定成本";
                                        break;
                                    case income:
                                        auditType = "核定收入";
                                        break;
                                    case paytax:
                                        auditType = "应纳所得税额";
                                        break;
                                }
                            }
                            put("auditType", auditType);
                            put("saleAmount", bitCategory.getSaleAmount() == null ? "" : bitCategory.getSaleAmount());
                            put("buyAmount", "");
                            put("profitAmount", "");
                            String technologySmallCompany = "";
                            String highTechnologyCompany = "";
                            String technologyAdmissionMatter = "";
                            String employeeNumber = "";
                            if (bitCategory != null) {
                                if ("Y".equals(bitCategory.getTechnologySmallCompany())) {
                                    technologySmallCompany = "是";
                                } else if ("N".equals(bitCategory.getHighTechnologyCompany())) {
                                    technologySmallCompany = "否";
                                }
                                if ("Y".equals(bitCategory.getHighTechnologyCompany())) {
                                    highTechnologyCompany = "是";
                                } else if ("N".equals(bitCategory.getHighTechnologyCompany())) {
                                    highTechnologyCompany = "否";
                                }
                                if ("Y".equals(bitCategory.getTechnologyAdmissionMatter())) {
                                    technologyAdmissionMatter = "是";
                                } else if ("N".equals(bitCategory.getTechnologyAdmissionMatter())) {
                                    technologyAdmissionMatter = "否";
                                }
                                employeeNumber = bitCategory.getEmployeeNumber() == null ? "" : "" + bitCategory.getEmployeeNumber();
                            }

                            put("employeeNumber", employeeNumber);
                            put("technologyAdmissionMatter", technologyAdmissionMatter);
                            put("technologySmallCompany", technologySmallCompany);
                            put("highTechnologyCompany", highTechnologyCompany);
                        }};
                        dataMapList.add(dataMap);
                    });
        }

        try {
            Map<Integer, Short> rowHeight = new LinkedHashMap<Integer, Short>() {{
                put(0, (short) 1000);
                put(1, (short) 1000);
            }};

            ExportFileDTO exportFileDTO = new ExportFileDTO(request, response, dataMapList, rowHeight, bulidExportFileDTOs(), getExcelTitleMap(), 8, "sheet1", bizMdAccountCycle.getSn() + "企业所得税季报下载模板");
            ExportExcelUtil.downloadExcel(exportFileDTO);
        } catch (Exception e) {
            logger.error("下载错误", e);
            throw BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "下载错误");
        }
    }

    @Override
    public void profitAccounting(BizTaxInstanceCategoryBit bizTaxInstanceCategoryBit, long userID,
                                 String remark) throws IllegalAccessException {
        //检查参数非空
        chectBit(bizTaxInstanceCategoryBit);
        //检查A类B类
        TaxBitQOperation.checkBitAOrB(bizTaxInstanceCategoryBit);
        //b类去空指针
        if (Type.B.equals(bizTaxInstanceCategoryBit.getType())) {
            if (StringUtils.isEmpty(bizTaxInstanceCategoryBit.getHighTechnologyCompany())) {
                bizTaxInstanceCategoryBit.setHighTechnologyCompany("");
            }
            if (StringUtils.isEmpty(bizTaxInstanceCategoryBit.getTechnologyAdmissionMatter())) {
                bizTaxInstanceCategoryBit.setTechnologyAdmissionMatter("");
            }
            if (StringUtils.isEmpty(bizTaxInstanceCategoryBit.getTechnologySmallCompany())) {
                bizTaxInstanceCategoryBit.setTechnologySmallCompany("");
            }
        }

        //查询获取实例
        BizTaxInstanceCategoryBit bizTaxInstanceCategoryBitExist = Optional.ofNullable(
                bizTaxInstanceCategoryBitService.findById(bizTaxInstanceCategoryBit.getId()))
                .orElseThrow(() -> BizTaxException
                        .throwException(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误，没有找到税种实例"));
        //判断可用
        if (!bizTaxInstanceCategoryBitExist.isEnable()) {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "企业所得税申报实例不可用");
        }
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryBitExist.getBizTaxInstanceCategory();
        if (null == bizTaxInstanceCategory) {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "对应的申报实例为空");
        }
        if (bizTaxInstanceCategory.isAudit()) {
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "税种已审核,无需再次审核");
        }
        //保存
        bizTaxInstanceCategoryBitService.updateById(bizTaxInstanceCategoryBit, userID, remark);
        //调用统一审核
        taxMdCategoryHandler.audit(bizTaxInstanceCategory, userID, new TaxMdCategoryHandler.AuditCallback() {
            @Override
            public void success() {
                //生产报表
                //reportFactory.generate(bizTaxInstanceCategory, null);
            }

            @Override
            public void exception(BizException ex) {
                throw ex;
            }
        });
    }

    @Override
    public void cancelAudit(long id, long userID, String remark) {
        //查询获取实例
        BizTaxInstanceCategoryBit bizTaxInstanceCategoryBit = Optional
                .ofNullable(bizTaxInstanceCategoryBitService.findById(id))
                .orElseThrow(() -> BizTaxException
                        .throwException(BizTaxException.Codes.BIZ_TAX_ERROR, "参数错误，没有找到税种实例"));

        //检查是否为disable
        if (bizTaxInstanceCategoryBit.getDisabled() != 0) {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "该操作税种状态只能为可用状态");
        }
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryBit.getBizTaxInstanceCategory();

        if (!bizTaxInstanceCategory.isAudit()) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "该公司未审核,无需取消审核");
        }
        //取消报表审核
        BizTaxInstanceCategoryReport byTaxInstanceCategoryReport =
                bizTaxInstanceCategoryReportService.findByTaxInstanceCategoryId(bizTaxInstanceCategory.getId());
        if (null != byTaxInstanceCategoryReport) {
            bizTaxInstanceCategoryReportService.disabledByTaxInstanceCategoryId(bizTaxInstanceCategory.getId());
        }
        //调用统一审核
        taxMdCategoryHandler.cancelOfAudit(bizTaxInstanceCategory.getId(), userID);

    }

    @Override
    public BigDecimal getSaleAmountByVat(long id, long userID) {
        //查询税种实例
        BizTaxInstanceCategory bizTaxInstanceCategory = Optional
                .ofNullable(bizTaxInstanceCategoryService.findById(id))
                .orElseThrow(() -> BizTaxException
                        .throwException(BizTaxException.Codes.BIZ_TAX_ERROR, "获取税种信息失败!"));
        final Map<String, Object> val = new HashedMap();
        List<TaxSn> taxSnList = new ArrayList<>();
        taxSnList.add(TaxSn.m_vat);
        taxSnList.add(TaxSn.q_vat);
        Map<TaxSn, BizTaxMdCategory> bizTaxMdCategoryList = bizTaxMdCategoryService.findMapBySn(taxSnList);
        if (null != bizTaxMdCategoryList && bizTaxMdCategoryList.size() > 0) {
            bizTaxMdCategoryList.forEach((taxSn, bizTaxMdCategory) -> {
                BizTaxInstanceCategory bizTaxInstanceCategorys = bizTaxInstanceCategoryService.findByBizTaxMdCategoryIdAndBizTaxInstanceId(bizTaxMdCategory.getId(), bizTaxInstanceCategory.getBizTaxInstanceId());
                if (null != bizTaxInstanceCategorys) {
                    if (taxSn == TaxSn.q_vat) {
                        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatSmall = bizTaxInstanceCategoryVatSmallService.findByBizTaxInstanceCategoryId(bizTaxInstanceCategorys.getId());
                        if (null != bizTaxInstanceCategoryVatSmall && (bizTaxInstanceCategorys.getState().equals(BizTaxInstanceCategory.State.complete) || bizTaxInstanceCategorys.getState().equals(BizTaxInstanceCategory.State.deduct))) {
                            val.put("vat_sale_amount", bizTaxInstanceCategoryVatSmall.getSaleAmount());
                        }
                    } else if (taxSn == TaxSn.m_vat) {
                        BizTaxInstanceCategoryVatNormal bizTaxInstanceCategoryVatNormal = bizTaxInstanceCategoryVatNormalService.findByBizTaxInstanceCategoryId(bizTaxInstanceCategorys.getId());
                        if (null != bizTaxInstanceCategoryVatNormal && (bizTaxInstanceCategorys.getState().equals(BizTaxInstanceCategory.State.complete) || bizTaxInstanceCategorys.getState().equals(BizTaxInstanceCategory.State.deduct))) {
                            val.put("vat_sale_amount", bizTaxInstanceCategoryVatNormal.getSaleAmount());
                        }
                    }
                }
            });
        }
        if (StringUtils.isEmpty(val.get("vat_sale_amount")))
            ServiceTaxException.throwException(ServiceTaxException.Codes.vat_tax_not_found, "增值税还未申报!");
        return (BigDecimal) val.get("vat_sale_amount");
    }

    @Override
    public int getEmployeeNumberByPersonal(long id, long userID) {
        //查询税种实例
        BizTaxInstanceCategory bizTaxInstanceCategory = Optional
                .ofNullable(bizTaxInstanceCategoryService.findById(id))
                .orElseThrow(() -> BizTaxException
                        .throwException(BizTaxException.Codes.BIZ_TAX_ERROR, "获取税种信息失败!"));
        BizTaxInstance bizTaxInstanceGs = bizTaxInstanceService.findByMdInstClientIdAndMdCompanyIdAndMdAccountCycleIdAndTaxOfficeAndDisabled(bizTaxInstanceCategory.getBizTaxInstance().getMdInstClientId(), bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId(), bizTaxInstanceCategory.getBizTaxInstance().getMdAccountCycleId(), TaxOffice.ds);
        final Map<String, Object> val = new HashedMap();
        if (null != bizTaxInstanceGs) {
            List<TaxSn> taxSnList = new ArrayList<>();
            taxSnList.add(TaxSn.m_personal_payroll);
            Map<TaxSn, BizTaxMdCategory> bizTaxMdCategoryList = bizTaxMdCategoryService.findMapBySn(taxSnList);
            if (null != bizTaxMdCategoryList && bizTaxMdCategoryList.size() > 0) {
                bizTaxMdCategoryList.forEach((taxSn, bizTaxMdCategory) -> {
                    BizTaxInstanceCategory bizTaxInstanceCategorys = bizTaxInstanceCategoryService.findByBizTaxMdCategoryIdAndBizTaxInstanceId(bizTaxMdCategory.getId(), bizTaxInstanceGs.getId());
                    if (null != bizTaxInstanceCategorys) {
                        BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = bizTaxInstanceCategoryPersonalPayrollService.findByInstanceCategoryId(bizTaxInstanceCategorys.getId());
                        if (null != bizTaxInstanceCategoryPersonalPayroll && (bizTaxInstanceCategorys.getState().equals(BizTaxInstanceCategory.State.complete) || bizTaxInstanceCategorys.getState().equals(BizTaxInstanceCategory.State.deduct))) {
                            List<BizTaxInstanceCategoryPersonalPayrollItem> bizTaxInstanceCategoryPersonalPayrollItemList = bizTaxInstanceCategoryPersonalPayrollItemService.findByBizTaxInstanceCategoryPersonalPayrollIdAndUseType(bizTaxInstanceCategoryPersonalPayroll.getId(), BizTaxInstanceCategoryPersonalPayrollItem.UseType.result);
                            if (null != bizTaxInstanceCategoryPersonalPayrollItemList && bizTaxInstanceCategoryPersonalPayrollItemList.size() > 0) {
                                val.put("employee_number", bizTaxInstanceCategoryPersonalPayrollItemList.size());
                            } else {
                                ServiceTaxException.throwException(ServiceTaxException.Codes.personal_not_found, "获取个税人员信息失败!");
                            }
                        }

                    }
                });
            }
        }
        if (StringUtils.isEmpty(val.get("employee_number")))
            ServiceTaxException.throwException(ServiceTaxException.Codes.personal_tax_not_found, "个税还未申报!");
        return (int) val.get("employee_number");
    }

    @Override
    public void saveProfit(BizTaxInstanceCategoryBit bizTaxInstanceCategoryBit, long userID,
                           String remark) throws IllegalAccessException {
        //检查参数非空
        chectBit(bizTaxInstanceCategoryBit);
        //检查A类B类
        TaxBitQOperation.checkBitAOrB(bizTaxInstanceCategoryBit);
        //b类去空指针
        if (Type.B.equals(bizTaxInstanceCategoryBit.getType())) {
            if (StringUtils.isEmpty(bizTaxInstanceCategoryBit.getHighTechnologyCompany())) {
                bizTaxInstanceCategoryBit.setHighTechnologyCompany("");
            }
            if (StringUtils.isEmpty(bizTaxInstanceCategoryBit.getTechnologyAdmissionMatter())) {
                bizTaxInstanceCategoryBit.setTechnologyAdmissionMatter("");
            }
            if (StringUtils.isEmpty(bizTaxInstanceCategoryBit.getTechnologySmallCompany())) {
                bizTaxInstanceCategoryBit.setTechnologySmallCompany("");
            }
        }
        //查询获取实例
        BizTaxInstanceCategoryBit bizTaxInstanceCategoryExist = Optional.ofNullable(
                bizTaxInstanceCategoryBitService.findById(bizTaxInstanceCategoryBit.getId()))
                .orElseThrow(() -> BizTaxException
                        .throwException(BizTaxException.Codes.BizTaxException, "参数错误，没有找到税种实例"));
        //检查是否为disable
        if (bizTaxInstanceCategoryExist.getDisabled() != 0) {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "该操作税种状态只能为可用状态");
        }
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryExist.getBizTaxInstanceCategory();

        if (bizTaxInstanceCategory.isAudit()) {
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "税种已审核,不能发起暂存");
        }

        //保存
        bizTaxInstanceCategoryBitService.updateById(bizTaxInstanceCategoryBit, userID, remark);

    }

    @Override
    public void batchAudit(List<Long> ids, long userID) {
        logger.debug("批量审核开始");
        for (long id : ids) {
            logger.debug("当前企业所得税id为{}", id);
            BizTaxInstanceCategoryBit bizTaxInstanceCategoryBit = Optional.ofNullable(
                    bizTaxInstanceCategoryBitService.findById(id)
            ).orElseThrow(() ->
                    BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "未找到企业所得税申报实例")
            );
            //判断可用
            if (!bizTaxInstanceCategoryBit.isEnable()) {
                BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "企业所得税申报实例不可用");
            }
            //检查A类b类
            TaxBitQOperation.checkBitAOrB(bizTaxInstanceCategoryBit);
            BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryBit.getBizTaxInstanceCategory();
            if (bizTaxInstanceCategory.isAudit()) {
                BizTaxException
                        .throwException(BizTaxException.Codes.BizTaxException, "税种已审核,无需再次审核");
            }
            //调用审核
            //统一审核
            taxMdCategoryHandler.audit(bizTaxInstanceCategory, userID, new TaxMdCategoryHandler.AuditCallback() {
                @Override
                public void success() {
                    //审核成功
                }

                @Override
                public void exception(BizException ex) {
                    throw ex;
                }
            });
            logger.debug("审核企业所得税id为{}，审核通过", id);
        }
        logger.debug("批量审核完成");
    }


    /**
     * 获取excel表头信息
     */
    public static LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO>> getExcelTitleMap() {
        LinkedHashMap<Integer, LinkedHashMap<Integer, ? extends ExcelDTO>> map = new LinkedHashMap<>();
        LinkedHashMap<Integer, BizExcelDTO> oneRow = new LinkedHashMap<Integer, BizExcelDTO>();
        BizExcelDTO bizExcelDTOs = new BizExcelDTO();
        bizExcelDTOs.setName("\"填表说明：\n" +
                "1.客户名称必填且不可重复，可直接使用导出的模板数据\n" +
                "2.参考税种类型、核定类型填写利润数据；属于A类时，收入、成本、利润均须填写；属于B类时，核定收入可只填收入，核定成本可只填成本，核定应纳税额可只填应纳所得税额\n" +
                "3.属于需要填写的数据，若无发生额，可不填，系统将自动按0.00导入\"\t\t\t\t\t\t\n");
        bizExcelDTOs.setFontHeightInPoints((short) 10);
        bizExcelDTOs.setFontColor(IndexedColors.RED.getIndex());
        bizExcelDTOs.setBackgroundColor(null);
        oneRow.put(0, bizExcelDTOs);
        map.put(0, oneRow);

        LinkedHashMap<Integer, BizExcelDTO> twoRow = new LinkedHashMap<Integer, BizExcelDTO>();

        //公司信息
        BizExcelDTO bizExcelDTO = new BizExcelDTO();
        bizExcelDTO.setName("客户名称（必填）");
        bizExcelDTO.setFiled("mdCompanyName");
        bizExcelDTO.setComment("填写说明:\r\n填写客户全称，请勿重复");
        twoRow.put(0, bizExcelDTO);


        //企业所得税类型
        BizExcelDTO typeBizExcelDTO = new BizExcelDTO();
        typeBizExcelDTO.setName("税种类型（选填）");
        typeBizExcelDTO.setFiled("type");
        typeBizExcelDTO.setComment("填写说明:");
        twoRow.put(1, typeBizExcelDTO);

        //核定类型
        BizExcelDTO auditTypeBizExcelDTO = new BizExcelDTO();
        auditTypeBizExcelDTO.setName("核定类型（选填）");
        auditTypeBizExcelDTO.setFiled("auditType");
        auditTypeBizExcelDTO.setComment("填写说明:");
        twoRow.put(2, auditTypeBizExcelDTO);

        //营业收入（选填）
        BizExcelDTO saleAmountBizExcelDTO = new BizExcelDTO();
        saleAmountBizExcelDTO.setName("营业收入（必填）");
        saleAmountBizExcelDTO.setFiled("saleAmount");
        saleAmountBizExcelDTO.setComment("填写说明:");
        twoRow.put(3, saleAmountBizExcelDTO);

        //营业成本
        BizExcelDTO buyAmountBizExcelDTO = new BizExcelDTO();
        buyAmountBizExcelDTO.setName("营业成本（选填 ）");
        buyAmountBizExcelDTO.setFiled("buyAmount");
        buyAmountBizExcelDTO.setComment("填写说明:");
        twoRow.put(4, buyAmountBizExcelDTO);

        //利润总额（B类不填）
        BizExcelDTO profitAmountBizExcelDTO = new BizExcelDTO();
        profitAmountBizExcelDTO.setName("利润总额（选填）");
        profitAmountBizExcelDTO.setFiled("profitAmount");
        profitAmountBizExcelDTO.setComment("填写说明:");
        twoRow.put(5, profitAmountBizExcelDTO);

/*        //应纳所得税额
        BizExcelDTO paytaxBizExcelDTO = new BizExcelDTO();
        paytaxBizExcelDTO.setName("应纳所得税额（选填）");
        paytaxBizExcelDTO.setFiled("auditPaytax");
        paytaxBizExcelDTO.setComment("填写说明:");
        twoRow.put(6, paytaxBizExcelDTO);*/

        //科技型中小企业
        BizExcelDTO technologySmallCompanyBizExcelDTO = new BizExcelDTO();
        technologySmallCompanyBizExcelDTO.setName("科技型中小企业（A类必填）");
        technologySmallCompanyBizExcelDTO.setFiled("technologySmallCompany");
        technologySmallCompanyBizExcelDTO.setComment("填写说明:请填写是或否");
        twoRow.put(6, technologySmallCompanyBizExcelDTO);

        //高新技术企业
        BizExcelDTO highTechnologyCompanyBizExcelDTO = new BizExcelDTO();
        highTechnologyCompanyBizExcelDTO.setName("高新技术企业（A类必填）");
        highTechnologyCompanyBizExcelDTO.setFiled("highTechnologyCompany");
        highTechnologyCompanyBizExcelDTO.setComment("填写说明:请填写是或否");
        twoRow.put(7, highTechnologyCompanyBizExcelDTO);

        //高新技术企业
        BizExcelDTO technologyAdmissionMatterBizExcelDTO = new BizExcelDTO();
        technologyAdmissionMatterBizExcelDTO.setName("技术入股递延纳税事项（A类必填）");
        technologyAdmissionMatterBizExcelDTO.setFiled("technologyAdmissionMatter");
        technologyAdmissionMatterBizExcelDTO.setComment("填写说明:请填写是或否");
        twoRow.put(8, technologyAdmissionMatterBizExcelDTO);

        BizExcelDTO employeeNumberBizExcelDTO = new BizExcelDTO();
        employeeNumberBizExcelDTO.setName("期末从业人数（必填）");
        employeeNumberBizExcelDTO.setFiled("employeeNumber");
        employeeNumberBizExcelDTO.setComment("填写说明:请填写数字");
        twoRow.put(9, employeeNumberBizExcelDTO);

        map.put(1, twoRow);

        return map;
    }

    public static List<ExcelMergedDTO> bulidExportFileDTOs() {
        List<ExcelMergedDTO> excelMergedDTOS = new ArrayList<>();
        ExcelMergedDTO excelMergedDTO = new ExcelMergedDTO();
        excelMergedDTO.setMergedBeginRow(0);
        excelMergedDTO.setMergedEndRow(0);
        excelMergedDTO.setMergedBeginCol(0);
        excelMergedDTO.setMergedEndCol(6);
        excelMergedDTOS.add(excelMergedDTO);
        return excelMergedDTOS;
    }

    @Override
    public BizTaxBitImportDTO parseBitExcel(BizTaxBitImportSheetDTO importSheetDTO) {
        logger.debug("解析个税工资薪金文件开始");

        if (null == importSheetDTO.getFileData() || "".equals(importSheetDTO.getFileData())) {
            throw BizTaxException.build(BizTaxException.Codes.Biz_Vat_Import_File_Error);
        }

        BizMdAccountCycle bizMdAccountCycle = Optional
                .ofNullable(bizMdAccountCycleService.findById(importSheetDTO.getMdAccountCycleId()))
                .orElseThrow(() -> BizTaxException
                        .throwException(BizTaxException.Codes.TAX_VAT_CYCLE_ERRORS, "申报月份不存在"));

        String str = FileUtil.importFile(importSheetDTO.getFileUploadPath(),
                importSheetDTO.getFileData(), importSheetDTO.getFileOriginalName());
        File file = new File(str);

        List<BizTaxBitItemDTO> sheetData = parseBit(file, bizMdAccountCycle,
                importSheetDTO.getInstId(), importSheetDTO.getTaxOffice());

        BizTaxBitImportDTO bizTaxBitImportDTO = new BizTaxBitImportDTO();
        bizTaxBitImportDTO.setAuditSheet(sheetData);
        return bizTaxBitImportDTO;
    }

    /**
     * 个人所得税解析数据
     */
    public List<BizTaxBitItemDTO> parseBit(File file, BizMdAccountCycle bizMdAccountCycle,
                                           long instId, TaxOffice taxOffice) {
        logger.debug("开始解析文件：{}", file.getName());

        String fileType = file.getName().substring(file.getName().lastIndexOf("."));
        String type = StringUtils.isNotEmpty(fileType.substring(1)) ? fileType.substring(1) : null;
        logger.debug("文件名:{},文件类型:{}", file.getName(), type);

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            if ("csv".equals(type)) {
                return readCSVBit(inputStream, bizMdAccountCycle, instId, taxOffice);
            } else {
                return parseExcelBit(type, inputStream, bizMdAccountCycle, instId, taxOffice);
            }
        } catch (FileNotFoundException e) {
            logger.error("解析文件出现错误.", e);
            file.delete();
            throw BizTaxException
                    .build(BizTaxException.Codes.BIZ_REPORT_FILE_PARSE_IO_EXCEPTION, "支持xlsx,xls,csv");

        } finally {
            logger.debug("关闭文件输入流");
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<BizTaxBitItemDTO> parseExcelBit(String fileType, InputStream inputStream,
                                                 BizMdAccountCycle bizMdAccountCycle, long instId, TaxOffice taxOffice) {
        Workbook wb = FileParse.readExcel(fileType, inputStream);
        int sheetNumber = wb.getNumberOfSheets();
        logger.debug("解析Excel文件，共找到{}张Sheet", sheetNumber);
        List<BizTaxBitItemDTO> auditSheet = null;
        for (int i = 0; i < 1; i++) {
            //构建数据行
            auditSheet = buildPersonalSheet(wb.getSheetAt(i), bizMdAccountCycle, instId, taxOffice);
        }
        return auditSheet;
    }

    private List<BizTaxBitItemDTO> buildPersonalSheet(Sheet sheet,
                                                      BizMdAccountCycle bizMdAccountCycle, long instId, TaxOffice taxOffice) {

        int maxRow = sheet.getLastRowNum() + 1;

        logger.debug("解析Excel具体Sheet:{}，sheet共有{}行", sheet.getSheetName(), maxRow);
        List<BizTaxBitItemDTO> bizTaxBitItemDTOS = new ArrayList<>();

        List<String> message;
        for (int i = 0; i < maxRow; i++) {
            Row row = sheet.getRow(i);
            if (null == row) {
                break;
            }
            if (i == 0) {
                continue;
            }
            if (i == 1) {
                if (row.getCell(0).toString().contains("客户名称") && row.getCell(1).toString()
                        .contains("税种类型")) {
                    continue;
                } else {
                    throw BizTaxException
                            .build(BizTaxException.Codes.BIZ_REPORT_FILE_PARSE_IO_EXCEPTION,
                                    "请按照下载的模板提交");
                }
            } else {

                message = new ArrayList<>();
                BizTaxBitItemDTO bizTaxBitItemDTO = new BizTaxBitItemDTO();
                int code = 0;
                bizTaxBitItemDTO
                        .setCompany(
                                row.getCell(0) == null ? null : row.getCell(0).toString().replace(" ", ""));
                bizTaxBitItemDTO.setSzlx(row.getCell(1) == null ? null : row.getCell(1).toString());
                bizTaxBitItemDTO.setHdlx(row.getCell(2) == null ? null : row.getCell(2).toString());
                bizTaxBitItemDTO
                        .setIncome(row.getCell(3) == null ? null : row.getCell(3).toString());
                bizTaxBitItemDTO.setCost(row.getCell(4) == null ? null : row.getCell(4).toString());
                bizTaxBitItemDTO
                        .setProfit(row.getCell(5) == null ? null : row.getCell(5).toString());
                bizTaxBitItemDTO.setTechnologySmallCompany(row.getCell(6) == null ? null : row.getCell(6).toString());
                bizTaxBitItemDTO.setHighTechnologyCompany(row.getCell(7) == null ? null : row.getCell(7).toString());
                bizTaxBitItemDTO.setTechnologyAdmissionMatter(row.getCell(8) == null ? null : row.getCell(8).toString());
                bizTaxBitItemDTO.setEmployeeNumber(row.getCell(9) == null ? null : row.getCell(9).toString());
                if (bizTaxBitItemDTO.getEmployeeNumber() != null) {
                    int num = new Double(bizTaxBitItemDTO.getEmployeeNumber()).intValue();
                    bizTaxBitItemDTO.setEmployeeNumber(num + "");
                }
                if (StringUtils.isEmpty(bizTaxBitItemDTO.getCompany())) {
                    code = 1;
                    message.add("客户名称不能为空");
                }

                if (code == 1) {
                    bizTaxBitItemDTO.setMessage(message);
                    bizTaxBitItemDTO.setCode("error");
                    bizTaxBitItemDTOS.add(bizTaxBitItemDTO);
                    continue;
                }

                CheckValue(bizTaxBitItemDTO, bizMdAccountCycle, instId, taxOffice);
                bizTaxBitItemDTOS.add(bizTaxBitItemDTO);
            }
        }
        return bizTaxBitItemDTOS;
    }


    private List<BizTaxBitItemDTO> readCSVBit(InputStream inputStream, BizMdAccountCycle bizMdAccountCycle, long instId, TaxOffice taxOffice) {

        CSVReader reader = null;
        List<BizTaxBitItemDTO> bizTaxBitItemDTOS = new ArrayList<>();
        List<String> message;

        try {
            reader = new CSVReader(new InputStreamReader(inputStream, "gbk"));
            String[] strArr;
            int i = 1;
            while ((strArr = reader.readNext()) != null) {
                if (i == 1) {
                    i++;
                    continue;
                }
                if (i == 2) {
                    if (strArr[0].contains("客户名称") && strArr[1].contains("税种类型")) {
                        i++;
                        continue;
                    } else {
                        throw BizTaxException
                                .build(BizTaxException.Codes.BIZ_REPORT_FILE_PARSE_IO_EXCEPTION,
                                        "请按照下载的模板提交");
                    }
                }

                int code = 0;
                BizTaxBitItemDTO bizTaxBitItemDTO = new BizTaxBitItemDTO();
                message = new ArrayList<>();

                bizTaxBitItemDTO.setCompany(strArr[0] == null ? null : strArr[0].replace(" ", ""));
                bizTaxBitItemDTO.setSzlx(strArr[1]);
                bizTaxBitItemDTO.setHdlx(strArr[2]);
                bizTaxBitItemDTO.setIncome(strArr[3]);
                bizTaxBitItemDTO.setCost(strArr[4]);
                bizTaxBitItemDTO.setProfit(strArr[5]);
                bizTaxBitItemDTO.setTechnologySmallCompany(strArr[6]);
                bizTaxBitItemDTO.setHighTechnologyCompany(strArr[7]);
                bizTaxBitItemDTO.setTechnologyAdmissionMatter(strArr[8]);
                bizTaxBitItemDTO.setEmployeeNumber(strArr[9]);
                if (bizTaxBitItemDTO.getEmployeeNumber() != null) {
                    int num = new Double(bizTaxBitItemDTO.getEmployeeNumber()).intValue();
                    bizTaxBitItemDTO.setEmployeeNumber(num + "");
                }
                if (strArr[0] == null || "".equals(strArr[0])) {
                    code = 1;
                    message.add("客户名称不能为空");
                }

                if (code == 1) {
                    bizTaxBitItemDTO.setMessage(message);
                    bizTaxBitItemDTO.setCode("error");
                    bizTaxBitItemDTOS.add(bizTaxBitItemDTO);
                    i++;
                    continue;
                }

                CheckValue(bizTaxBitItemDTO, bizMdAccountCycle, instId, taxOffice);
                bizTaxBitItemDTOS.add(bizTaxBitItemDTO);
                i++;
            }
        } catch (Exception e) {
            logger.debug("POI解析Excel输入流异常", e);
            throw BizTaxException
                    .build(BizTaxException.Codes.BIZ_REPORT_FILE_PARSE_IO_EXCEPTION, "解析csv异常,");
        } finally {
            logger.debug("关闭文件输入流");
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bizTaxBitItemDTOS;
    }

    private void CheckValue(BizTaxBitItemDTO bizTaxBitItemDTO, BizMdAccountCycle bizMdAccountCycle,
                            long instId, TaxOffice taxOffice) {
        String company = bizTaxBitItemDTO.getCompany();
        BizMdCompany bizMdCompany = bizMdCompanyService.findByFullName(company);
        if (bizTaxBitItemDTO.getMessage() == null) {
            bizTaxBitItemDTO.setMessage(new ArrayList<>());
        }
        if (bizMdCompany == null) {
            bizTaxBitItemDTO.setCode("error");
            bizTaxBitItemDTO.getMessage().add("公司不存在");
            return;
        }

        BizMdInstClient bizMdInstClient = bizMdInstClientService
                .findByCompanyIdAndInstId(bizMdCompany.getId(), instId);
        if (bizMdInstClient == null) {
            bizTaxBitItemDTO.setCode("error");
            bizTaxBitItemDTO.getMessage().add("公司不存在");
            return;
        }

        //todo ...需要公司id
        BizTaxInstance bizTaxInstance = bizTaxInstanceService.currentTaxOfficeInstClientInstance(bizMdInstClient.getId(), bizMdInstClient.getBizMdCompanyId(), bizMdAccountCycle.getId(), taxOffice, bizMdCompany.getTaxAreaId());
        if (bizTaxInstance == null) {
            bizTaxBitItemDTO.setCode("error");
            bizTaxBitItemDTO.getMessage().add("不存在符合该公司的税种");
            return;
        }

        //获取税种id
        BizTaxMdCategory bizTaxMdCategory = bizTaxMdCategoryService.findBySn(TaxSn.q_bit);
        if (!Optional.ofNullable(bizTaxMdCategory).isPresent()) {
            bizTaxBitItemDTO.setCode("error");
            bizTaxBitItemDTO.getMessage().add("不存在符合该公司的税种");
            return;
        }

        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService
                .findByInstanceAndTaxMdCategoryId(bizTaxInstance.getId(), bizTaxMdCategory.getId());

        if (!Optional.ofNullable(bizTaxInstanceCategory).isPresent()) {
            bizTaxBitItemDTO.setCode("error");
            bizTaxBitItemDTO.getMessage().add("不存在符合该公司的税种");
            return;
        }


        //从业人数必填
        if (StringUtils.isEmpty(bizTaxBitItemDTO.getEmployeeNumber())) {
            bizTaxBitItemDTO.setCode("error");
            bizTaxBitItemDTO.getMessage().add("期末从业人数必须填写");
            return;
        }

        List<BizTaxInstanceCategoryBit> bizTaxInstanceCategoryBitList = bizTaxInstanceCategoryBitService
                .findByBizTaxInstanceCategoryIdIn(new ArrayList<Long>() {{
                    add(bizTaxInstanceCategory.getId());
                }});
        if (CollectionUtils.isEmpty(bizTaxInstanceCategoryBitList)) {
            bizTaxBitItemDTO.setCode("error");
            bizTaxBitItemDTO.getMessage().add("不存在符合该公司的税种");
            return;
        }
        BizTaxInstanceCategoryBit bizTaxInstanceCategoryBit = bizTaxInstanceCategoryBitList.get(0);
        Type type = bizTaxInstanceCategoryBit.getType();
        if (type.equals(Type.A)) {
            bizTaxBitItemDTO.setSzlx("A");
            bizTaxBitItemDTO.setHdlx(null);
            bizTaxBitItemDTO.setId(bizTaxInstanceCategoryBit.getId());
            setIncome(bizTaxBitItemDTO);
            setCost(bizTaxBitItemDTO);
            setProfit(bizTaxBitItemDTO);
            if (StringUtils.isEmpty(bizTaxBitItemDTO.getTechnologySmallCompany())) {
                bizTaxBitItemDTO.setCode("error");
                bizTaxBitItemDTO.getMessage().add("是否科技型中小企业必须填写");
            }
            if (!StringUtils.isEmpty(bizTaxBitItemDTO.getTechnologySmallCompany()) && (!"是".equals(bizTaxBitItemDTO.getTechnologySmallCompany()) && !"否".equals(bizTaxBitItemDTO.getTechnologySmallCompany()))) {
                bizTaxBitItemDTO.setCode("error");
                bizTaxBitItemDTO.getMessage().add("是否科技型中小企业填写错误,只能填写\"是\"或\"否\"");
            }
            if (StringUtils.isEmpty(bizTaxBitItemDTO.getHighTechnologyCompany())) {
                bizTaxBitItemDTO.setCode("error");
                bizTaxBitItemDTO.getMessage().add("是否高新技术企业必须填写");
            }

            if (!StringUtils.isEmpty(bizTaxBitItemDTO.getHighTechnologyCompany()) && (!"是".equals(bizTaxBitItemDTO.getHighTechnologyCompany()) && !"否".equals(bizTaxBitItemDTO.getHighTechnologyCompany()))) {
                bizTaxBitItemDTO.setCode("error");
                bizTaxBitItemDTO.getMessage().add("是否高新技术企业填写错误,只能填写\"是\"或\"否\"");
            }

            if (StringUtils.isEmpty(bizTaxBitItemDTO.getTechnologyAdmissionMatter())) {
                bizTaxBitItemDTO.setCode("error");
                bizTaxBitItemDTO.getMessage().add("是否技术入股递延纳税事项必须填写");
            }

            if (!StringUtils.isEmpty(bizTaxBitItemDTO.getTechnologyAdmissionMatter()) && (!"是".equals(bizTaxBitItemDTO.getTechnologyAdmissionMatter()) && !"否".equals(bizTaxBitItemDTO.getTechnologyAdmissionMatter()))) {
                bizTaxBitItemDTO.setCode("error");
                bizTaxBitItemDTO.getMessage().add("是否技术入股递延纳税事项填写错误,只能填写\"是\"或\"否\"");
            }
        } else if (type.equals(Type.B)) {
            bizTaxBitItemDTO.setSzlx("B");
            bizTaxBitItemDTO.setId(bizTaxInstanceCategoryBit.getId());
            AuditType auditType = bizTaxInstanceCategoryBit.getAuditType();
            if (auditType.equals(AuditType.income)) {
                bizTaxBitItemDTO.setHdlx("核定收入");
                setIncome(bizTaxBitItemDTO);
                if (!StringUtils.isEmpty(bizTaxBitItemDTO.getCost())) {
                    //bizTaxBitItemDTO.getMessage().add("营业成本不应该填值");
                    bizTaxBitItemDTO.setCost("0.00");
                }
                if (!StringUtils.isEmpty(bizTaxBitItemDTO.getYnse())) {
                    //bizTaxBitItemDTO.getMessage().add("应纳税额不应该填值");
                    bizTaxBitItemDTO.setYnse("0.00");
                }
            } else if (auditType.equals(AuditType.cost)) {
                bizTaxBitItemDTO.setHdlx("核定成本");
                setCost(bizTaxBitItemDTO);
                if (!StringUtils.isEmpty(bizTaxBitItemDTO.getIncome())) {
                    //bizTaxBitItemDTO.getMessage().add("营业收入不应该填值");
                    bizTaxBitItemDTO.setIncome("0.00");
                }
                if (!StringUtils.isEmpty(bizTaxBitItemDTO.getYnse())) {
                    //bizTaxBitItemDTO.getMessage().add("应纳税额不应该填值");
                    bizTaxBitItemDTO.setYnse("0.00");
                }
            } else if (auditType.equals(AuditType.paytax)) {
                bizTaxBitItemDTO.setHdlx("核定应纳税额");
                setPayTax(bizTaxBitItemDTO);
                if (!StringUtils.isEmpty(bizTaxBitItemDTO.getCost())) {
                    //bizTaxBitItemDTO.getMessage().add("营业成本不应该填值");
                    bizTaxBitItemDTO.setCost("0.00");
                }
                if (!StringUtils.isEmpty(bizTaxBitItemDTO.getIncome())) {
                    //bizTaxBitItemDTO.getMessage().add("营业收入不应该填值");
                    bizTaxBitItemDTO.setIncome("0.00");
                }
            } else {
                bizTaxBitItemDTO.setCode("error");
                bizTaxBitItemDTO.getMessage().add("未找到该公司税种核定类型");
            }
        }
        if (bizTaxBitItemDTO.getMessage().size() == 0) {
            bizTaxBitItemDTO.setCode("success");
            int state = bizTaxInstanceCategory.getAudit();
            if (state == 1) {
                bizTaxBitItemDTO.getMessage().add("申报数据已审核，继续导入将覆盖审核");
                bizTaxBitItemDTO.setCode("warning");
            }
        }else{
            bizTaxBitItemDTO.setCode("error");
        }
    }

    private void setIncome(BizTaxBitItemDTO bizTaxBitItemDTO) {
        if (StringUtils.isEmpty(bizTaxBitItemDTO.getIncome())) {
            bizTaxBitItemDTO.setIncome("0");
            return;
        }
        try {
            BigDecimal income = new BigDecimal(bizTaxBitItemDTO.getIncome());
            if (income.doubleValue() < 0) {
                bizTaxBitItemDTO.getMessage().add("营业收入不能为负数");
            }
        } catch (Exception e) {
            bizTaxBitItemDTO.getMessage().add("营业收入格式错误");
        }
    }

    private void setProfit(BizTaxBitItemDTO bizTaxBitItemDTO) {
        if (StringUtils.isEmpty(bizTaxBitItemDTO.getProfit())) {
            bizTaxBitItemDTO.setProfit("0");
            return;
        }
        try {
            new BigDecimal(bizTaxBitItemDTO.getProfit());
        } catch (Exception e) {
            bizTaxBitItemDTO.getMessage().add("利润总和格式错误");
        }
    }

    private void setCost(BizTaxBitItemDTO bizTaxBitItemDTO) {
        if (StringUtils.isEmpty(bizTaxBitItemDTO.getCost())) {
            bizTaxBitItemDTO.setCost("0");
            return;
        }
        try {
            BigDecimal income = new BigDecimal(bizTaxBitItemDTO.getCost());
            if (income.doubleValue() < 0) {
                bizTaxBitItemDTO.getMessage().add("营业成本不能为负数");
            }
        } catch (Exception e) {
            bizTaxBitItemDTO.getMessage().add("营业成本格式错误");
        }
    }

    private void setPayTax(BizTaxBitItemDTO bizTaxBitItemDTO) {
        if (StringUtils.isEmpty(bizTaxBitItemDTO.getYnse())) {
            bizTaxBitItemDTO.setYnse("0");
            return;
        }
        try {
            BigDecimal ynse = new BigDecimal(bizTaxBitItemDTO.getYnse());
            if (ynse.doubleValue() < 0) {
                bizTaxBitItemDTO.getMessage().add("应纳税额不能为负数");
            }
        } catch (Exception e) {
            bizTaxBitItemDTO.getMessage().add("应纳税额格式错误");
        }
    }

    private void chectBit(BizTaxInstanceCategoryBit bizTaxInstanceCategoryBit) throws IllegalAccessException {
        if (bizTaxInstanceCategoryBit == null) {
            throw BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "操作失败,传入数据为空");
        }
        Field[] declaredFields = bizTaxInstanceCategoryBit.getClass().getDeclaredFields();
        List<String> errors = new ArrayList<>();
        for (Field f : declaredFields) {
            f.setAccessible(true);
            logger.info(f.getName());
            if (f.get(bizTaxInstanceCategoryBit) == null || f.get(bizTaxInstanceCategoryBit) == "") {
                switch (f.getName()) {
                    case "type":
                        errors.add("企业所得税类型不能为空");
                        break;
                    case "auditType":
                        errors.add("核定类型不能为空");
                        break;
                    case "saleAmount":
                        errors.add("营业收入不能为空");
                        break;
                    case "buyAmount":
                        errors.add("营业成本不能为空");
                        break;
                    //case "auditPaytax":
                    //    errors.add("核定应纳税额不能为空");
                    //    break;
                    case "profitAmount":
                        errors.add("经营利润不能为空");
                        break;
                    case "employeeNumber":
                        errors.add("期末从业人数不能为空");
                        break;
                }
            }
        }
        if (errors.size() > 0) {
            String err = "";
            for (String e : errors) {
                err += e + ",";
            }
            throw BizTaxException.throwException(BizTaxException.Codes.BizTaxException, err);
        }
    }
}