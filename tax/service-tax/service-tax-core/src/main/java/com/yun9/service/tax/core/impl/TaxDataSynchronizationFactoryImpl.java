package com.yun9.service.tax.core.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yun9.biz.bill.domain.entity.BizBillAgentSummary;
import com.yun9.biz.bill.domain.entity.BizBillInvoice;
import com.yun9.biz.md.*;
import com.yun9.biz.md.domain.dto.InstClientDetailDTO;
import com.yun9.biz.md.domain.entity.*;
import com.yun9.biz.md.enums.CycleType;
import com.yun9.biz.md.enums.TaxType;
import com.yun9.biz.md.exception.BizMdException;
import com.yun9.biz.tax.*;
import com.yun9.biz.tax.domain.entity.*;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.DateUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.*;
import com.yun9.service.tax.core.dto.*;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaxDataSynchronizationFactoryImpl implements TaxDataSynchronizationFactory {

    public static final Logger logger = LoggerFactory.getLogger(TaxDataSynchronizationFactoryImpl.class);

    private static final Map<String, String> personalitemcodehash = new ConcurrentHashMap<>();
    private static final Map<String, String> countryareahash = new ConcurrentHashMap<>();
    private static final Map<String, String> cardclasshash = new ConcurrentHashMap<>();

    private static final Map<String, String> personalitemcodehashNew = new ConcurrentHashMap<>();
    private static final Map<String, String> countryareahashNew = new ConcurrentHashMap<>();
    private static final Map<String, String> cardclasshashNew = new ConcurrentHashMap<>();

    @Autowired
    private BizMdInstService bizMdInstService;

    @Autowired
    private BizMdCompanyService bizMdCompanyService;

    @Autowired
    private BizMdInstClientService bizMdInstClientService;

    @Autowired
    private BizMdAccountCycleService bizMdAccountCycleService;

    @Autowired
    private BizTaxInstanceService bizTaxInstanceService;

    @Autowired
    private BizTaxInstanceCategoryService bizTaxInstanceCategoryService;

    @Autowired
    private BizTaxMdCategoryService bizTaxMdCategoryService;

    @Autowired
    private BizTaxInstanceCategoryBitService bizTaxInstanceCategoryBitService;

    @Autowired
    private TaxInstanceCategoryVatQFactory taxInstanceCategoryVatQFactory;

    @Autowired
    private TaxInstanceCategoryBitFactory taxInstanceCategoryBitFactory;

    @Autowired
    private TaxInstanceCategoryPersonalPayrollItemFactory taxInstanceCategoryPersonalPayrollItemFactory;

    @Autowired
    private BizTaxInstanceCategoryPersonalPayrollService bizTaxInstanceCategoryPersonalPayrollService;

    @Autowired
    private BizTaxInstanceCategoryFzService bizTaxInstanceCategoryFzService;

    @Autowired
    private BizTaxInstanceCategoryFzItemService bizTaxInstanceCategoryFzItemService;

    @Autowired
    private TaxInstanceCategoryFzFactory taxInstanceCategoryFzFactory;

    @Autowired
    private BizMdDictionaryCodeService bizMdDictionaryCodeService;

    @Autowired
    private BizTaxInstanceCategoryPersonalPayrollItemService bizTaxInstanceCategoryPersonalPayrollItemService;

    @Autowired
    private TaxInstanceCategoryPersonalMFactory taxInstanceCategoryPersonalMFactory;

    @Autowired
    private TaxNoticeInstFactory taxNoticeInstFactory;

    @Autowired
    private ServiceTaxThreadPoolContainer serviceTaxThreadPoolContainer;

    @Autowired
    private BizTaxInstanceCategoryVatSmallService bizTaxInstanceCategoryVatSmallService;


    @Override
    public List<TaxDataSyncResultDTO> instUpdateData(TaxDataSyncDTO taxDataSyncDTO, long userId) {
        if (null == taxDataSyncDTO.getData()) {
            ServiceTaxException.throwException(ServiceTaxException.Codes.IllegalArgumentException, "data");
        }
        logger.info("机构导入税务数据：{}", JSONObject.toJSONString(taxDataSyncDTO));
        BizMdInst mdInst = null;
        if (taxDataSyncDTO.isFromOld()) {  // 来自老接口
            mdInst = bizMdInstService.findByFtId(taxDataSyncDTO.getInstid());
        } else {
            mdInst = bizMdInstService.findById(Long.valueOf(taxDataSyncDTO.getInstid()));
        }
        if (null == mdInst) {
            BizMdException.throwException(BizMdException.Codes.BizMdInstNotFound, taxDataSyncDTO.getInstid());
        }

        BizMdCompany bizMdCompany = null;
        if (StringUtils.isNotEmpty(taxDataSyncDTO.getFtId())) {
            bizMdCompany = bizMdCompanyService.findByFtId(taxDataSyncDTO.getFtId());
        } else {
            bizMdCompany = bizMdCompanyService.findByTaxNo(taxDataSyncDTO.getTaxno());
        }
        if (null == bizMdCompany) {
            BizMdException.throwException(BizMdException.Codes.BizMdCompanyNotFound, taxDataSyncDTO.getTaxno());
        }
        InstClientDetailDTO instClientDetailDTO = bizMdInstClientService.findByInstClientId(mdInst.getId(), bizMdCompany.getId());
        if (null == instClientDetailDTO) {
            BizMdException.throwException(BizMdException.Codes.BizMdInstClientNotFound, mdInst.getName(), bizMdCompany.getFullName());
        }
        final List<TaxDataSyncResultDTO> taxDataSyncResultDTOS = new ArrayList<>();
        taxDataSyncDTO.getData().keySet().forEach((key) -> {
            try {
                // 增值税数据
                if ("vat".equals(key)) {
                    handlerVat(taxDataSyncDTO,
                            instClientDetailDTO, taxDataSyncDTO.getData().getJSONObject(key), userId);
                }
                // 企业所得税数据
                if ("bitq".equals(key)) {
                    handlerBitq(taxDataSyncDTO,
                            instClientDetailDTO, taxDataSyncDTO.getData().getJSONObject(key), userId);
                }
                // 个税数据
                if ("personaltax".equals(key)) {
                    handlerPersonaltax(taxDataSyncDTO,
                            instClientDetailDTO, taxDataSyncDTO.getData().getJSONObject(key), userId);
                }
                // 附征税数据
                if ("fztax".equals(key)) {
                    handlerFztax(taxDataSyncDTO,
                            instClientDetailDTO, taxDataSyncDTO.getData().getJSONObject(key), userId);
                }
                taxDataSyncResultDTOS.add(new TaxDataSyncResultDTO() {{
                    this.setType(key);
                    this.setSuccess(true);
                    this.setMessage("导入成功" + key);
                }});
            } catch (Exception ex) {
                logger.error("机构数据导入失败", ex);
                taxDataSyncResultDTOS.add(new TaxDataSyncResultDTO() {{
                    this.setType(key);
                    this.setSuccess(false);
                    this.setMessage(ex.getMessage());
                }});
            }
        });

        return taxDataSyncResultDTOS;
    }

    @Override
    public Object instResetUploadState(TaxDataUnAuditDTO taxDataUnAuditDTO, long userId) {
        logger.info("机构撤销审核：{}", JSONObject.toJSONString(taxDataUnAuditDTO));
        BizMdInst mdInst = null;
        if (taxDataUnAuditDTO.isFromOld()) {  // 来自老接口
            mdInst = bizMdInstService.findByFtId(taxDataUnAuditDTO.getInstid());
        } else {
            mdInst = bizMdInstService.findById(Long.valueOf(taxDataUnAuditDTO.getInstid()));
        }
        if (null == mdInst) {
            BizMdException.throwException(BizMdException.Codes.BizMdInstNotFound, taxDataUnAuditDTO.getInstid());
        }
        BizMdCompany bizMdCompany = null;
        if (StringUtils.isNotEmpty(taxDataUnAuditDTO.getFtId())) {
            bizMdCompany = bizMdCompanyService.findByFtId(taxDataUnAuditDTO.getFtId());
        } else {
            bizMdCompany = bizMdCompanyService.findByTaxNo(taxDataUnAuditDTO.getTaxno());
        }
        if (null == bizMdCompany) {
            BizMdException.throwException(BizMdException.Codes.BizMdCompanyNotFound, taxDataUnAuditDTO.getTaxno());
        }
        InstClientDetailDTO instClientDetailDTO = bizMdInstClientService.findByInstClientId(mdInst.getId(), bizMdCompany.getId());
        if (null == instClientDetailDTO) {
            BizMdException.throwException(BizMdException.Codes.BizMdInstClientNotFound, mdInst.getName(), bizMdCompany.getFullName());
        }
        BizTaxInstanceCategory bizTaxInstanceCategory = null;
        if (TaxDataUnAuditDTO.TaxType.personaltax.name().equals(taxDataUnAuditDTO.getTaxtype())) {
            bizTaxInstanceCategory = findBizTaxInstanceCategory(instClientDetailDTO, taxDataUnAuditDTO.getAccountcyclesn(), new ArrayList<TaxSn>() {{
                this.add(TaxSn.m_personal_payroll);
            }}, TaxOffice.ds);
        } else if (TaxDataUnAuditDTO.TaxType.bit.name().equals(taxDataUnAuditDTO.getTaxtype())) {
            bizTaxInstanceCategory = findBizTaxInstanceCategory(instClientDetailDTO, taxDataUnAuditDTO.getAccountcyclesn(), new ArrayList<TaxSn>() {{
                this.add(TaxSn.q_bit);
            }});
        } else if (TaxDataUnAuditDTO.TaxType.frq.name().equals(taxDataUnAuditDTO.getTaxtype())) {
            bizTaxInstanceCategory = findBizTaxInstanceCategory(instClientDetailDTO, taxDataUnAuditDTO.getAccountcyclesn(), new ArrayList<TaxSn>() {{
                this.add(TaxSn.q_fr);
                this.add(TaxSn.y_fr);
            }});
        } else if (TaxDataUnAuditDTO.TaxType.vat.name().equals(taxDataUnAuditDTO.getTaxtype())) {
            bizTaxInstanceCategory = findBizTaxInstanceCategory(instClientDetailDTO, taxDataUnAuditDTO.getAccountcyclesn(), new ArrayList<TaxSn>() {{
                this.add(TaxSn.q_vat);
            }}, TaxOffice.gs);
        }
        if (null != bizTaxInstanceCategory) {
            bizTaxInstanceCategoryService.cancelOfAudit(bizTaxInstanceCategory.getId(), userId);
        }
        return null;
    }

    @Override
    public Object instUpdateClients(TaxDataUpdateClientDTO taxDataUpdateClientDTO, long userId) {
        if (null == taxDataUpdateClientDTO.getClients() || taxDataUpdateClientDTO.getClients().size() == 0) {
            BizTaxException.throwException(BizTaxException.Codes.IllegalArgumentError, "clients");
        }
        logger.info("机构更新客户信息：{}", JSONObject.toJSONString(taxDataUpdateClientDTO));
        BizMdInst mdInst = null;
        if (taxDataUpdateClientDTO.isFromOld()) {  // 来自老接口
            mdInst = bizMdInstService.findByFtId(taxDataUpdateClientDTO.getInstid());
        } else {
            mdInst = bizMdInstService.findById(Long.valueOf(taxDataUpdateClientDTO.getInstid()));
        }
        if (null == mdInst) {
            BizMdException.throwException(BizMdException.Codes.BizMdInstNotFound, taxDataUpdateClientDTO.getInstid());
        }
        List<TaxDataSyncResultDTO> taxDataSyncResultDTOS = new ArrayList<>();
        for (int i = 0; i < taxDataUpdateClientDTO.getClients().size(); i++) {
            JSONObject jsonObject = taxDataUpdateClientDTO.getClients().getJSONObject(i);
            BizMdCompany bizMdCompany = null;
            try {
                if ("taxno".equals(taxDataUpdateClientDTO.getSelecttype())) {
                    bizMdCompany = bizMdCompanyService.findByTaxNo(jsonObject.getString("taxno"));
                } else if ("name".equals(taxDataUpdateClientDTO.getSelecttype())) {
                    bizMdCompany = bizMdCompanyService.findByFullName(jsonObject.getString("name"));
                }
                if (null == bizMdCompany) {
                    taxDataSyncResultDTOS.add(new TaxDataSyncResultDTO() {{
                        this.setSuccess(false);
                        this.setMessage("无法找到客户：" + jsonObject.toJSONString());
                    }});
                    continue;
                }
                setupCompany(bizMdCompany, jsonObject);
                taxDataSyncResultDTOS.add(new TaxDataSyncResultDTO() {{
                    this.setSuccess(true);
                    this.setMessage("执行成功");
                }});
            } catch (Exception ex) {
                taxDataSyncResultDTOS.add(new TaxDataSyncResultDTO() {{
                    this.setSuccess(false);
                    this.setMessage(ex.getMessage());
                }});
            }
        }

        return taxDataSyncResultDTOS;
    }

    @Override
    public Object noticeInst(TaxDataNoticeInstDTO taxDataNoticeInstDTO, long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "200");
        result.put("nums", 0);
        result.put("message", "发起成功");
        BizMdInst mdInst = bizMdInstService.findById(taxDataNoticeInstDTO.getInitId());
        List<BizMdInstClient> bizMdInstClients = null;
        if (null == taxDataNoticeInstDTO.getCompanyId()) {
            bizMdInstClients = bizMdInstClientService.findByInstId(mdInst.getId());
        } else {
            BizMdInstClient mdInstClient = bizMdInstClientService.findByCompanyIdAndInstId(taxDataNoticeInstDTO.getCompanyId(),
                    mdInst.getId());
            if (null != mdInstClient) {
                bizMdInstClients = new ArrayList<>();
                bizMdInstClients.add(mdInstClient);
            }
        }
        if (CollectionUtils.isEmpty(bizMdInstClients)) {
            result.put("status", "500");
            result.put("nums", "0");
            result.put("message", "发起失败，未找到客户");
            return result;
        }
        bizMdInstClients.stream().forEach((bizMdInstClient) -> {
            BizTaxInstance bizTaxInstance = bizTaxInstanceService
                    .findByMdInstClientIdAndMdCompanyIdAndMdAccountCycleIdAndTaxOfficeAndDisabled(bizMdInstClient.getId(),
                            bizMdInstClient.getBizMdCompanyId(), taxDataNoticeInstDTO.getMdAccountCycleId(),
                            TaxOffice.valueOf(taxDataNoticeInstDTO.getTaxOffice()));
            if (null != bizTaxInstance) {
                List<BizTaxInstanceCategory> bizTaxInstanceCategories =
                        bizTaxInstanceCategoryService.findByBizTaxInstanceId(bizTaxInstance.getId());
                if (CollectionUtils.isNotEmpty(bizTaxInstanceCategories)) {
                    bizTaxInstanceCategories.stream().forEach((bizTaxInstanceCategory) -> {
                        serviceTaxThreadPoolContainer.getMultTaskThreadPool().execute(new Runnable() {
                            @Override
                            public void run() {
                                taxNoticeInstFactory.noticeByBizTaxInstanceCategoryId(bizTaxInstanceCategory.getId());
                            }
                        });
                        logger.info("发起申报信息通知："+bizTaxInstanceCategory.getId());
                        result.put("nums",Integer.valueOf(result.get("nums").toString()) +1);
                    });
                } else {
                    logger.info("通知申报结果，未找到InstanceCategory记录,instance_id："+bizTaxInstance.getId());
                }
            } else {
                logger.info("通知申报结果，未找到Instance记录,inst_client_id："+bizMdInstClient.getId());
            }
        });

        return result;
    }

    private void setupCompany(BizMdCompany bizMdCompany, JSONObject jsonObject) {
        if (jsonObject.containsKey("taxno")) {
            bizMdCompany.setTaxNo(jsonObject.getString("taxno"));
        }
        if (jsonObject.containsKey("name")) {
            bizMdCompany.setFullName(jsonObject.getString("name"));
        }
        if (jsonObject.containsKey("boxno")) {
        }
        if (jsonObject.containsKey("sn")) {
        }
        if (jsonObject.containsKey("taxtype")) {
            bizMdCompany.setTaxType(TaxType.valueOf(jsonObject.getString("taxtype")));
        }
        if (jsonObject.containsKey("taxarea")) {

        }
        if (jsonObject.containsKey("gspassword")) {

        }
        if (jsonObject.containsKey("dspassword")) {

        }
    }

    /*--------处理增值税导入*/
    private void handlerVat(TaxDataSyncDTO taxDataSyncDTO, InstClientDetailDTO instClientDetailDTO, JSONObject vatData, long userId) {
        if (!vatData.containsKey("bill")) {
            BizTaxException.throwException(BizTaxException.Codes.IllegalArgumentError, "bill");
        }
        JSONObject billData = vatData.getJSONObject("bill");
        BizMdAccountCycle bizMdAccountCycle = bizMdAccountCycleService
                .findBySnAndType(taxDataSyncDTO.getAccountcyclesn(), CycleType.q);
        BizTaxInstance bizTaxInstance = bizTaxInstanceService
                .findByMdInstClientIdAndMdCompanyIdAndMdAccountCycleIdAndTaxOfficeAndDisabled(
                        instClientDetailDTO.getInstClientId(), instClientDetailDTO.getCompanyId(),
                        bizMdAccountCycle.getId(), TaxOffice.gs);
        if (null == bizTaxInstance) {
            BizTaxException.throwException(BizTaxException.Codes.TaxInstanceNotFound);
        }
        BizTaxMdCategory bizTaxMdCategory = bizTaxMdCategoryService.findBySn(TaxSn.q_vat);
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService.findByInstanceAndTaxMdCategoryId(bizTaxInstance.getId(),
                bizTaxMdCategory.getId());
        if (null == bizTaxInstanceCategory) {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxMdCategoryNotFound);
        }
        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatSmall = bizTaxInstanceCategoryVatSmallService
                .findByBizTaxInstanceCategoryId(bizTaxInstanceCategory.getId());
        if (null == bizTaxInstanceCategoryVatSmall) {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxMdCategoryNotFound);
        }

        BizMdAccountCycle bizMdAccountCycleMonth = bizMdAccountCycleService
                .findBySnAndType(taxDataSyncDTO.getAccountcyclesn(), CycleType.m);
        final List<BillDTO> billDTOs = new ArrayList<>();
        billData.keySet().forEach((key) -> {
            // 劳务不开票金额
            if ("labournobill".equals(key)) {
                billDTOs.add(new BillDTO() {{
                    this.setType(BizBillInvoice.Type.plain.getValue());
                    this.setCycleId(bizMdAccountCycleMonth.getId());
                    this.setTaxRate(new BigDecimal("0.03"));
                    this.setDeclareAmount(new BigDecimal(billData.getDouble(key)));
                    this.setBillType(BizBillInvoice.BillType.nobill.getValue());
                    this.setCategory(BizBillAgentSummary.BillingType.cargo.name());
                }});
            }
            //  服务不开票金额0.03
            if ("servicenobillthree".equals(key)) {
                billDTOs.add(new BillDTO() {{
                    this.setType(BizBillInvoice.Type.plain.getValue());
                    this.setCycleId(bizMdAccountCycleMonth.getId());
                    this.setTaxRate(new BigDecimal("0.03"));
                    this.setDeclareAmount(new BigDecimal(billData.getDouble(key)));
                    this.setBillType(BizBillInvoice.BillType.nobill.getValue());
                    this.setCategory(BizBillAgentSummary.BillingType.service.name());
                }});
            }
            // 服务不开票金额0.05
            if ("servicenobillfive".equals(key)) {
                billDTOs.add(new BillDTO() {{
                    this.setType(BizBillInvoice.Type.plain.getValue());
                    this.setCycleId(bizMdAccountCycleMonth.getId());
                    this.setTaxRate(new BigDecimal("0.05"));
                    this.setDeclareAmount(new BigDecimal(billData.getDouble(key)));
                    this.setBillType(BizBillInvoice.BillType.nobill.getValue());
                    this.setCategory(BizBillAgentSummary.BillingType.service.name());
                }});
            }
            // 劳务自开免税金额
            if ("selflabournotax".equals(key)) {
                billDTOs.add(new BillDTO() {{
                    this.setType(BizBillInvoice.Type.plain.getValue());
                    this.setCycleId(bizMdAccountCycleMonth.getId());
                    this.setTaxRate(new BigDecimal("0.00"));
                    this.setDeclareAmount(new BigDecimal(billData.getDouble(key)));
                    this.setBillType(BizBillInvoice.BillType.output.getValue());
                    this.setCategory(BizBillAgentSummary.BillingType.cargo.name());
                }});
            }
            // 劳务自开0.03
            if ("selflabourthree".equals(key)) {
                billDTOs.add(new BillDTO() {{
                    this.setType(BizBillInvoice.Type.plain.getValue());
                    this.setCycleId(bizMdAccountCycleMonth.getId());
                    this.setTaxRate(new BigDecimal("0.03"));
                    this.setDeclareAmount(new BigDecimal(billData.getDouble(key)));
                    this.setBillType(BizBillInvoice.BillType.output.getValue());
                    this.setCategory(BizBillAgentSummary.BillingType.cargo.name());
                }});
            }
            // 服务自开免税
            if ("selfservicenotax".equals(key)) {
                billDTOs.add(new BillDTO() {{
                    this.setType(BizBillInvoice.Type.plain.getValue());
                    this.setCycleId(bizMdAccountCycleMonth.getId());
                    this.setTaxRate(new BigDecimal("0.00"));
                    this.setDeclareAmount(new BigDecimal(billData.getDouble(key)));
                    this.setBillType(BizBillInvoice.BillType.output.getValue());
                    this.setCategory(BizBillAgentSummary.BillingType.service.name());
                }});
            }
            // 服务自开0.03
            if ("selfservicethree".equals(key)) {
                billDTOs.add(new BillDTO() {{
                    this.setType(BizBillInvoice.Type.plain.getValue());
                    this.setCycleId(bizMdAccountCycleMonth.getId());
                    this.setTaxRate(new BigDecimal("0.03"));
                    this.setDeclareAmount(new BigDecimal(billData.getDouble(key)));
                    this.setBillType(BizBillInvoice.BillType.output.getValue());
                    this.setCategory(BizBillAgentSummary.BillingType.service.name());
                }});
            }
            // 服务自开0.05
            if ("selfservicefive".equals(key)) {
                billDTOs.add(new BillDTO() {{
                    this.setType(BizBillInvoice.Type.plain.getValue());
                    this.setCycleId(bizMdAccountCycleMonth.getId());
                    this.setTaxRate(new BigDecimal("0.05"));
                    this.setDeclareAmount(new BigDecimal(billData.getDouble(key)));
                    this.setBillType(BizBillInvoice.BillType.output.getValue());
                    this.setCategory(BizBillAgentSummary.BillingType.service.name());
                }});
            }
        });
        taxInstanceCategoryVatQFactory.resetBill(billDTOs, bizMdAccountCycleMonth, bizTaxInstanceCategoryVatSmall.getId(), userId, true);

    }

    /*--------处理企业所得导入*/
    private void handlerBitq(TaxDataSyncDTO taxDataSyncDTO, InstClientDetailDTO instClientDetailDTO, JSONObject bitData, long userId) {
        if (!bitData.containsKey("profit")) {
            BizTaxException.throwException(BizTaxException.Codes.IllegalArgumentError, "profit");
        }
        JSONObject profitData = bitData.getJSONObject("profit");
        BizTaxInstanceCategory bizTaxInstanceCategory = findBizTaxInstanceCategory(instClientDetailDTO, taxDataSyncDTO.getAccountcyclesn(), new ArrayList<TaxSn>() {{
            this.add(TaxSn.q_bit);
        }});
        if (null == bizTaxInstanceCategory) {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxMdCategoryNotFound);
        }
        BizTaxInstanceCategoryBit bizTaxInstanceCategoryBit = bizTaxInstanceCategoryBitService
                .findByTaxInstanceCategoryId(bizTaxInstanceCategory.getId());
        if (null == bizTaxInstanceCategoryBit) {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxMdCategoryNotFound);
        }
        profitData.keySet().forEach((key) -> {
            if ("income".equals(key)) {
                bizTaxInstanceCategoryBit.setSaleAmount(profitData.getBigDecimal(key));
            }
            if ("cost".equals(key)) {
                bizTaxInstanceCategoryBit.setBuyAmount(profitData.getBigDecimal(key));
            }
            if ("profit".equals(key)) {
                bizTaxInstanceCategoryBit.setProfitAmount(profitData.getBigDecimal(key));
            }
            if ("employeeNumber".equals(key)) {
                bizTaxInstanceCategoryBit.setEmployeeNumber(profitData.getInteger(key));
            }
            if ("technologySmallCompany".equals(key)) {
                bizTaxInstanceCategoryBit.setTechnologySmallCompany(profitData.getString(key));
            }
            if ("highTechnologyCompany".equals(key)) {
                bizTaxInstanceCategoryBit.setHighTechnologyCompany(profitData.getString(key));
            }
            if ("technologyAdmissionMatter".equals(key)) {
                bizTaxInstanceCategoryBit.setTechnologyAdmissionMatter(profitData.getString(key));
            }
        });
        try {
            taxInstanceCategoryBitFactory.profitAccounting(bizTaxInstanceCategoryBit, userId, "机构接口修改");
        } catch (IllegalAccessException e) {
            ServiceTaxException.throwException(ServiceTaxException.Codes.IllegalArgumentException, e.getMessage());
        }

    }

    private void handlerPersonaltax(TaxDataSyncDTO taxDataSyncDTO, InstClientDetailDTO instClientDetailDTO, JSONObject personData, long userId) {
        if (!personData.containsKey("items")) {
            BizTaxException.throwException(BizTaxException.Codes.IllegalArgumentError, "items");
        }

        // 兼容德永信老规范
        if (personalitemcodehash.size() <= 0) {
            List<BizMdDictionaryCode> BizMdDictionaryCodes = bizMdDictionaryCodeService.findByDefsn("personalItemCode");
            BizMdDictionaryCodes.forEach(item -> {
                personalitemcodehash.put(item.getSn(), item.getName());
                personalitemcodehash.put(item.getName(), item.getSn());
            });
        }
        if (countryareahash.size() <= 0) {
            List<BizMdDictionaryCode> bizMdDictionaryCodes = bizMdDictionaryCodeService.findByDefsn("countryArea");
            bizMdDictionaryCodes.forEach(item -> {
                countryareahash.put(item.getSn(), item.getName());
                countryareahash.put(item.getName(), item.getSn());
            });
        }
        if (cardclasshash.size() <= 0) {
            List<BizMdDictionaryCode> bizMdDictionaryCodes = bizMdDictionaryCodeService.findByDefsn("cardClass");
            bizMdDictionaryCodes.forEach(item -> {
                cardclasshash.put(item.getSn(), item.getName());
                cardclasshash.put(item.getName(), item.getSn());
            });
        }

        // 新的规范
        if (personalitemcodehashNew.size() <= 0) {
            List<BizMdDictionaryCode> BizMdDictionaryCodes = bizMdDictionaryCodeService.findByDefsn("incometype");
            BizMdDictionaryCodes.forEach(item -> {
                personalitemcodehashNew.put(item.getSn(), item.getName());
                personalitemcodehashNew.put(item.getName(), item.getSn());
            });
        }
        if (countryareahashNew.size() <= 0) {
            List<BizMdDictionaryCode> bizMdDictionaryCodes = bizMdDictionaryCodeService.findByDefsn("countrytype");
            bizMdDictionaryCodes.forEach(item -> {
                countryareahashNew.put(item.getSn(), item.getName());
                countryareahashNew.put(item.getName(), item.getSn());
            });
        }
        if (cardclasshashNew.size() <= 0) {
            List<BizMdDictionaryCode> bizMdDictionaryCodes = bizMdDictionaryCodeService.findByDefsn("cardtype");
            bizMdDictionaryCodes.forEach(item -> {
                cardclasshashNew.put(item.getSn(), item.getName());
                cardclasshashNew.put(item.getName(), item.getSn());
            });
        }

        JSONArray itemsData = personData.getJSONArray("items");

        BizTaxInstanceCategory bizTaxInstanceCategory = findBizTaxInstanceCategory(instClientDetailDTO, taxDataSyncDTO.getAccountcyclesn(), new ArrayList<TaxSn>() {{
            this.add(TaxSn.m_personal_payroll);
        }}, TaxOffice.ds);
        if (null == bizTaxInstanceCategory) {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxMdCategoryNotFound);
        }
        BizTaxInstanceCategoryPersonalPayroll bizTaxInstanceCategoryPersonalPayroll = bizTaxInstanceCategoryPersonalPayrollService.findByInstanceCategoryId(bizTaxInstanceCategory.getId());
        if (null == bizTaxInstanceCategoryPersonalPayroll) {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxMdCategoryNotFound);
        }
        List<BizTaxInstanceCategoryPersonalPayrollItem> itemList = new ArrayList<>();
        JSONObject item = null;
        for (int i = 0; i < itemsData.size(); i++) {
            item = itemsData.getJSONObject(i);
            BizTaxInstanceCategoryPersonalPayrollItem bizTaxInstanceCategoryPersonalPayrollItem = toPersonalPayrollItem(item);
            bizTaxInstanceCategoryPersonalPayrollItem.setBizTaxInstanceCategoryPersonalPayrollId(bizTaxInstanceCategoryPersonalPayroll.getId());
            bizTaxInstanceCategoryPersonalPayrollItem.setBegindate(
                    DateUtils.Format.YYYYMMDDHHMM.StringValue(
                            DateUtils.unixTimeToLocalDateTime(bizTaxInstanceCategory.getBeginDate()), DateUtils.ZH_PATTERN_DAY)
            );
            bizTaxInstanceCategoryPersonalPayrollItem.setEnddate(DateUtils.Format.YYYYMMDDHHMM.StringValue(
                    DateUtils.unixTimeToLocalDateTime(bizTaxInstanceCategory.getEndDate()), DateUtils.ZH_PATTERN_DAY));
            itemList.add(bizTaxInstanceCategoryPersonalPayrollItem);
        }
        bizTaxInstanceCategoryPersonalPayroll.setSourceType(BizTaxInstanceCategoryPersonalPayroll.SourceType.hand);
        bizTaxInstanceCategoryPersonalPayrollService.create(bizTaxInstanceCategoryPersonalPayroll);
        bizTaxInstanceCategoryPersonalPayrollItemService.create(bizTaxInstanceCategoryPersonalPayroll.getId(), true, itemList);
        taxInstanceCategoryPersonalMFactory.audit(bizTaxInstanceCategoryPersonalPayroll.getId(), userId, "confirmed");

    }

    private BizTaxInstanceCategoryPersonalPayrollItem toPersonalPayrollItem(JSONObject item) {
        BizTaxInstanceCategoryPersonalPayrollItem personalPayrollItem = new BizTaxInstanceCategoryPersonalPayrollItem();
        item.keySet().forEach((key) -> {
            if ("name".equals(key)) {
                personalPayrollItem.setName(item.getString(key));
            }
            if ("cardname".equals(key)) {
                personalPayrollItem.setCardtype(cardclasshash.get(item.getString(key)));
                personalPayrollItem.setCardname(cardclasshashNew.get(personalPayrollItem.getCardtype()));
            }
            if ("countryname".equals(key)) {
                personalPayrollItem.setCountryid(countryareahash.get(item.getString(key)));
                personalPayrollItem.setCountryname(countryareahashNew.get(personalPayrollItem.getCountryid()));
            }
            if ("itemname".equals(key)) {
                personalPayrollItem.setItemcode(personalitemcodehash.get(item.getString(key)));
                personalPayrollItem.setItemname(personalitemcodehashNew.get(personalPayrollItem.getItemcode()));
            }
            if ("wage".equals(key)) {
                personalPayrollItem.setWage(item.getBigDecimal(key));
            }
            if ("dutyfreeamount".equals(key)) {
                personalPayrollItem.setDutyfreeamount(item.getBigDecimal(key));
            }
            if ("pension".equals(key)) {
                personalPayrollItem.setPension(item.getBigDecimal(key));
            }
            if ("healthinsurance".equals(key)) {
                personalPayrollItem.setHealthinsurance(item.getBigDecimal(key));
            }
            if ("unemploymentinsurance".equals(key)) {
                personalPayrollItem.setUnemploymentinsurance(item.getBigDecimal(key));
            }
            if ("housingfund".equals(key)) {
                personalPayrollItem.setHousingfund(item.getBigDecimal(key));
            }
            if ("originalproperty".equals(key)) {
                personalPayrollItem.setOriginalproperty(item.getBigDecimal(key));
            }
            if ("allowdeduction".equals(key)) {
                personalPayrollItem.setAllowdeduction(item.getBigDecimal(key));
            }
            if ("other".equals(key)) {
                personalPayrollItem.setOther(item.getBigDecimal(key));
            }
            if ("deductiondonate".equals(key)) {
                personalPayrollItem.setDeductiondonate(item.getBigDecimal(key));
            }
            if ("relieftax".equals(key)) {
                personalPayrollItem.setRelieftax(item.getBigDecimal(key));
            }
            if ("alreadycosttax".equals(key)) {
                personalPayrollItem.setAlreadycosttax(item.getBigDecimal(key));
            }
            if ("deduction".equals(key)) {
                personalPayrollItem.setDeduction(item.getBigDecimal(key));
            }
            if ("insurance".equals(key)) {
                personalPayrollItem.setInsurance(item.getBigDecimal(key));
            }
            if ("nianjin".equals(key)) {
                personalPayrollItem.setAnnuity(item.getBigDecimal(key));
            }
            if ("cardnumber".equals(key)) {
                personalPayrollItem.setCardnumber(item.getString(key));
            }
        });
        personalPayrollItem.setUseType(BizTaxInstanceCategoryPersonalPayrollItem.UseType.declare);
        taxInstanceCategoryPersonalPayrollItemFactory.calculate(personalPayrollItem);
        return personalPayrollItem;
    }

    private void handlerFztax(TaxDataSyncDTO taxDataSyncDTO, InstClientDetailDTO instClientDetailDTO, JSONObject fzTaxData, long userId) {
        BizTaxInstanceCategory bizTaxInstanceCategory = findBizTaxInstanceCategory(instClientDetailDTO, taxDataSyncDTO.getAccountcyclesn(), new ArrayList<TaxSn>() {{
            this.add(TaxSn.m_fz);
            this.add(TaxSn.q_fz);
        }}, TaxOffice.ds);
        if (null == bizTaxInstanceCategory) {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxMdCategoryNotFound);
        }
        BizTaxInstanceCategoryFz bizTaxInstanceCategoryFz = bizTaxInstanceCategoryFzService.findByBizTaxInstanceCategoryId(bizTaxInstanceCategory.getId());
        if (null == bizTaxInstanceCategoryFz) {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxMdCategoryNotFound);
        }
        if (bizTaxInstanceCategory.isAudit()) {
            ServiceTaxException.throwException(ServiceTaxException.Codes.AUDIT_ERROR, "附征税已审核通过，请先撤销审核.");
        }
        List<BizTaxInstanceCategoryFzItem> fzItems = bizTaxInstanceCategoryFzItemService.findByBizTaxInstanceCategoryFzId(bizTaxInstanceCategoryFz.getId());
        if (CollectionUtils.isEmpty(fzItems)) {
            BizTaxException.throwException(BizTaxException.Codes.TaxInstanceCategoryNotFound);
        }
        fzTaxData.keySet().forEach((key) -> {
            if ("vatSaleAmount".equals(key)) {
                bizTaxInstanceCategoryFz.setVatSaleAmount(fzTaxData.getBigDecimal(key));
            }
            if ("vatTaxAmount".equals(key)) {
                bizTaxInstanceCategoryFz.setVatTaxAmount(fzTaxData.getBigDecimal(key));
            }
            if ("soqTaxAmount".equals(key)) {
                bizTaxInstanceCategoryFz.setSoqTaxAmount(fzTaxData.getBigDecimal(key));
            }
            if ("businessTaxAmount".equals(key)) {
                BizTaxException.throwException(BizTaxException.Codes.IllegalArgumentError, "暂时不支持营业税应纳税额");
                bizTaxInstanceCategoryFz.setBusinessTaxAmount(fzTaxData.getBigDecimal(key));
            }
        });
        bizTaxInstanceCategoryFzService.create(bizTaxInstanceCategoryFz);
        fzItems.stream().forEach((item) -> {
            if ("302030100".equals(item.getItemDetailCode()) ||
                    "101090101".equals(item.getItemDetailCode()) ||
                    "302160100".equals(item.getItemDetailCode())) {
                item.setSaleAmountVatNormal(bizTaxInstanceCategoryFz.getVatTaxAmount());
                item.setSaleAmountTotal(new BigDecimal(item.getSaleAmountVatNormal().doubleValue() + item.getSaleAmountVatFree().doubleValue() + item.getSaleAmountSoq().doubleValue() + item.getSaleAmountBusiness().doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP));
                item.setTaxPayAmount(new BigDecimal(item.getSaleAmountTotal().doubleValue() * item.getTaxRate().doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP));
                if (item.getTaxPayAmount().doubleValue() > 0
                        && (((bizTaxInstanceCategoryFz.getVatSaleAmount().doubleValue() <= 100000 && bizTaxInstanceCategoryFz.getFrType().equals(BizTaxInstanceCategoryFr.FrType.normal))
                        || (bizTaxInstanceCategoryFz.getVatSaleAmount().doubleValue() <= 300000 && bizTaxInstanceCategoryFz.getFrType().equals(BizTaxInstanceCategoryFr.FrType.small)))
                        && (item.getItemDetailCode().equals("302030100") || item.getItemDetailCode().equals("302160100")))) {
                    item.setTaxRemitCode("0099129999");
                    item.setTaxRemitAmount(item.getTaxPayAmount());
                }
            } else if ("302030300".equals(item.getItemDetailCode()) ||
                    "101090103".equals(item.getItemDetailCode()) ||
                    "302160300".equals(item.getItemDetailCode())) {
                item.setSaleAmountVatNormal(bizTaxInstanceCategoryFz.getSoqTaxAmount());
                item.setSaleAmountTotal(new BigDecimal(item.getSaleAmountVatNormal().doubleValue() + item.getSaleAmountVatFree().doubleValue() + item.getSaleAmountSoq().doubleValue() + item.getSaleAmountBusiness().doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP));
                item.setTaxPayAmount(new BigDecimal(item.getSaleAmountTotal().doubleValue() * item.getTaxRate().doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP));
//                    TODO 暂时屏蔽消费税的减免
//                    if (((fz.getVatSaleAmount().doubleValue() <= 100000 && fz.getFrType().equals(BizTaxInstanceCategoryFr.FrType.normal)) || (fz.getVatSaleAmount().doubleValue() <= 300000 && fz.getFrType().equals(BizTaxInstanceCategoryFr.FrType.small))) && (item.getDetailCode().equals("302030300") || item.getDetailCode().equals("302160300"))) {
//                        bizTaxInstanceCategoryFzItem.setTaxRemitCode("0099129999");
//                        bizTaxInstanceCategoryFzItem.setTaxRemitAmount(bizTaxInstanceCategoryFzItem.getTaxPayAmount());
//                    }
            }
        });
        BizTaxFzItemDTO bizTaxFzItemDTO = new BizTaxFzItemDTO();
        bizTaxFzItemDTO.setFzId(bizTaxInstanceCategoryFz.getId());
        bizTaxFzItemDTO.setFzItems(fzItems);
        bizTaxInstanceCategoryFzItemService.save(bizTaxInstanceCategoryFz.getId(), fzItems);
        taxInstanceCategoryFzFactory.confirmed(bizTaxFzItemDTO, userId);
    }

    private BizTaxInstanceCategory findBizTaxInstanceCategory(InstClientDetailDTO instClientDetailDTO, String accountCycleSn, List<TaxSn> taxSns) {
        return findBizTaxInstanceCategory(instClientDetailDTO, accountCycleSn, taxSns, null);
    }

    private BizTaxInstanceCategory findBizTaxInstanceCategory(InstClientDetailDTO instClientDetailDTO, String accountCycleSn, List<TaxSn> taxSns, TaxOffice taxOffice) {
        List<BizMdAccountCycle> bizMdAccountCycles = bizMdAccountCycleService
                .findBySn(accountCycleSn);
        if (CollectionUtils.isEmpty(bizMdAccountCycles)) {
            BizTaxException.throwException(BizTaxException.Codes.IllegalArgumentError, "无法找到税期");
        }
        List<TaxOffice> taxOffices = new ArrayList<>();
        if (null == taxOffice) {
            taxOffices.add(TaxOffice.gs);
            taxOffices.add(TaxOffice.ds);
        } else {
            taxOffices.add(taxOffice);
        }
        BizTaxInstance tempInstance = null;
        BizTaxInstanceCategory tempCategory = null;
        for (BizMdAccountCycle bizMdAccountCycle : bizMdAccountCycles) {
            for (TaxOffice to : taxOffices) {
                tempInstance = getTaxInstance(instClientDetailDTO, bizMdAccountCycle, to);
                if (null != tempInstance) {
                    for (TaxSn taxSn : taxSns) {
                        tempCategory = getTaxIntanceCategory(tempInstance, taxSn);
                        if (null != tempCategory) {
                            return tempCategory;
                        }
                    }
                }
            }
        }
        return null;
    }

    private BizTaxInstanceCategory getTaxIntanceCategory(BizTaxInstance tempInstance, TaxSn taxSn) {
        BizTaxMdCategory bizTaxMdCategory = bizTaxMdCategoryService.findBySn(taxSn);
        BizTaxInstanceCategory bizTaxInstanceCategory = bizTaxInstanceCategoryService.findByInstanceAndTaxMdCategoryId(tempInstance.getId(),
                bizTaxMdCategory.getId());
        return bizTaxInstanceCategory;
    }

    private BizTaxInstance getTaxInstance(InstClientDetailDTO instClientDetailDTO, BizMdAccountCycle bizMdAccountCycle, TaxOffice taxOffice) {
        return bizTaxInstanceService
                .findByMdInstClientIdAndMdCompanyIdAndMdAccountCycleIdAndTaxOfficeAndDisabled(
                        instClientDetailDTO.getInstClientId(), instClientDetailDTO.getCompanyId(),
                        bizMdAccountCycle.getId(), taxOffice);
    }

}
