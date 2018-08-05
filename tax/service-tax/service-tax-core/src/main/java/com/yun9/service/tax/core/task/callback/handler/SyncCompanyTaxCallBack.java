package com.yun9.service.tax.core.task.callback.handler;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.md.BizMdInstClientService;
import com.yun9.biz.md.domain.dto.CompanyOwnerDTO;
import com.yun9.biz.md.domain.dto.InstClientDetailDTO;
import com.yun9.biz.md.domain.entity.BizMdCompanyOwner;
import com.yun9.biz.md.enums.TaxType;
import com.yun9.biz.tax.BizTaxCompanyTaxService;
import com.yun9.biz.tax.BizTaxMdMsgCodeService;
import com.yun9.biz.tax.domain.dto.tax.CompanyTax;
import com.yun9.biz.tax.domain.dto.tax.TaxOfficeRegisterTaxCategory;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyCategoryItem;
import com.yun9.biz.tax.domain.entity.BizTaxCompanyTax;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdMsgCode;
import com.yun9.biz.tax.enums.BillingType;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.InvoiceSystem;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.ops.BizTaxCompanyTaxStartService;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import com.yun9.service.tax.core.task.annotation.TaskSnMapping;
import com.yun9.service.tax.core.task.callback.TaskCallBackContext;
import com.yun9.service.tax.core.task.helper.JsonMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.yun9.service.tax.core.exception.ServiceTaxException.Codes.CallbackProcessFailed;


/**
 * Created by zhengzb on  2018/6/21.
 * 同步客户资料回调处理
 */
@TaskSnMapping(sns = {"SZ0014"})
public class SyncCompanyTaxCallBack extends AbstractCallbackHandlerMapping {

    public static Logger logger = LoggerFactory.getLogger(SyncCompanyTaxCallBack.class);

    @Autowired
    BizMdInstClientService bizMdInstClientService;
    @Autowired
    BizTaxMdMsgCodeService bizTaxMdMsgCodeService;
    //    @Autowired
//    BizTaxCompanyTaxGsService bizTaxCompanyTaxGsService;
    @Autowired
    BizTaxCompanyTaxService bizTaxCompanyTaxService;
    @Autowired
    BizTaxCompanyTaxStartService bizTaxCompanyTaxStartService;

    @Override
    public TaskCallBackContext process(TaskCallBackContext context) {

        //同步客户资料回调处理
        final BizTaxCompanyTax bizTaxCompanyTax = bizTaxCompanyTaxService.findBySeq(context.getTaskCallBackResponse().getSeq());
        //如果找不到seq,直接返回，不抛出异常
        if (bizTaxCompanyTax == null) {
            logger.warn("回调任务seq：{}，无法找到！跳过本次处理!", context.getTaskCallBackResponse().getSeq());
            return context;
        }
        context.setBizTaxCompanyTax(bizTaxCompanyTax);
        try {
            if (200 == context.getTaskCallBackResponse().getCode()) {
                JsonMap body = JSON.parseObject(context.getBody(), JsonMap.class);
                //同步客户资料
                this.syncInstClientInfo(bizTaxCompanyTax, body);
                //同步客户税务信息
                this.syncCompanyTaxInfo(bizTaxCompanyTax, body);
                return context;
            } else {
                //查询code
                BizTaxMdMsgCode mdMsgCode = bizTaxMdMsgCodeService.findByItemCode(String.valueOf(context.getTaskCallBackResponse().getCode()));
                bizTaxCompanyTax.setProcessMessage(context.getTaskCallBackResponse().getMessage());
                bizTaxCompanyTax.setProcessCodeId(null == mdMsgCode ? null : mdMsgCode.getId());
                bizTaxCompanyTax.setProcessState(BizTaxCompanyTax.ProcessState.exception);
                bizTaxCompanyTaxService.create(bizTaxCompanyTax);
                return context;
            }
        } catch (Exception e) {
            logger.error("回调执行错误", e);
            throw ServiceTaxException.build(CallbackProcessFailed, e.getMessage());
        }
    }

    //同步客户资料
    protected void syncInstClientInfo(BizTaxCompanyTax bizTaxCompanyTax, JsonMap body) {
        List<JsonMap> clientInfos = body.getArray("clientInfos");
        CompanyTax companyTax = GetTaxCategoriesCallBack.ProcessBody.companyTax(body);
        if (null != companyTax && CollectionUtils.isNotEmpty(clientInfos)) {
            for (JsonMap map : clientInfos) {
                InstClientDetailDTO instClientDetailDTO = new InstClientDetailDTO();
                instClientDetailDTO.setFullName(map.getString("clientFullName"));
                instClientDetailDTO.setCreditCode(map.getString("creditCode"));
                if (StringUtils.isNotEmpty(companyTax.getTaxType().name())) {
                    if (TaxType.small.name().equals(companyTax.getTaxType().name()) && StringUtils.isNotEmpty(map.getString("zzlxmc")) && map.getString("zzlxmc").contains("个体")) {
                        instClientDetailDTO.setTaxType(TaxType.personal);
                    } else {
                        instClientDetailDTO.setTaxType(TaxType.valueOf(companyTax.getTaxType().name()));
                    }
                }
                instClientDetailDTO.setCompanyId(bizTaxCompanyTax.getBizMdCompanyId());

                List<CompanyOwnerDTO> bizMdCompanyOwners = new ArrayList<>();
                //法人
                CompanyOwnerDTO legalBizMdCompanyOwner = new CompanyOwnerDTO();
                legalBizMdCompanyOwner.setCompanyId(bizTaxCompanyTax.getBizMdCompanyId());
                legalBizMdCompanyOwner.setType(BizMdCompanyOwner.Type.owner);
                legalBizMdCompanyOwner.setOwnername(map.getString("legalPerson"));
                legalBizMdCompanyOwner.setPhone(map.getString("legalPersonMobilePhone"));
                if (StringUtils.isNotEmpty(map.getString("legalPersonCardName")) && map.getString("legalPersonCardName").contains("身份证")) {
                    legalBizMdCompanyOwner.setIdcardType("01");
                }
                if (StringUtils.isNotEmpty(map.getString("legalPersonCardName")) && map.getString("legalPersonCardName").contains("驾驶证")) {
                    legalBizMdCompanyOwner.setIdcardType("02");
                }
                legalBizMdCompanyOwner.setIdcardNumber(map.getString("legalPersonCardNum"));
                bizMdCompanyOwners.add(legalBizMdCompanyOwner);

                CompanyOwnerDTO frBizMdCompanyOwner = new CompanyOwnerDTO();
                legalBizMdCompanyOwner.setCompanyId(bizTaxCompanyTax.getBizMdCompanyId());
                frBizMdCompanyOwner.setType(BizMdCompanyOwner.Type.owner);
                frBizMdCompanyOwner.setOwnername(map.getString("frManager"));
                frBizMdCompanyOwner.setPhone(map.getString("frManagerMobilePhone"));
                if (StringUtils.isNotEmpty(map.getString("frManagerCardName")) && map.getString("frManagerCardName").contains("身份证")) {
                    frBizMdCompanyOwner.setIdcardType("01");
                }
                if (StringUtils.isNotEmpty(map.getString("frManagerCardName")) && map.getString("frManagerCardName").contains("驾驶证")) {
                    frBizMdCompanyOwner.setIdcardType("02");
                }
                frBizMdCompanyOwner.setIdcardNumber(map.getString("frManagerCardNumber"));
                bizMdCompanyOwners.add(frBizMdCompanyOwner);

                instClientDetailDTO.setOwners(bizMdCompanyOwners);

                bizMdInstClientService.update(instClientDetailDTO, bizTaxCompanyTax.getSyncBy());
            }
        }

    }

    //同步客户税务信息
    protected void syncCompanyTaxInfo(BizTaxCompanyTax bizTaxCompanyTax, JsonMap body) {
        List<TaxOfficeRegisterTaxCategory> taxOfficeRegisterTaxCategories = ProcessBody.registerTaxCategory(body, TaxOffice.gs);
        //客户税务信息
        CompanyTax companyTax = SyncCompanyTaxCallBack.ProcessBody.companyTax(body);
        InvoiceSystem invoiceSystem = SyncCompanyTaxCallBack.ProcessBody.getInvoiceSystem(body);
        bizTaxCompanyTaxStartService.callback(bizTaxCompanyTax.getBizMdCompanyId(),
                taxOfficeRegisterTaxCategories,
                companyTax,
                invoiceSystem,
                null);
    }

    public static class ProcessBody {


        public static List<TaxOfficeRegisterTaxCategory> registerTaxCategory(JsonMap body, TaxOffice taxOffice) {
            List<TaxOfficeRegisterTaxCategory> result = new ArrayList<>();

//            if (CollectionUtils.isNotEmpty(_registerList)) {
//                Map<String, List<BizTaxCompanyCategoryItem>> taxCompanyCategoryMap = new HashMap<>();
//                for (JsonMap _map : _registerList) {
//                    String key = _map.getString("zspmDm") + "&" + getCycleType(_map.getString("nsqx")) + "&" + TaxOffice.ds.toString();
//                    BizTaxCompanyCategoryItem bizTaxCompanyCategoryItem = new BizTaxCompanyCategoryItem();
//                    bizTaxCompanyCategoryItem.setItemCode(_map.getString("zsxmDm"));
//                    bizTaxCompanyCategoryItem.setItemName(_map.getString("zsxmMc"));
//                    bizTaxCompanyCategoryItem.setDetailCode(_map.getString("zspmDm"));
//                    bizTaxCompanyCategoryItem.setCycleType(getCycleType(_map.getString("nsqx")).name());
//                    bizTaxCompanyCategoryItem.setTaxRate(new BigDecimal(_map.getString("sl")));
//                    bizTaxCompanyCategoryItem.setTaxStartdate(java.sql.Date.valueOf(_map.getString("yxqQ")).getTime() / 1000);
//                    bizTaxCompanyCategoryItem.setTaxEnddate(java.sql.Date.valueOf(_map.getString("yxqZ")).getTime() / 1000);
//                    if (null == taxCompanyCategoryMap.get(key)) {
//                        List<BizTaxCompanyCategoryItem> taxCompanyCategoryList = new ArrayList<>();
//                        taxCompanyCategoryList.add(bizTaxCompanyCategoryItem);
//                        taxCompanyCategoryMap.put(key, taxCompanyCategoryList);
//                    } else {
//                        taxCompanyCategoryMap.get(key).add(bizTaxCompanyCategoryItem);
//                    }
//                }
//
//                taxCompanyCategoryMap.forEach((k, v) -> {
//                    TaxOfficeRegisterTaxCategory taxOfficeRegisterTaxCategory = new TaxOfficeRegisterTaxCategory();
//                    String[] keys = k.split("&");
//                    taxOfficeRegisterTaxCategory.setTaxOfficeCategoryPropertyValue(keys[0]);
//                    taxOfficeRegisterTaxCategory.setCycleType(CycleType.valueOf(keys[1]));
//                    taxOfficeRegisterTaxCategory.setTaxOffice(TaxOffice.ds);
//                    taxOfficeRegisterTaxCategory.setBizTaxCompanyCategoryItems(v);
//                    result.add(taxOfficeRegisterTaxCategory);
//                });
//            }
//            List<JsonMap> _gsList = body.getArray("gsRdsz");
//            if (CollectionUtils.isNotEmpty(_gsList)) {
//                Map<String, List<BizTaxCompanyCategoryItem>> taxGsCompanyCategoryMap = new HashMap<>();
//                for (JsonMap _map : _gsList) {
//                    String key = _map.getString("taxCode") + "&" + _map.getString("cycleType") + "&" + TaxOffice.gs.toString();
//                    BizTaxCompanyCategoryItem bizTaxCompanyCategoryItem = new BizTaxCompanyCategoryItem();
//                    bizTaxCompanyCategoryItem.setItemCode(_map.getString("xmdmCode"));
//                    bizTaxCompanyCategoryItem.setItemName(_map.getString("zsxmMc"));
//                    bizTaxCompanyCategoryItem.setDetailCode(_map.getString("taxCode"));
//                    bizTaxCompanyCategoryItem.setCycleType( _map.getString("cycleType"));
//                    bizTaxCompanyCategoryItem.setTaxRate(StringUtils.isNotEmpty(_map.getString("sl"))?new BigDecimal(_map.getString("sl")):new BigDecimal(0));
//                    bizTaxCompanyCategoryItem.setTaxStartdate(java.sql.Date.valueOf(_map.getString("zcqq")).getTime() / 1000);
//                    bizTaxCompanyCategoryItem.setTaxEnddate(java.sql.Date.valueOf(_map.getString("qxqz")).getTime() / 1000);
//                    if (null == taxGsCompanyCategoryMap.get(key)) {
//                        List<BizTaxCompanyCategoryItem> taxCompanyCategoryList = new ArrayList<>();
//                        taxCompanyCategoryList.add(bizTaxCompanyCategoryItem);
//                        taxGsCompanyCategoryMap.put(key, taxCompanyCategoryList);
//                    } else {
//                        taxGsCompanyCategoryMap.get(key).add(bizTaxCompanyCategoryItem);
//                    }
//                }
//
//                taxGsCompanyCategoryMap.forEach((k, v) -> {
//                    TaxOfficeRegisterTaxCategory taxOfficeRegisterTaxCategory = new TaxOfficeRegisterTaxCategory();
//                    String[] keys = k.split("&");
//                    taxOfficeRegisterTaxCategory.setTaxOfficeCategoryPropertyValue(keys[0]);
//                    taxOfficeRegisterTaxCategory.setCycleType(CycleType.valueOf(keys[1]));
//                    taxOfficeRegisterTaxCategory.setTaxOffice(TaxOffice.gs);
//                    taxOfficeRegisterTaxCategory.setBizTaxCompanyCategoryItems(v);
//                    result.add(taxOfficeRegisterTaxCategory);
//                });
//            }v
            List<JsonMap> dsRdsz = body.getArray("dsRdsz");
            if (CollectionUtils.isNotEmpty(dsRdsz)) {
                for (JsonMap map : dsRdsz) {
                    result.add(getTaxOfficeRegisterTaxCategory(map));
                }
            }
            List<JsonMap> gsList = body.getArray("gsRdsz");
            if (CollectionUtils.isNotEmpty(gsList)) {
                for (JsonMap map : gsList) {
                    result.add(getTaxOfficeRegisterTaxCategory(map));
                }
            }

            return CollectionUtils.isNotEmpty(result) ? result : null;
        }

        private static TaxOfficeRegisterTaxCategory getTaxOfficeRegisterTaxCategory(JsonMap jsonMap) {
            TaxOfficeRegisterTaxCategory taxOfficeRegisterTaxCategory = new TaxOfficeRegisterTaxCategory();
            taxOfficeRegisterTaxCategory.setTaxOfficeCategoryPropertyValue(jsonMap.getString("taxCode"));
            if (StringUtils.isEmpty(jsonMap.getString("cycleType"))) {
                throw ServiceTaxException.build(CallbackProcessFailed, "认定税种cycleType周期为空");
            }
            taxOfficeRegisterTaxCategory.setCycleType(CycleType.valueOf(jsonMap.getString("cycleType")));
            taxOfficeRegisterTaxCategory.setTaxOffice(TaxOffice.ds);
            //征收品目明细
            if (StringUtils.isNotEmpty(jsonMap.getArray("pmList")) && jsonMap.getArray("pmList").size() > 0) {
                List<JsonMap> pmList = jsonMap.getArray("pmList");
                List<BizTaxCompanyCategoryItem> bizTaxCompanyCategoryItemlsit = new ArrayList<>();
                pmList.forEach(userItem -> {
                    bizTaxCompanyCategoryItemlsit.add(getBizTaxCompanyCategoryItem(userItem));
                });
                taxOfficeRegisterTaxCategory.setBizTaxCompanyCategoryItems(bizTaxCompanyCategoryItemlsit);
            }
            return taxOfficeRegisterTaxCategory;
        }

        private static BizTaxCompanyCategoryItem getBizTaxCompanyCategoryItem(JsonMap jsonMap) {
            BizTaxCompanyCategoryItem item = new BizTaxCompanyCategoryItem();
            item.setItemCode(jsonMap.getString("itemCode"));
            item.setItemName(jsonMap.getString("itemName"));
            item.setDetailCode(jsonMap.getString("detailCode"));
            item.setCycleType(jsonMap.getString("cycleType"));
            item.setTaxRate(new BigDecimal(jsonMap.getString("taxRate")));
            item.setTaxStartdate(getTime(java.sql.Date.valueOf(jsonMap.getString("taxStartDate")).getTime() / 1000));
            item.setTaxEnddate(getTime(java.sql.Date.valueOf(jsonMap.getString("taxEndDate")).getTime() / 1000));
            return item;
        }

        public static CompanyTax companyTax(JsonMap body) {
            JsonMap _companyTax = body.getObject("clientinfotaxtype");
            if (null != _companyTax && 200 == _companyTax.getInteger("code")) {
                CompanyTax companyTax = new CompanyTax();
                if (StringUtils.isNotEmpty(_companyTax.getString("taxtype"))) {
                    companyTax.setTaxType(com.yun9.biz.tax.enums.TaxType.valueOf(_companyTax.getString("taxtype")));
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

        public static InvoiceSystem getInvoiceSystem(JsonMap body) {
            JsonMap _invoiceSystem = body.getObject("invoicesystem");
            if (null != _invoiceSystem && 200 == _invoiceSystem.getInteger("code")) {
                if (StringUtils.isNotEmpty(_invoiceSystem.getString("invoiceSystem"))) {
                    if ("N".equals(_invoiceSystem.getString("invoiceSystem"))) {
                        return InvoiceSystem.none;
                    }
                    if ("Y".equals(_invoiceSystem.getString("invoiceSystem"))) {
                        return InvoiceSystem.exist;
                    }
                }
            }
            return null;
        }

        public static CycleType getCycleType(String cycleType) {
            if (StringUtils.isEmpty(cycleType)) {
                return null;
            }
            switch (cycleType) {
                case "年":
                    return CycleType.y;
                case "季":
                    return CycleType.q;
                case "月":
                    return CycleType.m;
            }
            return null;
        }
    }

    private static int getTime(Long times) {
        if (StringUtils.isEmpty(times)) {
            return Integer.MAX_VALUE;
        }
        if (times <= Integer.MAX_VALUE){
            return times.intValue();
        }
        return Integer.MAX_VALUE;
    }

}
