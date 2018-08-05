package com.yun9.service.tax.core.impl;

import com.yun9.biz.tax.BizTaxInstanceCategoryDeductService;
import com.yun9.biz.tax.BizTaxInstanceCategoryHistoryService;
import com.yun9.biz.tax.BizTaxInstanceCategoryService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryDeduct;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryHistory;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.service.tax.core.TaxInstanceCategoryDeductFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TaxInstanceCategoryDeductFactoryImpl implements TaxInstanceCategoryDeductFactory {
    public static final Logger logger = LoggerFactory.getLogger(TaxInstanceCategoryDeductFactoryImpl.class);


    @Autowired
    BizTaxInstanceCategoryService bizTaxInstanceCategoryService;


    @Autowired
    BizTaxInstanceCategoryDeductService bizTaxInstanceCategoryDeductService;


    @Autowired
    BizTaxInstanceCategoryHistoryService bizTaxInstanceCategoryHistoryService;

    @Override
    public List<HashMap> listByInstanceCategoryIdAndParams(long instanceCategoryId, Map<String, Object> params) {
        //1 获取二维码
        List<BizTaxInstanceCategoryDeduct> deducts = Optional.ofNullable(bizTaxInstanceCategoryDeductService.listByInstanceCategoryIdAndParams(instanceCategoryId, params))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BizTaxException, "没有申报扣款记录"));

        //2 根据 税种申报ID 获取税种实例
        BizTaxInstanceCategory bizTaxInstanceCategory = Optional.ofNullable(bizTaxInstanceCategoryService.findById(instanceCategoryId))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BizTaxException, "税种实例不存在"));

        return new ArrayList() {{
            if (deducts != null) {
                deducts.forEach(v -> {
                    add(new HashMap() {{
                        put("payDate", v.getPayDate());//缴款时间
                        put("message", v.getMessage());//支付消息
                        put("taxInstanceCategoryId", v.getBizTaxInstanceCategoryId());//税种id
                        put("id", v.getId());//扣款对象id
                        put("taxTicketCode", v.getTaxTicketCode());//税票号码
                        put("deductType",v.getType());//缴款方式
                        put("qrcode",v.getQrcode());//支付二维码
                        put("createdAt",v.getCreatedAt());//创建时间
                        put("payExpiry",v.getPayExpiry());//支付有效期
                        put("deductCheckState",v.getDeductCheckState());//扣款凭证状态 0没有凭证1有凭证
                        put("deductCheckDate",v.getDeductCheckDate());//扣款凭证检查时间
                        put("taxOfficePayAmount",bizTaxInstanceCategory.getTaxOfficePayAmount());//实际已交纳税额度
                        put("realPayAmount",bizTaxInstanceCategory.getRealPayAmount());//税局返回的应纳税额度
                        put("taxPayAmount",bizTaxInstanceCategory.getTaxPayAmount());//系统返回的应纳税额度
                    }});
                });
            }
        }};

    }


    @Override
    public void cancel(long id, long processBy) {

        //1 根据 税种申报ID 获取申报扣款
        BizTaxInstanceCategoryDeduct bizTaxInstanceCategoryDeduct = Optional.ofNullable(bizTaxInstanceCategoryDeductService.findById(id))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BizTaxException, "没有申报扣款记录"));

        if (bizTaxInstanceCategoryDeduct.getState() == BizTaxInstanceCategoryDeduct.State.expire) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "操作失败，当前状态已\"过期\"");
        }

        if (bizTaxInstanceCategoryDeduct.getType() != BizTaxInstanceCategoryDeduct.Type.wx) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "操作失败，当前支付方式不是\"微信支付\"");
        }


        //2 根据 税种申报ID 获取税种实例
        BizTaxInstanceCategory bizTaxInstanceCategory = Optional.ofNullable(bizTaxInstanceCategoryService.findById(bizTaxInstanceCategoryDeduct.getBizTaxInstanceCategoryId()))
                .orElseThrow(() -> BizTaxException.build(BizTaxException.Codes.BizTaxException, "税种实例不存在"));

        //3 判断是否有其他执行状态 process
        if (bizTaxInstanceCategory.getState() != BizTaxInstanceCategory.State.deduct) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "当前状态不是\"缴税\"状态");
        }

        bizTaxInstanceCategoryService.isProcess(bizTaxInstanceCategory);

        //3 修改instanceCategory的状态
        bizTaxInstanceCategory.setProcessState(BizTaxInstanceCategory.ProcessState.none);
        bizTaxInstanceCategory.setProcessCodeId(null);
        bizTaxInstanceCategory.setProcessMessage(null);
        bizTaxInstanceCategoryService.create(bizTaxInstanceCategory);

        //4 修改deduct
        bizTaxInstanceCategoryDeduct.setState(BizTaxInstanceCategoryDeduct.State.expire);
        bizTaxInstanceCategoryDeductService.create(bizTaxInstanceCategoryDeduct);

        //5 修改日志
        bizTaxInstanceCategoryHistoryService.log(bizTaxInstanceCategory.getId(), processBy, BizTaxInstanceCategoryHistory.Type.deduct, "取消微信支付");


    }
}
