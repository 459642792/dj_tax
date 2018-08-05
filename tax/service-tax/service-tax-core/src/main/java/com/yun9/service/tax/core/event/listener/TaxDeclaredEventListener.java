package com.yun9.service.tax.core.event.listener;

import com.yun9.biz.tax.*;
import com.yun9.biz.tax.domain.entity.*;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.enums.DeclareType;
import com.yun9.biz.tax.enums.TaxOffice;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.biz.tax.event.VatEventService;
import com.yun9.commons.utils.CollectionUtils;
import com.yun9.service.tax.core.event.TaxDeclaredEvent;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-07-10 17:41
 */
@Component
public class TaxDeclaredEventListener implements ApplicationListener<TaxDeclaredEvent> {
    @Autowired
    BizTaxInstanceService bizTaxInstanceService;
    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;
    @Autowired
    BizTaxMdCategoryService bizTaxMdCategoryService;
    @Autowired
    BizTaxInstanceCategoryFzService bizTaxInstanceCategoryFzService;
    @Autowired
    BizTaxInstanceCategoryFzItemService bizTaxInstanceCategoryFzItemService;
    @Autowired
    VatEventService vatEventService;

    @Override
    public void onApplicationEvent(TaxDeclaredEvent event) {
        BizTaxInstanceCategory bizTaxInstanceCategory = event.getBizTaxInstanceCategory();

        //增值税申报成功事件逻辑
        BizTaxMdCategory taxMdCategory = bizTaxMdCategoryService.findById(bizTaxInstanceCategory.getBizTaxMdCategoryId());

        if (taxMdCategory == null) {
            throw ServiceTaxException.build(ServiceTaxException.Codes.tax_router_config_not_found, "税种配置");
        }
        TaxSn taxSn = taxMdCategory.getSn();
        switch (taxSn) {
            //增值税申报成功事件逻辑
            case m_vat:

                if (bizTaxInstanceCategory.getDeclareType() == DeclareType.yun9 || bizTaxInstanceCategory.getDeclareType() == DeclareType.taxOffice) {
                    BizTaxInstance bizTaxInstance = bizTaxInstanceService.findById(bizTaxInstanceCategory.getBizTaxInstanceId());

                    BizTaxInstance bizTaxInstanceDs = bizTaxInstanceService.currentTaxOfficeInstClientInstance(bizTaxInstance.getMdInstClientId(), bizTaxInstance.getMdCompanyId(), bizTaxInstance.getMdAccountCycleId(), TaxOffice.ds, bizTaxInstance.getMdAreaId());
                    if (bizTaxInstanceDs == null) {
                        throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "没有地税申报记录");
                    }
                    List<BizTaxInstanceCategory> instanceCategoryList = bizTaxInstanceCategoryService.findByBizTaxInstanceId(bizTaxInstanceDs.getId());

                    if (CollectionUtils.isEmpty(instanceCategoryList)) {
                        throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "没有地税具体申报记录");

                    }
                    List<BizTaxInstanceCategory> bizTaxInstanceCategories = instanceCategoryList.stream().filter(a -> {
                        BizTaxMdCategory bizTaxMdCategory = bizTaxMdCategoryService.findById(a.getBizTaxMdCategoryId());
                        //附征税月报和季报没有审核的记录
                        return bizTaxMdCategory != null && (bizTaxMdCategory.getSn() == TaxSn.m_fz || bizTaxMdCategory.getSn() == TaxSn.q_fz) && !a.isAudit();
                    }).collect(Collectors.toList());
                    if (CollectionUtils.isEmpty(bizTaxInstanceCategories)) {
                        throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "没有申报附征税");
                    }
                    bizTaxInstanceCategories.forEach(a -> {
                        BizTaxInstanceCategoryFz bizTaxInstanceCategoryFz = bizTaxInstanceCategoryFzService.findByBizTaxInstanceCategoryId(a.getId());
                        if (bizTaxInstanceCategoryFz == null) {
                            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "没有找到附征税记录");
                        }
                        List<BizTaxInstanceCategoryFzItem> list = bizTaxInstanceCategoryFzItemService.findByBizTaxInstanceCategoryFzId(bizTaxInstanceCategoryFz.getId());
                        if (CollectionUtils.isEmpty(list)) {
                            throw ServiceTaxException.build(ServiceTaxException.Codes.IllegalArgumentException, "没有找到附征税报表记录");
                        }
                        final BigDecimal[] taxPayAmount = {BigDecimal.ZERO};
                        list.forEach(item -> {
                            if (bizTaxInstanceCategoryFz.getVatAudit() == 1) {
                                item.setSaleAmountVatNormal(bizTaxInstanceCategoryFz.getVatTaxAmount());
                                item.setSaleAmountTotal(new BigDecimal(item.getSaleAmountVatNormal().doubleValue() + item.getSaleAmountVatFree().doubleValue() + item.getSaleAmountSoq().doubleValue() + item.getSaleAmountBusiness().doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP));
                                item.setTaxPayAmount(new BigDecimal(item.getSaleAmountTotal().doubleValue() * item.getTaxRate().doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP));
                                if (item.getTaxPayAmount().doubleValue() > 0 && ((bizTaxInstanceCategoryFz.getVatSaleAmount().doubleValue() <= 100000 && bizTaxInstanceCategoryFz.getFrType().equals(BizTaxInstanceCategoryFr.FrType.normal)) || (bizTaxInstanceCategoryFz.getVatSaleAmount().doubleValue() <= 300000 && bizTaxInstanceCategoryFz.getFrType().equals(BizTaxInstanceCategoryFr.FrType.small))) && (item.getItemDetailCode().equals("302030100") || item.getItemDetailCode().equals("302160100"))) {
                                    item.setTaxRemitCode("0099129999");
                                    item.setTaxRemitAmount(item.getTaxPayAmount());
                                }
                            } else if (bizTaxInstanceCategoryFz.getSoqAudit() == 1) {
                                item.setSaleAmountVatNormal(bizTaxInstanceCategoryFz.getSoqTaxAmount());
                                item.setSaleAmountTotal(new BigDecimal(item.getSaleAmountVatNormal().doubleValue() + item.getSaleAmountVatFree().doubleValue() + item.getSaleAmountSoq().doubleValue() + item.getSaleAmountBusiness().doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP));
                                item.setTaxPayAmount(new BigDecimal(item.getSaleAmountTotal().doubleValue() * item.getTaxRate().doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP));

                            }
                            item.setTaxShouldPayAmount(new BigDecimal(item.getTaxPayAmount().doubleValue() - item.getTaxRemitAmount().doubleValue() - item.getTaxAlreadyPayAmount().doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP));
                            taxPayAmount[0] = new BigDecimal(taxPayAmount[0].doubleValue() + item.getTaxShouldPayAmount().doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP);
                        });

                        vatEventService.fzTaxReportInfoChange(bizTaxInstanceCategory, list, bizTaxInstanceCategoryFz.getId(), taxPayAmount[0]);

                    });


                }
                break;
            default:
                break;
        }

    }
}
