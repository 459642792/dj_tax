package com.yun9.service.tax.core.task.callback.handler;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.bill.BizBillInvoiceService;
import com.yun9.biz.bill.domain.bo.BizBillInvoiceAgentInvoiceDto;
import com.yun9.biz.bill.domain.entity.BizBillInvoice;
import com.yun9.biz.bill.domain.entity.BizBillInvoiceItem;
import com.yun9.biz.bill.domain.enums.Category;
import com.yun9.biz.md.BizMdAccountCycleService;
import com.yun9.biz.md.BizMdAreaService;
import com.yun9.biz.md.BizMdCompanyService;
import com.yun9.biz.md.domain.entity.BizMdAccountCycle;
import com.yun9.biz.md.domain.entity.BizMdArea;
import com.yun9.biz.md.domain.entity.BizMdCompany;
import com.yun9.biz.tax.BizTaxCompanyBankService;
import com.yun9.biz.tax.BizTaxInstanceCategoryPersonalPayrollService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.BizTaxInstanceService;
import com.yun9.biz.tax.domain.dto.tax.CompanyTax;
import com.yun9.biz.tax.domain.dto.tax.DeclareTaxCategory;
import com.yun9.biz.tax.domain.dto.tax.TaxOfficeRegisterTaxCategory;
import com.yun9.biz.tax.domain.entity.*;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdMsgCode;
import com.yun9.biz.tax.enums.*;
import com.yun9.biz.tax.ops.BizTaxMultipleStartService;
import com.yun9.commons.exception.BizException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.DateUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.TaxInstanceCategoryPersonalPayrollItemFactory;
import com.yun9.service.tax.core.event.ServiceTaxEventPublisher;
import com.yun9.service.tax.core.event.TaskDownloadComplete;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.task.annotation.TaskSnMapping;
import com.yun9.service.tax.core.task.callback.TaskCallBackContext;
import com.yun9.service.tax.core.task.helper.JsonMap;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.yun9.service.tax.core.exception.ServiceTaxException.Codes.CallbackProcessFailed;


/**
 * Created by werewolf on  2018/5/21.
 * 深圳综合查询回调(下载综合数据(深圳))
 */
@TaskSnMapping(sns = {"SZ0024", "SZ0050"})
public class GetTaxCategoriesCallBack extends AbstractCallbackHandlerMapping {

    public static Logger logger = LoggerFactory.getLogger(GetTaxCategoriesCallBack.class);

    @Autowired
    private BizMdAccountCycleService bizMdAccountCycleService;

    @Autowired
    private BizTaxMultipleStartService bizTaxMultipleStartService;

    @Autowired
    private BizTaxInstanceCategoryPersonalPayrollService bizTaxInstanceCategoryPersonalPayrollService;

    @Autowired
    private BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    private TaxInstanceCategoryPersonalPayrollItemFactory taxInstanceCategoryPersonalPayrollItemFactory;


    @Autowired
    private ServiceTaxEventPublisher serviceTaxEventPublisher;
    @Autowired
    private BizBillInvoiceService bizBillInvoiceService;

    @Autowired
    private BizMdCompanyService bizMdCompanyService;

    @Autowired
    private BizMdAreaService bizMdAreaService;

    @Autowired
    private BizTaxCompanyBankService bizTaxCompanyBankService;

    @Autowired
    private BizTaxInstanceService bizTaxInstanceService;

    @Override
    public TaskCallBackContext process(TaskCallBackContext context) {
        JsonMap body = JSON.parseObject(context.getBody(), JsonMap.class);
        final BizTaxInstance bizTaxInstance = context.getBizTaxInstance();
        final BizMdAccountCycle bizMdAccountCycle = bizMdAccountCycleService.findById(bizTaxInstance.getMdAccountCycleId());


        //上一次申报月份
        final BizMdAccountCycle bizMdAccountCycleLastMonth = Optional.ofNullable(bizMdAccountCycleService.findBySnAndType(bizMdAccountCycle.getSn(), com.yun9.biz.md.enums.CycleType.m, 1, -1)).orElse(new ArrayList<>()).stream().findFirst().orElse(null);
        //上一次申报季度
        final BizMdAccountCycle bizMdAccountCycleLastQuarter = Optional.ofNullable(bizMdAccountCycleService.findBySnAndType(bizMdAccountCycle.getSn(), com.yun9.biz.md.enums.CycleType.q, 1, -1)).orElse(new ArrayList<>()).stream().findFirst().orElse(null);

        Map<String, Object> otherInfo = new HashMap() {{
            put("dsData", body.getBoolean("dsData"));
            put("gsData", body.getBoolean("gsData"));

            if (bizMdAccountCycle.getType() == com.yun9.biz.md.enums.CycleType.q) {
                put("currentQuarterCycleBeginDate", bizMdAccountCycle.getBeginDate());
                put("currentQuarterCycleEndDate", bizMdAccountCycle.getEndDate());
                final BizMdAccountCycle bizMdAccountCycleMonth = bizMdAccountCycleService.findBySnAndType(bizMdAccountCycle.getSn(), com.yun9.biz.md.enums.CycleType.m);
                put("currentMonthCycleBeginDate", bizMdAccountCycleMonth.getBeginDate());
                put("currentMonthCycleEndDate", bizMdAccountCycleMonth.getEndDate());
            } else if (bizMdAccountCycle.getType() == com.yun9.biz.md.enums.CycleType.m) {
                put("currentMonthCycleBeginDate", bizMdAccountCycle.getBeginDate());
                put("currentMonthCycleEndDate", bizMdAccountCycle.getEndDate());
            }

            if (null != bizMdAccountCycleLastMonth) {
                put("lastMonthAccountCycleId", bizMdAccountCycleLastMonth.getId());
            }
            if (null != bizMdAccountCycleLastQuarter) {
                put("lastQuarterAccountCycleId", bizMdAccountCycleLastQuarter.getId());
            }
            BizMdArea bizMdArea = bizMdAreaService.findById(bizTaxInstance.getMdAreaId());
            if (null != bizMdArea) {
                put("areaSn", bizMdArea.getSn());
            }
            BizMdAccountCycle _bizMdAccountCycle = personalPayrollLastMothDeclare(bizTaxInstance, bizMdAccountCycleLastMonth);
            if (null != _bizMdAccountCycle) {
                //个税上一次申报
                put("personalPayrollLastMothAccountCycleId", _bizMdAccountCycle.getId());
            }
        }};

        //======================处理税中数据==============================

        logger.debug("当前会计期间{},申报周期{}", bizMdAccountCycle.getSn(), bizMdAccountCycle.getType());

        //税局认定税种
        List<TaxOfficeRegisterTaxCategory> taxOfficeRegisterTaxCategories = ProcessBody.registerTaxCategory(body, bizTaxInstance);
        //申报清册数据
        List<DeclareTaxCategory> declareTaxCategories = ProcessBody.declareTaxCategory(body, bizTaxInstance, bizMdAccountCycle);
        //客户税务信息
        CompanyTax companyTax = ProcessBody.companyTax(body);

        //是否有发票信息,解析发票信息[添加判断是否和上次下载发票信息一致]
        this.resolveAndProcessInvoice(bizTaxInstance, body, otherInfo);

        List<BizTaxInstanceCategory> instanceCategories = bizTaxMultipleStartService.callback(
                context.getBizTaxInstance().getId(),
                context.getTaskCallBackResponse().getSeq(),
                CycleType.valueOf(bizMdAccountCycle.getType().name()),
                taxOfficeRegisterTaxCategories,
                declareTaxCategories,
                companyTax,
                otherInfo
        );
        logger.debug("-------回调创建税种信息成功{}------", instanceCategories.size());
        if (CollectionUtils.isEmpty(instanceCategories)) {
            throw ServiceTaxException.build(CallbackProcessFailed, "回调执行失败没有创建税种信息");
        }

        //个税人员清单
        this.resolveAndProcessPersonalPayrollItem(bizTaxInstance, instanceCategories, body);

        //银行信息的回写
        this.resolveAndProcessBankInfo(bizTaxInstance, body);

        for (BizTaxInstanceCategory instanceCategory : instanceCategories) {
            //调用报表信息,保存/生成报表
            try {
                logger.debug("-----------发布下载税种完成事件------------{}", JSON.toJSONString(instanceCategory));
                TaskDownloadComplete taskDownloadComplete = new TaskDownloadComplete();
                taskDownloadComplete.setBizTaxInstanceCategory(instanceCategory);
                taskDownloadComplete.setBody(body);
                serviceTaxEventPublisher.publish(taskDownloadComplete);
            } catch (BizException ex) {
                logger.error("执行下载完成事件出现错误{}", ex.getMessage());
            }
        }
        return context;
    }


    protected void resolveAndProcessInvoice(BizTaxInstance bizTaxInstance, JsonMap body, Map<String, Object> otherInfo) {
        if (hasInvoice(body)) {
            //处理发票
            List<BizBillInvoiceAgentInvoiceDto> invoiceList = this.resolveInvoice(bizTaxInstance, body);
            logger.debug("解析后发票信息{}", JSON.toJSONString(invoiceList));
            List<Long> accountCycleIds = bizMdAccountCycleService.findContainsMonth(bizTaxInstance.getMdAccountCycleId()).stream().map(BizMdAccountCycle::getId).collect(Collectors.toList());
            List<BizBillInvoice> bizBillInvoices = bizBillInvoiceService.companyInvoiceAtAccountCycles(bizTaxInstance.getMdCompanyId(), accountCycleIds.stream().mapToLong(Long::longValue).toArray(), BizBillInvoice.BillType.agent);
            //比较上次和该次下载发票数据是否相同
            if (!this.isSameAsInvoice(bizBillInvoices, invoiceList)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("-------对比发票存在不同的发票信息------------");
                }
                otherInfo.put("differentInvoice", "存在不同的发票信息");
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("-----------对比发票信息相同------------------");
                }

            }
            //保存 发票信息
            if (CollectionUtils.isNotEmpty(invoiceList)) {
                bizBillInvoiceService.batchCreate(bizTaxInstance.getMdCompanyId(), BizBillInvoice.Source.taxoffice, BizBillInvoice.BillType.agent, true, accountCycleIds, invoiceList);
            }
        }
    }

    protected boolean isSameAsInvoice(List<BizBillInvoice> history, List<BizBillInvoiceAgentInvoiceDto> current) {
        if (logger.isDebugEnabled()) {
            logger.debug("开始对比发票信息原始发票信息{}", JSON.toJSONString(history));
            logger.debug("开始对比发票信息下载后发票信息{}", JSON.toJSONString(current));
        }
        Map<String, BizBillInvoice> historyMap = new HashMap() {{
            if (null != history) {
                history.forEach(v -> {
                    put(v.getBatchSn() + "_" + v.getBillSn(), v);
                });
            }
        }};
        Map<String, BizBillInvoiceAgentInvoiceDto> currentMap = new HashMap() {{
            if (null != current) {
                current.forEach(v -> {
                    put(v.getBatchSn() + "_" + v.getBillSn(), v);
                });
            }
        }};
        if (historyMap.size() != currentMap.size()) {
            return false;
        }

        for (String sn : historyMap.keySet()) {
            BizBillInvoice bizBillInvoice = historyMap.get(sn);
            BizBillInvoiceAgentInvoiceDto bizBillInvoiceAgentInvoiceDto = currentMap.get(sn);
            //比较发票金额
            if (bizBillInvoice.getAmount().setScale(4, BigDecimal.ROUND_HALF_UP).compareTo(bizBillInvoiceAgentInvoiceDto.getAmount().setScale(4, BigDecimal.ROUND_HALF_UP)) != 0) {
                return false;
            }
            //比较发票分类信息
            List<BizBillInvoiceItem> items = bizBillInvoice.getItems();
            List<BizBillInvoiceAgentInvoiceDto.Item> _items = bizBillInvoiceAgentInvoiceDto.getItems();
            if (items.size() != _items.size()) {
                return false;
            }
            if (items.size() > 0 && _items.size() > 0) {
                if (!items.get(0).getDeclareCategory().equals(_items.get(0).getCategory())) {
                    return false;
                }
            }
        }
        return true;
    }


    protected BizMdAccountCycle personalPayrollLastMothDeclare(BizTaxInstance bizTaxInstance, BizMdAccountCycle lastBizMdAccountCycle) {
        if (null == lastBizMdAccountCycle) {
            return null;
        }
        BizTaxMdCategory bizTaxMdCategory = bizTaxMdCategoryService.findBySn(TaxSn.m_personal_payroll);
        BizTaxInstance lastBizTaxInstance = bizTaxInstanceService.findByMdInstClientIdAndMdCompanyIdAndMdAccountCycleIdAndTaxOfficeAndDisabled(
                bizTaxInstance.getMdInstClientId(), bizTaxInstance.getMdCompanyId(), lastBizMdAccountCycle.getId(), TaxOffice.ds);
        if (null != bizTaxMdCategory && null != lastBizTaxInstance) {
            BizTaxInstanceCategory lastBizTaxInstanceCategory = bizTaxInstanceCategoryService.findByInstanceAndTaxMdCategoryId(lastBizTaxInstance.getId(), bizTaxMdCategory.getId());
            if (null != lastBizTaxInstanceCategory) {
                BizTaxInstanceCategoryPersonalPayroll lastDeclare = bizTaxInstanceCategoryPersonalPayrollService.findByInstanceCategoryId(lastBizTaxInstanceCategory.getId());
                if (null == lastDeclare) {
                    return Optional.ofNullable(bizMdAccountCycleService.findBySnAndType(lastBizMdAccountCycle.getSn(), com.yun9.biz.md.enums.CycleType.m, -1, 1)).orElse(new ArrayList<>()).stream().findFirst().orElse(null);
                }
            }
        }
        return null;
    }

    protected void resolveAndProcessPersonalPayrollItem(BizTaxInstance bizTaxInstance, List<BizTaxInstanceCategory> instanceCategories, JsonMap body) {
        //找到个税的税种表
        BizTaxMdCategory bizTaxMdCategory = bizTaxMdCategoryService.findBySn(TaxSn.m_personal_payroll);
        if (bizTaxMdCategory == null) {
            return;
        }
        BizTaxInstanceCategory bizTaxInstanceCategory = instanceCategories.stream().filter(v -> v.getBizTaxMdCategoryId() == bizTaxMdCategory.getId()).findFirst().orElse(null);
        if (null != bizTaxInstanceCategory) {
            JsonMap personalInfo = body.getObject("personalInfo");
            if (null != personalInfo && StringUtils.isNotEmpty(personalInfo.getString("code")) && personalInfo.getString("code").equals("200")) {
                List<JsonMap> _personalItem = personalInfo.getArray("data");
                if (null != _personalItem && _personalItem.size() > 0) {
                    List<BizTaxInstanceCategoryPersonalPayrollItem> personalItems = ProcessBody.personalItem(_personalItem);
                    if (null != personalItems && personalItems.size() > 0) {
                        BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = bizTaxInstanceCategoryPersonalPayrollService.findByInstanceCategoryId(bizTaxInstanceCategory.getId());
                        if (null != bizTaxInstanceCategoryPersonalPayroll) {
                            taxInstanceCategoryPersonalPayrollItemFactory.bacthCreate(bizTaxInstance.getMdInstClientId(), bizTaxInstance.getMdCompanyId(), bizTaxInstance.getMdAccountCycleId(), bizTaxInstance.getMdAreaId(), bizTaxInstanceCategoryPersonalPayroll.getId(), personalItems);
                        }
                    }
                }
            } else {
                if (bizTaxInstanceCategory.getId() > 0) {
                    bizTaxInstanceCategory.setProcessCodeId(BizTaxMdMsgCode.Process.taxoffice_exception.getCode());
                    bizTaxInstanceCategory.setProcessMessage("下载个税人员清单失败，税局服务器异常,请稍后重试：" + personalInfo.getString("message"));
                    bizTaxInstanceCategoryService.create(bizTaxInstanceCategory);
                } else {
                    //todo 临时查看
                    logger.error("出现重复数据原因{}", bizTaxInstanceCategory);
                }

            }
        }
    }

    //下载银行
    protected void resolveAndProcessBankInfo(BizTaxInstance bizTaxInstance, JsonMap body) {
        List<BizTaxCompanyBank> bizTaxCompanyBanks = bankInfo(bizTaxInstance, body);
        if (null != bizTaxCompanyBanks && bizTaxCompanyBanks.size() > 0) {
            //保存银行信息
            bizTaxCompanyBanks.forEach(item -> {
                bizTaxCompanyBankService.create(item);
            });
        }
    }

    private List<BizTaxCompanyBank> bankInfo(BizTaxInstance bizTaxInstance, JsonMap body) {
        List<JsonMap> _bankInfo = body.getArray("bankInfoList");
        if (null != _bankInfo && _bankInfo.size() > 0) {
            List<BizTaxCompanyBank> bizTaxCompanyBanks = new ArrayList<>();
            for (JsonMap _map : _bankInfo) {
                TaxOffice taxOffice = null != _map.getString("taxOffice") && _map.getString("taxOffice").equals("gs") ? TaxOffice.gs : TaxOffice.ds;
                BizTaxCompanyBank bizTaxCompanyBank = bizTaxCompanyBankService.findByBizMdCompanyIdAndAccountAndTaxOffice(bizTaxInstance.getMdCompanyId(), _map.getString("accountType"), taxOffice);
                if (null == bizTaxCompanyBank) {
                    bizTaxCompanyBank = new BizTaxCompanyBank();
                }
                String taxoffice = null != _map.getString("taxOffice") && _map.getString("taxOffice").equals("gs") ? "gs" : (null != _map.getString("taxOffice") && _map.getString("taxOffice").equals("ds") ? "ds" : "none");
                bizTaxCompanyBank.setBizMdCompanyId(bizTaxInstance.getMdCompanyId());
                bizTaxCompanyBank.setAccount((_map.getString("account")));
                bizTaxCompanyBank.setAccountType((_map.getString("accountType")));
                bizTaxCompanyBank.setBankBranchName((_map.getString("bankBranchName")));
                bizTaxCompanyBank.setBankBranchSn((_map.getString("bankBranchSn")));
                bizTaxCompanyBank.setBankName((_map.getString("bankName")));
                bizTaxCompanyBank.setBankSn((_map.getString("bankSn")));
                bizTaxCompanyBank.setBindSn((_map.getString("bindSn")));
                bizTaxCompanyBank.setCurrencyType((_map.getString("currencyType")));
                bizTaxCompanyBank.setDisplayAccount((_map.getString("displayAccount")));
                bizTaxCompanyBank.setIsDrawback((_map.getString("isDrawback").equals("Y") ? 1 : 0));
                bizTaxCompanyBank.setIsFirstTax((_map.getString("isFirstTax").equals("Y") ? 1 : 0));
                bizTaxCompanyBank.setTaxOffice(TaxOffice.valueOf(taxoffice));
                bizTaxCompanyBank.setTaxOfficeSn((_map.getString("taxOfficeSn")));
                bizTaxCompanyBanks.add(bizTaxCompanyBank);
            }
            return bizTaxCompanyBanks;
        }
        return null;
    }

    protected boolean hasInvoice(JsonMap body) {
        return body.getObject("invoicedatas") != null;
    }

    public static class ProcessBody {

        public static List<DeclareTaxCategory> declareTaxCategory(JsonMap body, BizTaxInstance bizTaxInstance, BizMdAccountCycle bizMdAccountCycle) {
            List<JsonMap> _taxCategories = body.getArray("taxCategorys");
            if (CollectionUtils.isEmpty(_taxCategories)) {
                return null;
            }
            List<DeclareTaxCategory> result = new ArrayList<>();
            for (JsonMap _map : _taxCategories) {
                DeclareTaxCategory declareTaxCategory = new DeclareTaxCategory();
                declareTaxCategory.setDeclared(_map.getString("alreadyDeclared").equals("Y") ? true : false);

                if (bizTaxInstance.getTaxOffice().equals(TaxOffice.gs)) {
                    String[] _cycle = _map.getString("cycle").split("~");//2018-04-01~2018-06-30
                    if (_cycle.length == 2 && !_cycle[0].startsWith("-")) {
                        declareTaxCategory.setStartDate(DateUtils.stringDateToTimeStampSecs(_cycle[0], DateUtils.ZH_PATTERN_DAY));
                        declareTaxCategory.setEndDate(DateUtils.stringDateToTimeStampSecs(_cycle[1], DateUtils.ZH_PATTERN_DAY));
                    } else {
                        declareTaxCategory.setStartDate(bizMdAccountCycle.getBeginDate());
                        declareTaxCategory.setEndDate(bizMdAccountCycle.getEndDate());
                    }
                } else if (bizTaxInstance.getTaxOffice().equals(TaxOffice.ds)) {
                    if (null != _map.getLong("startDate") && null != _map.getLong("endDate")) {
                        declareTaxCategory.setStartDate(_map.getLong("startDate") / 1000);
                        declareTaxCategory.setEndDate(_map.getLong("endDate") / 1000);
                    } else {
                        declareTaxCategory.setStartDate(bizMdAccountCycle.getBeginDate());
                        declareTaxCategory.setEndDate(bizMdAccountCycle.getEndDate());
                    }
                } else {
                    declareTaxCategory.setStartDate(bizMdAccountCycle.getBeginDate());
                    declareTaxCategory.setEndDate(bizMdAccountCycle.getEndDate());
                }

                declareTaxCategory.setTaxCode(_map.getString("sn"));
                declareTaxCategory.setName(_map.getString("name"));
                if ("Y".equals(_map.getString("alreadyPay"))) {
                    declareTaxCategory.setPayment(true);
                }
                if (StringUtils.isNotEmpty(_map.getString("declareDate")) && !"null".equals(_map.getString("declareDate"))) {
                    declareTaxCategory.setDeclareDate(_map.getLong("declareDate") / 1000);
                }
                if (StringUtils.isNotEmpty(_map.getString("ybtse")) && !"null".equals(_map.getString("ybtse"))) {
                    declareTaxCategory.setRealPayAmount(new BigDecimal(_map.getString("ybtse")));
                }

                if (StringUtils.isNotEmpty(_map.getString("taxOfficePayAmount")) && !"null".equals(_map.getString("taxOfficePayAmount"))) {
                    declareTaxCategory.setTaxOfficePayAmount(new BigDecimal(_map.getString("taxOfficePayAmount")));
                }
                declareTaxCategory.setTaxPaySn(_map.getString("taxPaySn"));

                if (null != _map.getObject("data") && _map.getObject("data").size() > 0) {
                    if (StringUtils.isNotEmpty(_map.getObject("data").getString("taxclosingdate"))) {
                        declareTaxCategory.setCloseDate(DateUtils.stringDateToTimeStampSecs(_map.getObject("data").getString("taxclosingdate"), DateUtils.ZH_PATTERN_SECOND));
                    }
                    declareTaxCategory.setCurrentDataJson(_map.getObject("data").getString("current").equals("[]") ? "" : _map.getObject("data").getString("current"));
                    declareTaxCategory.setHistoryDataJson(_map.getObject("data").getString("history").equals("[]") ? "" : _map.getObject("data").getString("history"));
                    declareTaxCategory.setTaxCustomData(new HashMap() {{
                        if (_map.getObject("data").getString("current").equals("[]") || _map.getObject("data").getString("history").equals("[]")) {
                            put("message", _map.getObject("data").getString("message"));
                        }
                    }});
                    declareTaxCategory.setBranchDataJson(_map.getObject("data").getString("branch"));
                    if (StringUtils.isNotEmpty(declareTaxCategory.getCurrentDataJson())) {
                        Map<String, String> currentDataMap = declareTaxCategory.toMap(declareTaxCategory.getCurrentDataJson());
                        String BQYSHWYJ = currentDataMap.get("BQYSHWYJ");//本期应税货物预缴
                        String BQYSFWYJ = currentDataMap.get("BQYSFWYJ");//本期应税服务预缴
                        String HDZSFS = currentDataMap.get("HDZSFS");//企业所得税核定征收类型
                        if (StringUtils.isNotEmpty(BQYSHWYJ)) {
                            declareTaxCategory.setPrepayTaxCargoTotalAmount(new BigDecimal(BQYSHWYJ));
                        }
                        if (StringUtils.isNotEmpty(BQYSFWYJ)) {
                            declareTaxCategory.setPrepayTaxServiceTotalAmount(new BigDecimal(BQYSFWYJ));
                        }
                        declareTaxCategory.setBitBAuditType(HDZSFS);
                    }

                }

                //客户信息
                declareTaxCategory.setClientInfoJson(body.getString("clientInfo"));
                //征收品目明细
                if (null != _map.getArray("pmList") && _map.getArray("pmList").size() > 0) {
                    List<JsonMap> _pmList = _map.getArray("pmList");
                    List<BizTaxCompanyCategoryItem> bizTaxCompanyCategoryItemlsit = new ArrayList<>();
                    _pmList.forEach(item -> {
                        BizTaxCompanyCategoryItem bizTaxCompanyCategoryItem = new BizTaxCompanyCategoryItem();
                        bizTaxCompanyCategoryItem.setItemCode(item.getString("itemCode"));
                        bizTaxCompanyCategoryItem.setItemName(item.getString("itemName"));
                        bizTaxCompanyCategoryItem.setDetailCode(item.getString("detailCode"));
                        bizTaxCompanyCategoryItem.setCycleType(item.getString("cycleType"));
                        bizTaxCompanyCategoryItem.setTaxRate(new BigDecimal(item.getString("taxRate")));
                        bizTaxCompanyCategoryItem.setTaxStartdate(java.sql.Date.valueOf(item.getString("taxStartDate")).getTime() / 1000);
                        bizTaxCompanyCategoryItem.setTaxEnddate(java.sql.Date.valueOf(item.getString("taxEndDate")).getTime() / 1000);
                        bizTaxCompanyCategoryItemlsit.add(bizTaxCompanyCategoryItem);
                    });
                    declareTaxCategory.setBizTaxCompanyCategoryItems(bizTaxCompanyCategoryItemlsit);
                }
                result.add(declareTaxCategory);
            }
            return result;
        }

        public static List<TaxOfficeRegisterTaxCategory> registerTaxCategory(JsonMap body, BizTaxInstance bizTaxInstance) {
            List<JsonMap> _registerList = body.getArray("taxRegisterList");
            if (CollectionUtils.isEmpty(_registerList)) {
                return null;
            }
            List<TaxOfficeRegisterTaxCategory> result = new ArrayList<>();
            for (JsonMap _map : _registerList) {
                TaxOfficeRegisterTaxCategory taxOfficeRegisterTaxCategory = new TaxOfficeRegisterTaxCategory();
                taxOfficeRegisterTaxCategory.setTaxOfficeCategoryPropertyValue(_map.getString("taxCode"));
                if (StringUtils.isEmpty(_map.getString("cycleType"))) {
                    throw ServiceTaxException.build(CallbackProcessFailed, "认定税种cycleType周期为空");
                }
                taxOfficeRegisterTaxCategory.setCycleType(CycleType.valueOf(_map.getString("cycleType")));
                taxOfficeRegisterTaxCategory.setTaxOffice(bizTaxInstance.getTaxOffice());
                //征收品目明细
                if (null != _map.getArray("pmList") && _map.getArray("pmList").size() > 0) {
                    List<JsonMap> _pmList = _map.getArray("pmList");
                    List<BizTaxCompanyCategoryItem> bizTaxCompanyCategoryItemlsit = new ArrayList<>();
                    _pmList.forEach(item -> {
                        BizTaxCompanyCategoryItem bizTaxCompanyCategoryItem = new BizTaxCompanyCategoryItem();
                        bizTaxCompanyCategoryItem.setItemCode(item.getString("itemCode"));
                        bizTaxCompanyCategoryItem.setItemName(item.getString("itemName"));
                        bizTaxCompanyCategoryItem.setDetailCode(item.getString("detailCode"));
                        bizTaxCompanyCategoryItem.setCycleType(item.getString("cycleType"));
                        bizTaxCompanyCategoryItem.setTaxRate(new BigDecimal(item.getString("taxRate")));
                        bizTaxCompanyCategoryItem.setTaxStartdate(java.sql.Date.valueOf(item.getString("taxStartDate")).getTime() / 1000);
                        bizTaxCompanyCategoryItem.setTaxEnddate(java.sql.Date.valueOf(item.getString("taxEndDate")).getTime() / 1000);
                        bizTaxCompanyCategoryItemlsit.add(bizTaxCompanyCategoryItem);
                    });
                    taxOfficeRegisterTaxCategory.setBizTaxCompanyCategoryItems(bizTaxCompanyCategoryItemlsit);
                }
                result.add(taxOfficeRegisterTaxCategory);
            }
            return result;
        }


        public static CompanyTax companyTax(JsonMap body) {
            JsonMap _companyTax = body.getObject("clientinfotaxtype");
            if (null != _companyTax) {
                CompanyTax companyTax = new CompanyTax();
                if (StringUtils.isNotEmpty(_companyTax.getString("taxtype"))) {
                    companyTax.setTaxType(TaxType.valueOf(_companyTax.getString("taxtype")));
                }
                if (StringUtils.isNotEmpty(_companyTax.getString("billingtype"))) {
                    if ("labour".equals(_companyTax.getString("billingtype"))) {
                        companyTax.setBillingType(BillingType.cargo);
                    } else {
                        companyTax.setBillingType(BillingType.valueOf(_companyTax.getString("billingtype")));
                    }

                }
                return companyTax;
            }
            return null;
        }

        public static List<BizTaxInstanceCategoryPersonalPayrollItem> personalItem(List<JsonMap> _personalItem) {
            if (null != _personalItem && _personalItem.size() > 0) {
                List<BizTaxInstanceCategoryPersonalPayrollItem> personalItems = new ArrayList<>();
                for (JsonMap _map : _personalItem) {
                    BizTaxInstanceCategoryPersonalPayrollItem bizTaxInstanceCategoryPersonalPayrollItem = new BizTaxInstanceCategoryPersonalPayrollItem();
                    bizTaxInstanceCategoryPersonalPayrollItem.setAllowdeduction(new BigDecimal(_map.getString("allowdeduction")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setAlreadycosttax(new BigDecimal(_map.getString("alreadycosttax")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setCardnumber(_map.getString("cardnumber"));
                    bizTaxInstanceCategoryPersonalPayrollItem.setCardtype(_map.getString("cardtype"));
                    bizTaxInstanceCategoryPersonalPayrollItem.setCountryid(_map.getString("countryid"));
                    bizTaxInstanceCategoryPersonalPayrollItem.setDeduction(new BigDecimal(_map.getString("deduction")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setDeductionamount(new BigDecimal(_map.getString("deductionamount")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setDeductiondonate(new BigDecimal(_map.getString("deductiondonate")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setDutyfreeamount(new BigDecimal(_map.getString("dutyfreeamount")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setFinallytax(new BigDecimal(_map.getString("finallytax")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setHealthinsurance(new BigDecimal(_map.getString("healthinsurance")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setHousingfund(new BigDecimal(_map.getString("housingfund")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setInsurance(new BigDecimal(_map.getString("insurance")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setItemcode(_map.getString("itemcode"));
                    bizTaxInstanceCategoryPersonalPayrollItem.setItemname(_map.getString("itemname"));
                    bizTaxInstanceCategoryPersonalPayrollItem.setName(_map.getString("name"));
                    bizTaxInstanceCategoryPersonalPayrollItem.setAnnuity(new BigDecimal(_map.getString("nianjin")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setOriginalproperty(new BigDecimal(_map.getString("originalproperty")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setOther(new BigDecimal(_map.getString("other")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setPension(new BigDecimal(_map.getString("pension")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setRelieftax(new BigDecimal(_map.getString("relieftax")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setRemarks(_map.getString("remarks"));
                    bizTaxInstanceCategoryPersonalPayrollItem.setShouldcosttax(new BigDecimal(_map.getString("shouldcosttax")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setShouldpaytax(new BigDecimal(_map.getString("shouldpaytax")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setSpeeddeduction(new BigDecimal(_map.getString("speeddeduction")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setTaxincome(new BigDecimal(_map.getString("taxincome")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setTaxrate(new BigDecimal(_map.getString("taxrate")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setTotal(new BigDecimal(_map.getString("total")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setUnemploymentinsurance(new BigDecimal(_map.getString("unemploymentinsurance")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setWage(new BigDecimal(_map.getString("wage")));
                    bizTaxInstanceCategoryPersonalPayrollItem.setDetailcode(_map.getString("zspmGyDm"));
                    personalItems.add(bizTaxInstanceCategoryPersonalPayrollItem);
                }
                return personalItems;
            }
            return null;
        }

    }


    //解析發票
    public List<BizBillInvoiceAgentInvoiceDto> resolveInvoice(BizTaxInstance bizTaxInstance, JsonMap body) {
        //代開發票信息
        JsonMap invoiceData = body.getObject("invoicedatas");
        //文书信息
        JsonMap agentDoc = body.getObject("atricles");

        JsonMap companyTax = body.getObject("clientinfotaxtype");

        List<BizBillInvoiceAgentInvoiceDto> invoiceList = resolveInvoiceHasNotCategory(bizTaxInstance, invoiceData);

        if (CollectionUtils.isNotEmpty(invoiceList)) {
            logger.debug("开始设置发票分类信息共{}张", invoiceList.size());
            Category category = companyTaxHasCategory(companyTax);
            if (null != category) {
                logger.debug("当前客户信息中包含发票分类{}", category);
                this.settingInvoiceCategory(invoiceList, category);
            } else {
                logger.debug("客户信息不包含发票分类开始解析文书");
                List<AgentDoc> docList = resolveAgentDoc(agentDoc);
                if (CollectionUtils.isNotEmpty(docList)) {
                    category = AgentDoc.allTheSameCategory(docList);
                    if (null != category) {
                        logger.debug("当前文书存在唯一发票分类{}", category);
                        this.settingInvoiceCategory(invoiceList, category);
                    } else {
                        logger.debug("对比文书获得发票分类信息");
                        this.settingInvoiceCategoryCompareAgentDoc(invoiceList, docList);
                    }
                }

            }
        }
        return invoiceList;
    }

    /**
     * 文书中获得发票类型
     *
     * @param list
     * @param docList
     */
    protected void settingInvoiceCategoryCompareAgentDoc(List<BizBillInvoiceAgentInvoiceDto> list, List<AgentDoc> docList) {
        for (BizBillInvoiceAgentInvoiceDto t : list) {
            List<AgentDoc> _docList = AgentDoc.findByInvoice(docList, t);
            if (_docList.size() == 1) {
                for (BizBillInvoiceAgentInvoiceDto.Item item : t.getItems()) {
                    item.setCategory(_docList.get(0).getCategory());
                }
            } else if (_docList.size() > 1) {
                Category category = AgentDoc.allTheSameCategory(_docList);
                if (null != category) {
                    for (BizBillInvoiceAgentInvoiceDto.Item item : t.getItems()) {
                        item.setCategory(category);
                    }
                }
            } else {
                //nothing todo
            }
        }
    }

    /**
     * 设置发票类型
     *
     * @param list
     * @param category
     */
    protected void settingInvoiceCategory(List<BizBillInvoiceAgentInvoiceDto> list, Category category) {
        for (BizBillInvoiceAgentInvoiceDto t : list) {
            for (BizBillInvoiceAgentInvoiceDto.Item item : t.getItems()) {
                item.setCategory(category);
            }
        }
    }


    /**
     * 获得公司发票类型
     *
     * @param companyTax
     * @return
     */
    protected Category companyTaxHasCategory(JsonMap companyTax) {
        Category category = null;
        if (null != companyTax && 200 == companyTax.getInteger("code")) {
            if (companyTax.getString("billingtype").equals("service")) {
                category = Category.service;
            } else if (companyTax.getString("billingtype").equals("labour")) {
                category = Category.cargo;
            }
        }
        return category;
    }

    @Data
    public static class AgentDoc {
        private String salerBankNo;
        private String salerAddress;
        private String buyerBankNo;
        private String buyerAddress;
        private String buyerName;
        private String buyerTaxNo;
        private Category category = Category.none;

        List<Map<String, String>> items = new ArrayList<>();

        public static List<AgentDoc> findByInvoice(List<AgentDoc> agentDocs, BizBillInvoiceAgentInvoiceDto invoiceAgentInvoiceDto) {

            List<AgentDoc> _result = new ArrayList<>();
            for (AgentDoc _doc : agentDocs) {
                if (_doc.getSalerBankNo().equals(invoiceAgentInvoiceDto.getMdCompanySalerBankNo()) &&
                        _doc.getSalerAddress().equals(invoiceAgentInvoiceDto.getMdCompanySalerAddrPhone()) &&
                        _doc.getBuyerBankNo().equals(invoiceAgentInvoiceDto.getMdCompanyBuyerBankNo()) &&
                        _doc.getBuyerTaxNo().equals(invoiceAgentInvoiceDto.getMdCompanyBuyerTaxNo()) &&
                        _doc.getBuyerName().equals(invoiceAgentInvoiceDto.getMdCompanyBuyerName()) &&
                        _doc.getBuyerAddress().equals(invoiceAgentInvoiceDto.getMdCompanyBuyerAddrPhone())) {
                    //比较发票item
                    if (compareInvoiceItem(_doc.getItems(), invoiceAgentInvoiceDto.getItems())) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("当前文书{}和当前发票相同{}", JSON.toJSONString(_doc), JSON.toJSONString(invoiceAgentInvoiceDto));
                        }
                        _result.add(_doc);
                    }
                }
            }
            return _result;
        }

        private static boolean compareInvoiceItem(List<Map<String, String>> maps, List<BizBillInvoiceAgentInvoiceDto.Item> items) {
            int count = 0;
            if (logger.isDebugEnabled()) {
                logger.debug("对比 item 文书中和发票 doc{} invoice:{}", JSON.toJSONString(maps), JSON.toJSONString(items));
            }

            if (maps.size() != items.size()) {
                return false;
            }
            for (BizBillInvoiceAgentInvoiceDto.Item item : items) {
                String itemSn = item.getItemSn() != null ? item.getItemSn() : "";
                String itemName = item.getItemName() != null ? item.getItemName() : "";

                for (Map<String, String> t : maps) {
                    if (new BigDecimal(t.get("amount")).compareTo(item.getFullamount()) == 0 &&
                            new BigDecimal(t.get("itemNum")).compareTo(item.getItemNum()) == 0 &&
                            t.get("itemName").equals(itemName) &&
                            t.get("itemSn").equals(itemSn)) {
                        count = +1;
                        break;
                    }
                }
            }
            return count == items.size();
        }


        public static Category allTheSameCategory(List<AgentDoc> list) {
            Set<Category> _category = new HashSet<>();
            if (CollectionUtils.isEmpty(list)) {
                return null;
            }

            list.stream().forEach(v -> _category.add(v.getCategory()));
            if (_category.size() == 1) {
                return _category.stream().findFirst().get();
            }
            return null;
        }
    }

    /**
     * 解析文书
     *
     * @param agentDoc
     * @return
     */
    public List<AgentDoc> resolveAgentDoc(JsonMap agentDoc) {
        if (!agentDoc.getString("success").equals("true")) {
            return null;
        }
        List<AgentDoc> list = new ArrayList<>();
        for (JsonMap _map : agentDoc.getArray("shenzhenArticleDetails")) {
            JsonMap shenzhenBuyerSallerRequest = _map.getObject("shenzhenBuyerSallerRequest");
            List<JsonMap> shenzhenShouldPayTaxRequest = _map.getArray("shenzhenShouldPayTaxRequest");
            AgentDoc _doc = new AgentDoc();
            _doc.setSalerAddress(shenzhenBuyerSallerRequest.getString("xfdz", ""));
            _doc.setSalerBankNo(shenzhenBuyerSallerRequest.getString("xfyhzh", ""));
            _doc.setBuyerBankNo(shenzhenBuyerSallerRequest.getString("gfyhzh", ""));
            _doc.setBuyerAddress(shenzhenBuyerSallerRequest.getString("gfdz", ""));
            _doc.setBuyerName(shenzhenBuyerSallerRequest.getString("gfnsrmc", ""));
            _doc.setBuyerTaxNo(shenzhenBuyerSallerRequest.getString("gfnsrsbh", ""));

            for (JsonMap vat : shenzhenShouldPayTaxRequest) {
                if ("增值税".equals(vat.getString("zsxmMc")) || "10101".equals(vat.getString("zsxmDm"))) {
                    String zspmDm = vat.getString("zspmDm");
                    //根据itemcode判断，如果前六位 为："101016" || "101017"，表示服务类，其他为劳务类
                    if (StringUtils.isNotEmpty(zspmDm) && zspmDm.length() > 6) {
                        if ("101017".equals(zspmDm.substring(0, 6)) || "101016".equals(zspmDm.substring(0, 6))) {
                            _doc.setCategory(Category.service);
                        } else {
                            _doc.setCategory(Category.cargo);
                        }

                    }
                }
            }
            //解析具体商品信息
            List<JsonMap> shenzhenInvoiceDetailRequest = _map.getArray("shenzhenInvoiceDetailRequest");

            for (JsonMap item : shenzhenInvoiceDetailRequest) {
                Map<String, String> itemMap = new HashMap<>();
                if (StringUtils.isNotEmpty(item.getString("jsxj"))) {
                    itemMap.put("amount", item.getString("jsxj", "0.00"));
                } else if (StringUtils.isNotEmpty(item.getString("je"))) {
                    itemMap.put("amount", item.getString("je", "0.00"));
                } else {
                    itemMap.put("amount", "0.00");//不含税金额
                }
                itemMap.put("itemName", item.getString("hwlwmc", ""));//hwlwmc项目名称
                itemMap.put("itemNum", item.getString("hlsl", "0.00"));//项目数量hlsl
                itemMap.put("itemSn", item.getString("dwslDm", ""));//单位数量代码
                _doc.getItems().add(itemMap);
            }
            list.add(_doc);
        }
        return list;
    }

    /**
     * 解析发票
     *
     * @param billInvoice
     * @return
     */
    protected List<BizBillInvoiceAgentInvoiceDto> resolveInvoiceHasNotCategory(BizTaxInstance bizTaxInstance, JsonMap billInvoice) {
        logger.debug("开始解析发票");
        if (!"200".equals(billInvoice.getString("code"))) {
            return null;
        }
        List<BizBillInvoiceAgentInvoiceDto> result = new ArrayList<>();
        List<JsonMap> list = billInvoice.getArray("data");
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        for (JsonMap map : list) {

            if (CollectionUtils.isEmpty(map.getArray("detail"))) {
                return null;
            }
            JsonMap _detail = map.getArray("detail").get(0);
            JsonMap _main = map.getObject("main");
            BizBillInvoiceAgentInvoiceDto bizBillInvoice = new BizBillInvoiceAgentInvoiceDto();
            bizBillInvoice.setMdCompanyId(bizTaxInstance.getMdCompanyId()); //todo
            String kprq = _detail.getObject("zzsdkfpkjxxVo").getString("kjrq"); //2017-12-27
            //会计期间使用开票日期
            BizMdAccountCycle bizMdAccountCycle = bizMdAccountCycleService.findBySnAndType(kprq.replaceAll("-", "").substring(0, 6), com.yun9.biz.md.enums.CycleType.m);
            bizBillInvoice.setMdAccountCycleId(bizMdAccountCycle.getId());
            bizBillInvoice.setBillingDate(DateUtils.unixTime(DateUtils.stringToLocalDate(kprq))); //开票日期
            if (StringUtils.isNotEmpty(_main.getString("zfrq"))) {
                bizBillInvoice.setIssuedDate(DateUtils.unixTime(DateUtils.stringToLocalDate(_main.getString("zfrq"))));//领取日期
            }


            //购方公司信息
            if (StringUtils.isNotEmpty(_detail.getObject("zzsdkfpkjxxVo").getString("ghfnsrsbh")) && StringUtils.isNotEmpty(_detail.getObject("zzsdkfpkjxxVo").getString("ghfnsrmc"))) {
//                BizMdCompany buyCompany = bizMdCompanyService.createByTaxNo(_detail.getObject("zzsdkfpkjxxVo").getString("ghfnsrsbh"), new HashMap() {{
//                    put("fullName", _detail.getObject("zzsdkfpkjxxVo").getString("ghfnsrmc"));
//                    put("name", _detail.getObject("zzsdkfpkjxxVo").getString("ghfnsrmc"));
//                    put("taxType", com.yun9.biz.md.enums.TaxType.none);
//                }});

                //TODO 查数据库。有税号重复的公司
                BizMdCompany buyCompany = bizMdCompanyService.createByFullName(_detail.getObject("zzsdkfpkjxxVo").getString("ghfnsrmc"), new HashMap() {{
                    put("name", _detail.getObject("zzsdkfpkjxxVo").getString("ghfnsrmc"));
                    put("taxType", com.yun9.biz.md.enums.TaxType.none);
                    put("taxNo", _detail.getObject("zzsdkfpkjxxVo").getString("ghfnsrsbh"));
                }});

                if (null != buyCompany) {
                    bizBillInvoice.setMdCompanyBuyerId(buyCompany.getId());
                }

            }
            bizBillInvoice.setMdCompanyBuyerName(_detail.getObject("zzsdkfpkjxxVo").getString("ghfnsrmc"));
            bizBillInvoice.setMdCompanyBuyerTaxNo(_detail.getObject("zzsdkfpkjxxVo").getString("ghfnsrsbh"));
            bizBillInvoice.setMdCompanyBuyerAddrPhone(_detail.getObject("zzsdkfpkjxxVo").getString("ghfdz"));
            bizBillInvoice.setMdCompanyBuyerBankNo(_detail.getObject("zzsdkfpkjxxVo").getString("ghfyhzh"));
            bizBillInvoice.setMdCompanyBuyerBankName(_detail.getObject("zzsdkfpkjxxVo").getString("ghfkhyhmc")); //
            //销方公司信息
            bizBillInvoice.setMdCompanySalerId(bizTaxInstance.getMdCompanyId());//todo 当前公司信息
            bizBillInvoice.setMdCompanySalerName(_detail.getObject("zzsdkfpkjxxVo").getString("xhfnsrmc"));
            bizBillInvoice.setMdCompanySalerAddrPhone(_detail.getObject("zzsdkfpkjxxVo").getString("xhfdz"));
            bizBillInvoice.setMdCompanySalerTaxNo(_detail.getObject("zzsdkfpkjxxVo").getString("xhfdjxh"));//税号
            bizBillInvoice.setMdCompanySalerBankNo(_detail.getObject("zzsdkfpkjxxVo").getString("xhfyhzh")); //银行账户44201002400052515942


            bizBillInvoice.setBatchSn(_detail.getObject("zzsdkfpkjxxVo").getString("fpDm"));//发票代码
            bizBillInvoice.setBillSn(_detail.getObject("zzsdkfpkjxxVo").getString("fphm"));//发票号码
            bizBillInvoice.setBillType(BizBillInvoice.BillType.agent);

            bizBillInvoice.setType(_main.getString("dkfplbDm").equals("09") ? BizBillInvoice.Type.plain :
                    (_main.getString("dkfplbDm").equals("01") ? BizBillInvoice.Type.special : BizBillInvoice.Type.none));
            bizBillInvoice.setSource(BizBillInvoice.Source.taxoffice);
            bizBillInvoice.setState(_main.getString("zfbz1").equals("N") ?
                    (_detail.getObject("zzsdkfpkjxxVo").getBigDecimal("dksqje").compareTo(new BigDecimal("0.00")) == -1 ? BizBillInvoice.State.red : BizBillInvoice.State.normal) :
                    (_main.getString("zfbz1").equals("Y") ? BizBillInvoice.State.disabled : BizBillInvoice.State.none)
            );
            bizBillInvoice.setFullamount(_detail.getObject("zzsdkfpkjxxVo").getBigDecimal("kpje"));//含税金额
            bizBillInvoice.setAmount(_detail.getObject("zzsdkfpkjxxVo").getBigDecimal("dksqje"));//不含税金额
            bizBillInvoice.setTaxAmount(bizBillInvoice.getFullamount().subtract(bizBillInvoice.getAmount()));//税额
            //   bizBillInvoice.setDeclareAmount(_detail.getObject("zzsdkfpkjxxVo").getBigDecimal("dksqje"));// 申报金额
            bizBillInvoice.setTaxRemit(_detail.getObject("zzsdkfpkjxxVo").getString("jmsbz").equals("N") ? 0 : 1);// 减免标志
            bizBillInvoice.setPrepaid(_detail.getObject("zzsdkfpkjxxVo").getBigDecimal("ybsfe"));// 预交
            //  bizBillInvoice.setDeclarePrepaid(_detail.getObject("zzsdkfpkjxxVo").getBigDecimal("ybsfe"));// 申报预缴

            bizBillInvoice.setItems(new ArrayList<>());
            for (JsonMap _item : _detail.getArray("zzsdkfpHlmxVos")) {
                BizBillInvoiceAgentInvoiceDto.Item item = new BizBillInvoiceAgentInvoiceDto.Item();
                item.setCategory(Category.none);
                item.setItemSn(_item.getString("dwslDm"));
                item.setItemNum(_item.getBigDecimal("hlsl"));
                item.setItemName(_item.getString("hwlwmc"));
                item.setSpec(_item.getString("dwslmc"));//规格
                item.setUtil(_item.getString("dwslmc"));//单位
                item.setAmount(_item.getBigDecimal("je")); //不含税金额
                item.setTaxAmount(_item.getBigDecimal("se"));//税额
                item.setFullamount(item.getAmount().add(item.getTaxAmount())); //含税金额
                item.setDeclareamount(_item.getBigDecimal("se"));//申报金额
                item.setTaxRate(_item.getBigDecimal("sl"));//税率
                bizBillInvoice.getItems().add(item);
            }
            result.add(bizBillInvoice);

        }
        return result;
    }
}
