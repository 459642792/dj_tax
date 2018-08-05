package com.yun9.service.tax.core.impl;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.bill.BizBillInvoiceService;
import com.yun9.biz.bill.domain.bo.InvoiceCountDto;
import com.yun9.biz.md.BizMdCompanyAccountService;
import com.yun9.biz.md.BizMdCompanyService;
import com.yun9.biz.md.BizMdInstClientService;
import com.yun9.biz.md.domain.dto.CompanyAccountDTO;
import com.yun9.biz.md.domain.entity.BizMdCompany;
import com.yun9.biz.md.domain.entity.BizMdCompanyAccount;
import com.yun9.biz.md.domain.entity.BizMdInstClient;
import com.yun9.biz.report.BizReportService;
import com.yun9.biz.tax.*;
import com.yun9.biz.tax.domain.entity.*;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdOfficeCategoryCycle;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.DeclareType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.framework.orm.commons.entity.Disabled;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.enums.TaxLabelEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @program: service-tax
 * @description: ${description}
 * @author: leigm
 * @create: 2018-06-05 16:08
 **/
@Component
public class PageCommon {

    public static final Logger logger = LoggerFactory.getLogger(PageCommon.class);

    @Autowired
    BizTaxCompanyBankService bizTaxCompanyBankService;

    @Autowired
    BizMdCompanyAccountService bizMdCompanyAccountService;

    @Autowired
    BizTaxInstanceCategoryReportService bizTaxInstanceCategoryReportService;

    @Autowired
    BizTaxInstanceCategoryAttachmentService bizTaxInstanceCategoryAttachmentService;

    @Autowired
    BizTaxInstanceCategoryDeductService bizTaxInstanceCategoryDeductService;

    @Autowired
    BizTaxInstanceCategoryVatSmallService bizTaxInstanceCategoryVatSmallService;

    @Autowired
    BizTaxInstanceCategoryPersonalBusinessService bizTaxInstanceCategoryPersonalBusinessService;

    @Autowired
    BizTaxInstanceCategoryPersonalPayrollService bizTaxInstanceCategoryPersonalPayrollService;

    @Autowired
    BizTaxInstanceCategoryFrService bizTaxInstanceCategoryFrService;

    @Autowired
    BizTaxInstanceCategoryFzService bizTaxInstanceCategoryFzService;

    @Autowired
    BizTaxInstanceCategoryYhService bizTaxInstanceCategoryYhService;

    @Autowired
    BizTaxInstanceCategoryBitService bizTaxInstanceCategoryBitService;

    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    BizTaxInstanceCategoryPersonalPayrollItemService bizTaxInstanceCategoryPersonalPayrollItemService;

    @Autowired
    BizMdInstClientService bizMdInstClientService;

    @Autowired
    BizMdCompanyService bizMdCompanyService;

    @Autowired
    BizReportService bizReportService;

    @Autowired
    BizBillInvoiceService bizBillInvoiceService;

    /**
     * 获取机构客户
     * @param orgTreeId 组织id
     * @param params    参数
     * @return
     */
    public List<BizMdInstClient> getBizMdInstClients(long orgTreeId, Map<String, Object> params) {

        List<com.yun9.biz.md.enums.State> states = null;
        if (params != null && params.size() > 1 && StringUtils.isNotEmpty(params.get("instClientStateType"))) {
            states = new ArrayList<com.yun9.biz.md.enums.State>() {{
                add(com.yun9.biz.md.enums.State.valueOf(params.get("instClientStateType").toString()));
            }};
        }

        return bizMdInstClientService.findByOrgTreeId(orgTreeId, states);
    }


    public static Map<String, Object> label2params(Map<String, Object> params) {
        if (params != null && params.size() > 1) {

            //标签 已经审核
            if (StringUtils.isNotEmpty(params.get("taxAuditType"))) {
                params.put("audit",params.get("taxAuditType").toString().equals(TaxLabelEnum.TaxAudit.AUDIT.getSn())==true?BigDecimal.ONE:BigDecimal.ZERO);//审核
            }

        }

        return  params;
    }

    public Map<String, List<BizTaxCompanyBank>> getBanksMap(List<Long> companyIds) {

        List<BizTaxCompanyBank> banks = bizTaxCompanyBankService.findByBizMdCompanyIdIn(companyIds);
        return new HashMap<String, List<BizTaxCompanyBank>>() {{
            banks.forEach(e -> {
                if (e != null) {
                    List<BizTaxCompanyBank> deductList = get(e.getBizMdCompanyId() + "_" + e.getTaxOffice());
                    if (deductList == null) {
                        deductList = new ArrayList();
                    }
                    deductList.add(e);
                    put(e.getBizMdCompanyId() + "_" + e.getTaxOffice(), deductList);
                }
            });
        }};
    }


    public Map<String, CompanyAccountDTO> getCompanyAccountDTOS(List<Long> companyIds) {

        List<CompanyAccountDTO> companyAccountDTOS = bizMdCompanyAccountService.findByCompanyIds(companyIds);

        return new HashMap<String, CompanyAccountDTO>() {{
            if (companyAccountDTOS != null) {
                companyAccountDTOS.forEach(e -> {
                    if (e != null) {
                        put(e.getCompanyId() + "_" + e.getType() + "_" + e.getState(), e);
                    }
                });
            }
        }};
    }


    public Map<String, InvoiceCountDto> getInvoiceCountDtosMap(List<Long> companyIds, List<Long> accountCycleIds) {
        List<InvoiceCountDto> invoiceCountDtoList = bizBillInvoiceService.countAmountByCompanyIds(companyIds, accountCycleIds);
        logger.info("-----------> invoiceCountDtoList:{}", JSON.toJSONString(invoiceCountDtoList));
        return new HashMap<String, InvoiceCountDto>() {{
            invoiceCountDtoList.forEach(e -> {
                if (e != null) {
                    put(e.getCompanyId() + "_" + e.getBillType(), e);
                }
            });
        }};
    }


    public HashMap<String, Object> filterLoginType(List<Long> companyIds,HashMap<String,Object> params){
        HashMap<String,Object> result = new HashMap<>();
        List<CompanyAccountDTO> companyAccountDTOS  ;
        Map<String, CompanyAccountDTO> companyAccountMap = new HashMap<>();
        if(params != null && StringUtils.isNotEmpty(params.get("loginType"))){//需要过滤默认登陆方式
            if("none".equals(params.get("loginType").toString())){
                companyAccountDTOS = bizMdCompanyAccountService.findActivateLoginTypeByCompanyIdsAndParams(companyIds, null);
                if(CollectionUtils.isNotEmpty(companyAccountDTOS)){
                    List<Long> _companyIds = Optional.ofNullable(companyAccountDTOS).orElse(new ArrayList<>()).stream().map(v->v.getCompanyId()).collect(Collectors.toList());
                    companyIds.removeAll(_companyIds);
                }
            }else {
                companyAccountDTOS = bizMdCompanyAccountService.findActivateLoginTypeByCompanyIdsAndParams(companyIds, params);
                companyIds = companyAccountDTOS.stream().map(v -> v.getCompanyId()).collect(Collectors.toList());
                List<CompanyAccountDTO> finalCompanyAccountDTOS = companyAccountDTOS;
                companyAccountMap = new HashMap<String, CompanyAccountDTO>() {{
                    finalCompanyAccountDTOS.forEach(e -> {
                        if (e != null) {
                            put(e.getCompanyId() + "_" + e.getType() + "_" + e.getState(), e);
                        }
                    });
                }};
            }
        }else{
            companyAccountMap = this.getCompanyAccountDTOS(companyIds);
        }
        result.put("companyIds",companyIds);
        result.put("companyAccountMap",companyAccountMap);
        return result;
    }

    public Map<Long, BizTaxInstanceCategoryReport> getReportCheckStatesMap(List<Long> instanceCategoryIds) {
        List<BizTaxInstanceCategoryReport> bizTaxInstanceCategoryReport = bizTaxInstanceCategoryReportService.findByBizTaxInstanceCategoryIdIn(instanceCategoryIds);
        return new HashMap<Long, BizTaxInstanceCategoryReport>() {{
            bizTaxInstanceCategoryReport.forEach(e -> {
                if (e != null) {
                    put(e.getBizTaxInstanceCategoryId(), e);
                }
            });
        }};
    }


    public Map<Long, List<BizTaxInstanceCategoryDeduct>> getDeductsMap(List<Long> instanceCategoryIds) {
        List<BizTaxInstanceCategoryDeduct> bizTaxInstanceCategoryDeducts = bizTaxInstanceCategoryDeductService.findByBizTaxInstanceCategoryIdIn(instanceCategoryIds);
        return new HashMap<Long, List<BizTaxInstanceCategoryDeduct>>() {{
            bizTaxInstanceCategoryDeducts.forEach(e -> {
                if (e != null) {
                    List<BizTaxInstanceCategoryDeduct> deductList = get(e.getBizTaxInstanceCategoryId());

                    if (deductList == null) {
                        deductList = new ArrayList();
                    }
                    deductList.add(e);
                    put(e.getBizTaxInstanceCategoryId(), deductList);
                }
            });
        }};
    }

    public HashMap<Long, List<BizTaxInstanceCategoryPersonalPayrollItem>> getIncomeCycle(List<Long> instanceCategoryPersonalPayrollIds) {
        List<BizTaxInstanceCategoryPersonalPayrollItem> items = bizTaxInstanceCategoryPersonalPayrollItemService.findByCategoryPersonalPayrollIdIn(instanceCategoryPersonalPayrollIds);
        HashMap<Long, List<BizTaxInstanceCategoryPersonalPayrollItem>> hashMap = new HashMap<Long, List<BizTaxInstanceCategoryPersonalPayrollItem>>() {{
            items.forEach(e -> {
                if (e != null) {
                    List<BizTaxInstanceCategoryPersonalPayrollItem> payrollItems = get(e.getBizTaxInstanceCategoryPersonalPayrollId());
                    if (payrollItems == null) {
                        payrollItems = new ArrayList();
                    }
                    payrollItems.add(e);
                    put(e.getBizTaxInstanceCategoryPersonalPayrollId(), payrollItems);
                }
            });
        }};

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");

        return new HashMap() {{
            hashMap.forEach((k, v) -> {

                HashSet set = new HashSet() {{
                    if (CollectionUtils.isNotEmpty(v)) {
                        v.forEach(v -> {
                            if (v.getBegindate() != null){
                                add(v.getBegindate().substring(0, v.getBegindate().lastIndexOf("-")));
                            }
                            if (v.getEnddate() !=null){
                                add(v.getEnddate().substring(0, v.getEnddate().lastIndexOf("-")));
                            }
                        });
                    }
                }};

                if (set.size() == 0){
                    put(k,new ArrayList());
                }else{
                    TreeSet ts = new TreeSet(set);
                    ts.comparator();



                    Date beginDate = null;
                    Date endDate = null;
                    try {

                        beginDate = formatter.parse(ts.first().toString());
                        endDate = formatter.parse(ts.last().toString());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    List<Date> dates = getDatesBetweenTwoDate(beginDate, endDate);


                    put(k, new ArrayList() {{
                        dates.forEach(v -> {
                            add(sdf.format(v));
                        });
                    }});
                }
            });
        }};
    }


    /**
     * 根据开始时间和结束时间返回时间段内的时间集合
     *
     * @param beginDate
     * @param endDate
     * @return List
     */
    public static List<Date> getDatesBetweenTwoDate(Date beginDate, Date endDate) {
        Calendar cal = Calendar.getInstance();
        List<Date> lDate = new ArrayList<Date>();
        lDate.add(beginDate);// 把开始时间加入集合

        //开始月份和结束月份相同则去重
        if (beginDate.compareTo(endDate) == 0) {
            return lDate;
        }

        // 使用给定的 Date 设置此 Calendar 的时间
        cal.setTime(beginDate);
        boolean bContinue = true;
        while (bContinue) {
            // 根据日历的规则，为给定的日历字段添加或减去指定的时间量
            cal.add(Calendar.MONTH, 1);
            // 测试此日期是否在指定日期之后
            if (endDate.after(cal.getTime())) {
                lDate.add(cal.getTime());
            } else {
                break;
            }
        }
        lDate.add(endDate);// 把结束时间加入集合
        return lDate;
    }




    public Map<Long, BizMdInstClient> getMdInstClientsMap(List<Long> instClientIds) {
        List<BizMdInstClient>  bizMdInstClients = bizMdInstClientService.findByIds(instClientIds);
        return new HashMap<Long, BizMdInstClient>() {{
            bizMdInstClients.forEach(e -> {
                if (e != null) {
                    put(e.getId(), e);
                }
            });
        }};
    }


    public Map<Long, BizMdCompany> getCompanysMap(List<Long> comapnyIds) {
        List<BizMdCompany> bizMdCompanys = bizMdCompanyService.findByIdIn(comapnyIds);
        return new HashMap<Long, BizMdCompany>() {{
            bizMdCompanys.forEach(e -> {
                if (e != null) {
                    put(e.getId(), e);
                }
            });
        }};
    }


    /**
     * 看是否有报表 & 报表是否审核
     * <p>
     * 报表审核时间耗时比较长，先计算时间长
     * 1.调用小江的方法
     * 2.打印时间
     *
     * @param instanceCategoryIds
     * @return
     */
    public Map<Long,HashMap> getReportStatesMap(List<Long> instanceCategoryIds) {
        List<BizTaxInstanceCategoryReport> bizTaxInstanceCategoryReports = bizTaxInstanceCategoryReportService.findByBizTaxInstanceCategoryIdIn(instanceCategoryIds);

        Map<Long, Boolean> tmpMap = null;
        if (CollectionUtils.isNotEmpty(bizTaxInstanceCategoryReports)) {
            List<Long> instanceIds = bizTaxInstanceCategoryReports.stream().map(v -> v.getBizReportInstanceId()).collect(Collectors.toList());
            tmpMap = bizReportService.check(instanceIds);
        }

        final Map<Long, Boolean> auditMap = tmpMap;
        return new HashMap() {{
            bizTaxInstanceCategoryReports.forEach(e -> {
                if (e != null) {
                    put(e.getBizTaxInstanceCategoryId(),new HashMap(){{
                        put("reportCheckState", BigDecimal.ONE);//申报表
                        put("reportAuditState", Optional.ofNullable(auditMap).map(v->v.get(e.getBizReportInstanceId())).orElse(false));//报表状态
                    }});
                }
            });
        }};
    }



    /**
     * 列表公共数据组装
     *
     * @return
     */
    public HashMap getCommonData(BizTaxInstanceCategory bizTaxInstanceCategory) {
        return new HashMap() {{
            put("companyId", bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId());//公司ID
            put("clientSn", bizTaxInstanceCategory.getBizTaxInstance().getMdClientSn());//
            put("companyName", bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyName());
            put("taxAreaId", bizTaxInstanceCategory.getBizTaxInstance().getMdAreaId());//税区
            put("taxType", bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyTaxType());//纳税方式 小规模、个体
            put("taxOffice", bizTaxInstanceCategory.getBizTaxInstance().getTaxOffice());//税局
            put("billingType", bizTaxInstanceCategory.getBizTaxInstance().getBillingType());//开票类型
            put("invoiceSystem", bizTaxInstanceCategory.getBizTaxInstance().getInvoiceSystem());//开票系统

            put("taxInstanceCategoryId", bizTaxInstanceCategory.getId());//申报税种category id
            put("taxPayAmount", PageCommon.formatToNumber(bizTaxInstanceCategory.getTaxPayAmount()));//应纳税额度
            put("realPayAmount", PageCommon.formatToNumber(bizTaxInstanceCategory.getRealPayAmount()));//缴税金额
            put("taxOfficePayAmount", bizTaxInstanceCategory.getTaxOfficePayAmount());//税局确认金额
            put("beginDate", bizTaxInstanceCategory.getBeginDate()); //申报期限 biz_tax_md_office_category_cycle begin end by
            put("endDate", bizTaxInstanceCategory.getEndDate());//申报所属结束月份
            put("startDate", bizTaxInstanceCategory.getStartDate());//申报开始日期
            put("closeDate", bizTaxInstanceCategory.getCloseDate());//申报截止日期
            put("processCodeId", bizTaxInstanceCategory.getProcessCodeId());//办理ID
            put("processState", bizTaxInstanceCategory.getProcessState());//执行状态
            put("processMessage", bizTaxInstanceCategory.getProcessMessage());//执行方式
            put("declareType", bizTaxInstanceCategory.getDeclareType());//申报方式
            put("taxDeclareType", bizTaxInstanceCategory.getTaxDeclareType());//申报类型

            put("declareCheckState", bizTaxInstanceCategory.getDeclareCheckState());//申报截图状态
            put("declareCheckDate", bizTaxInstanceCategory.getDeclareCheckDate());//申报截图价差时间

            put("audit", bizTaxInstanceCategory.getAudit());// 审核状态 0否1是
            put("auditBy", bizTaxInstanceCategory.getAuditBy());//审核人
            put("auditAt", bizTaxInstanceCategory.getAuditAt());//审核时间

            put("taxOfficeConfirm", bizTaxInstanceCategory.getTaxOfficeConfirm());//税局是否启用[none 无][disabled 未启用][enable 已启用]
            put("taxPaySn", bizTaxInstanceCategory.getTaxPaySn());//应征凭证序号
            put("accountCycleId",bizTaxInstanceCategory.getBizTaxInstance().getMdAccountCycleId());//会计区间id
            put("state", bizTaxInstanceCategory.getState());//当前操作状态

            //todo 数据来源待定
            put("screenshotsCount", null);//已用截图次数
            put("screenshotsTotal", null);//允许最大截图次数
        }};
    }



    public enum PayCheckState {
        none,single,list;
    }

    public enum DeductState {
        none,process;
    }

    public HashMap getMapCommonData(Map<String, CompanyAccountDTO> companyAccountMap,
                                    Map<Long, BizMdInstClient> bizMdInstClientsMap,
                                    Map<Long, HashMap> reportStatesMap,
                                    Map<Long, List<BizTaxInstanceCategoryDeduct>> deductsMap ,
                                    Map<String, List<BizTaxCompanyBank>> banksMap,
                                    BizTaxInstanceCategory bizTaxInstanceCategory) {
        return new HashMap() {{
            //todo 公共
            put("passwordType", new HashMap() {{
                put(TaxOffice.gs, Optional.ofNullable(companyAccountMap.get(bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId() + "_" + TaxOffice.gs + "_" + BizMdCompanyAccount.State.activate)).orElse(null));
                put(TaxOffice.ds, Optional.ofNullable(companyAccountMap.get(bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId() + "_" + TaxOffice.ds + "_" + BizMdCompanyAccount.State.activate)).orElse(null));
            }});

            put("instClientState",bizMdInstClientsMap.get(bizTaxInstanceCategory.getBizTaxInstance().getMdInstClientId()).getState());//机构客户状态

            //todo 缴款
            put("payCheckState", BigDecimal.ZERO); //缴款截图
            put("deductState", DeductState.none); //缴款状态
            put("deductType", BizTaxInstanceCategoryDeduct.Type.none);//缴款方式
            if (deductsMap != null && deductsMap.get(bizTaxInstanceCategory.getId()) != null) {
                List<BizTaxInstanceCategoryDeduct> deducts = deductsMap.get(bizTaxInstanceCategory.getId());
                put("payCheckState", deducts.size() == 1 ? PayCheckState.single.ordinal() : PayCheckState.list.ordinal() );
                put("deductState", DeductState.process);
                put("deductType", deducts.get(0).getType());
            }

            //todo 非公共
            put("reportCheckState", BigDecimal.ZERO);//申报表
            put("reportAuditState", false);//报表状态
            if (reportStatesMap !=null ){
                if (reportStatesMap.get(bizTaxInstanceCategory.getId()) != null) {
                    put("reportCheckState", reportStatesMap.get(bizTaxInstanceCategory.getId()).get("reportCheckState"));//申报表
                    put("reportAuditState", reportStatesMap.get(bizTaxInstanceCategory.getId()).get("reportAuditState"));//报表状态
                }
            }

            //银行信息
            put("banks", new ArrayList());
            if (banksMap != null){
                put("banks", JSON.toJSON(banksMap.get(bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId() + "_" + bizTaxInstanceCategory.getBizTaxInstance().getTaxOffice())));//银行名称
            }
        }};
    }
    //===================================创建对象===================================

    private void createVatSmall(long bizTaxInstanceCategoryId) {
        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatSmall = new BizTaxInstanceCategoryVatSmall();
        bizTaxInstanceCategoryVatSmall.setBizTaxInstanceCategoryId(bizTaxInstanceCategoryId);
        bizTaxInstanceCategoryVatSmall.setPrepayTaxSource(BizTaxInstanceCategoryVatSmall.PrepayTaxSource.none);
        bizTaxInstanceCategoryVatSmall.setTicketCheckState(BizTaxInstanceCategoryVatSmall.TicketCheckState.none);
        bizTaxInstanceCategoryVatSmallService.create(bizTaxInstanceCategoryVatSmall);
    }


    private void createPersonalBusiness(long bizTaxInstanceCategoryId) {
        BizTaxInstanceCategoryPersonalBusiness bizTaxInstanceCategoryPersonalBusiness = new BizTaxInstanceCategoryPersonalBusiness();
        bizTaxInstanceCategoryPersonalBusiness.setBizTaxInstanceCategoryId(bizTaxInstanceCategoryId);
        bizTaxInstanceCategoryPersonalBusiness.setIncomeAmount(BigDecimal.ZERO);
        bizTaxInstanceCategoryPersonalBusiness.setBuyAmount(BigDecimal.ZERO);
        bizTaxInstanceCategoryPersonalBusinessService.create(bizTaxInstanceCategoryPersonalBusiness);
    }


    private void createBit(long bizTaxInstanceCategoryId,HashMap<String, Object> params) {

        if (params.get("type") == null ) {
            throw BizTaxException.build(BizTaxException.Codes.BIZ_TAX_ERROR, "需要输入企业所得税类型（A类还是B类）");
        }

        BizTaxInstanceCategoryBit bizTaxInstanceCategoryBit = new BizTaxInstanceCategoryBit();
        bizTaxInstanceCategoryBit.setBizTaxInstanceCategoryId(bizTaxInstanceCategoryId);
        bizTaxInstanceCategoryBit.setSaleAmount(BigDecimal.ZERO);
        bizTaxInstanceCategoryBit.setBuyAmount(BigDecimal.ZERO);
        bizTaxInstanceCategoryBit.setProfitAmount(BigDecimal.ZERO);
        bizTaxInstanceCategoryBit.setAuditType(BizTaxInstanceCategoryBit.AuditType.none);
        bizTaxInstanceCategoryBit.setType(BizTaxInstanceCategoryBit.Type.valueOf(params.get("type").toString()));
        bizTaxInstanceCategoryBit.setAmountAuditState(0);
        bizTaxInstanceCategoryBitService.create(bizTaxInstanceCategoryBit);
    }

    private void createPersonalPayroll(long bizTaxInstanceCategoryId) {
        BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = new BizTaxInstanceCategoryPersonalPayroll();
        bizTaxInstanceCategoryPersonalPayroll.setBizTaxInstanceCategoryId(bizTaxInstanceCategoryId);
        bizTaxInstanceCategoryPersonalPayroll.setPopleNum(0);
        bizTaxInstanceCategoryPersonalPayroll.setTaxAmount(BigDecimal.ZERO);
        bizTaxInstanceCategoryPersonalPayroll.setSourceType(BizTaxInstanceCategoryPersonalPayroll.SourceType.sso);
        bizTaxInstanceCategoryPersonalPayroll.setAuditState(0);
        bizTaxInstanceCategoryPersonalPayrollService.create(bizTaxInstanceCategoryPersonalPayroll);
    }


    private void createFr(long bizTaxInstanceCategoryId) {
        BizTaxInstanceCategoryFr bizTaxInstanceCategoryFr = new BizTaxInstanceCategoryFr();
        bizTaxInstanceCategoryFr.setBizTaxInstanceCategoryId(bizTaxInstanceCategoryId);
        bizTaxInstanceCategoryFr.setAllInOne(BizTaxInstanceCategoryFr.ALLInOne.none);
        bizTaxInstanceCategoryFr.setTaxOfficeFrType(BizTaxInstanceCategoryFr.FrType.none);
        bizTaxInstanceCategoryFrService.create(bizTaxInstanceCategoryFr);
    }


    private void createFz(long bizTaxInstanceCategoryId) {
        BizTaxInstanceCategoryFz bizTaxInstanceCategoryFz = new BizTaxInstanceCategoryFz();
        bizTaxInstanceCategoryFz.setBizTaxInstanceCategoryId(bizTaxInstanceCategoryId);
        bizTaxInstanceCategoryFz.setVatState(BizTaxInstanceCategoryFz.VatState.none);
        bizTaxInstanceCategoryFz.setSoqState(BizTaxInstanceCategoryFz.SoqState.none);
        bizTaxInstanceCategoryFz.setBusinessTaxState(BizTaxInstanceCategoryFz.BusinessTaxState.none);
        bizTaxInstanceCategoryFzService.create(bizTaxInstanceCategoryFz);
    }


    private void createYh(long bizTaxInstanceCategoryId) {
        BizTaxInstanceCategoryYh bizTaxInstanceCategoryYh = new BizTaxInstanceCategoryYh();
        bizTaxInstanceCategoryYh.setBizTaxInstanceCategoryId(bizTaxInstanceCategoryId);
        bizTaxInstanceCategoryYh.setVatState(BizTaxInstanceCategoryYh.VatState.none);
        bizTaxInstanceCategoryYh.setVatSaleAmount(BigDecimal.ZERO);
        bizTaxInstanceCategoryYhService.create(bizTaxInstanceCategoryYh);
    }


    public BizTaxInstanceCategory createTax(BizTaxInstance bizTaxInstance, BizTaxMdCategory bizTaxMdCategory, BizTaxMdOfficeCategoryCycle bizTaxMdOfficeCategoryCycle , CycleType cycleType, TaxSn taxSn,HashMap<String, Object> params) {

        //1 创建BizTaxInstanceCategory 类
        BizTaxInstanceCategory bizTaxInstanceCategory = new BizTaxInstanceCategory();
        bizTaxInstanceCategory.setBizTaxInstanceId(bizTaxInstance.getId());
        bizTaxInstanceCategory.setStartDate(bizTaxMdOfficeCategoryCycle.getBeginDate());
        bizTaxInstanceCategory.setCloseDate(bizTaxMdOfficeCategoryCycle.getEndDate());
        bizTaxInstanceCategory.setBizTaxMdCategoryId(bizTaxMdCategory.getId());
        bizTaxInstanceCategory.setCycleType(cycleType);
        bizTaxInstanceCategory.setDeclareType(DeclareType.handwork);
        bizTaxInstanceCategory.setProcessState(BizTaxInstanceCategory.ProcessState.success);
        bizTaxInstanceCategory.setState(BizTaxInstanceCategory.State.complete);
        bizTaxInstanceCategory = bizTaxInstanceCategoryService.create(bizTaxInstanceCategory);


        //2 创建子税种
        long bizTaxInstanceCategoryId = bizTaxInstanceCategory.getId();

        if (taxSn == TaxSn.m_yh) {
            this.createYh(bizTaxInstanceCategoryId);
        } else if (taxSn == TaxSn.m_bit || taxSn == TaxSn.q_bit) {
            this.createBit(bizTaxInstanceCategoryId, params);
        } else if (taxSn == TaxSn.m_vat || taxSn == TaxSn.q_vat) {
            this.createVatSmall(bizTaxInstanceCategoryId);
        } else if (taxSn == TaxSn.m_fz || taxSn == TaxSn.q_fz) {
            this.createFz(bizTaxInstanceCategoryId);
        } else if (taxSn == TaxSn.m_personal_payroll || taxSn == TaxSn.q_personal_business) {
            this.createPersonalPayroll(bizTaxInstanceCategoryId);
        } else if (taxSn == TaxSn.m_personal_business || taxSn == TaxSn.q_personal_business) {
            this.createPersonalBusiness(bizTaxInstanceCategoryId);
        } else if (taxSn == TaxSn.m_fr || taxSn == TaxSn.q_fr) {
            this.createFr(bizTaxInstanceCategoryId);
        }

        //3 返回对象
        return bizTaxInstanceCategory;
    }

    /**
     * @desc 1.0~1之间的BigDecimal小数，格式化后失去前面的0,则前面直接加上0。
     * 2.传入的参数等于0，则直接返回字符串"0.00"
     * 3.大于1的小数，直接格式化返回字符串
     * @param
     * @return
     */
    private static String formatToNumber(BigDecimal obj) {
        DecimalFormat df = new DecimalFormat("#.00");
        if(obj.compareTo(BigDecimal.ZERO)==0) {
            return "0.00";
        }else if(obj.compareTo(BigDecimal.ZERO)>0&&obj.compareTo(new BigDecimal(1))<0){
            return "0"+df.format(obj).toString();
        }else {
            return df.format(obj).toString();
        }
    }


}

//        m_fr,
//        m_bit,
//        m_personal_payroll,
//        q_fr,
//        q_bit,
//        q_vat,
//        m_vat,
//        y_fr,
//        m_fz,
//        q_fz,
//        m_yh,
//        y_bit,
//        m_personal_business,
//        q_personal_business,
//        y_personal_business,
//        y_sbt,
//        unknown_fr,
//        not_support_tax;