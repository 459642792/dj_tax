package com.yun9.service.tax.core.ft.ops;

import com.alibaba.fastjson.JSON;
import com.yun9.biz.bill.BizBillCollectService;
import com.yun9.biz.bill.BizBillInvoiceService;
import com.yun9.biz.bill.domain.entity.BizBillCollect;
import com.yun9.biz.bill.domain.entity.BizBillInvoice;
import com.yun9.biz.md.BizMdAccountCycleService;
import com.yun9.biz.md.domain.entity.BizMdAccountCycle;
import com.yun9.biz.tax.BizTaxInstanceCategoryVatSmallService;
import com.yun9.biz.tax.BizTaxInstanceService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryVatSmall;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.enums.InvoiceSystem;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.ft.TaxCategoryMapping;
import com.yun9.service.tax.core.ft.handler.AuditHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by werewolf on  2018/6/5.
 */
@TaxCategoryMapping(sn = {TaxSn.q_vat})
public class TaxVatSmallOperation implements AuditHandler {

    public static final Logger logger = LoggerFactory.getLogger(TaxVatSmallOperation.class);
    @Autowired
    BizTaxInstanceService bizTaxInstanceService;

    @Autowired
    BizBillCollectService bizBillCollectService;

    @Autowired
    BizBillInvoiceService bizBillInvoiceService;

    @Autowired
    BizMdAccountCycleService bizMdAccountCycleService;
    @Autowired
    BizTaxInstanceCategoryVatSmallService bizTaxInstanceCategoryVatSmallService;


    @Override
    public boolean isNeedAudit(BizTaxInstanceCategory bizTaxInstanceCategory, BizTaxMdCategory bizTaxMdCategory) {
        return true;
    }

    @Override
    public void audit(BizTaxInstanceCategory bizTaxInstanceCategory) {

        if(null == bizTaxInstanceCategory){
            throw BizTaxException.build(BizTaxException.Codes.IllegalArgumentError,"税种实例不能为空");
        }

        if(null == bizTaxInstanceCategory.getBizTaxInstance()){
            bizTaxInstanceCategory.setBizTaxInstance(bizTaxInstanceService.findById(bizTaxInstanceCategory.getBizTaxInstanceId()));
        }

        //查询会计区间
        List<BizMdAccountCycle> bizMdAccountCycles = bizMdAccountCycleService.findContainsMonth(bizTaxInstanceCategory.getBizTaxInstance().getMdAccountCycleId());
        List<Long> accountCycleIds = Optional.ofNullable(bizMdAccountCycles).orElse(new ArrayList<>()).stream().map(v -> v.getId()).collect(Collectors.toList());



        BizTaxInstanceCategoryVatSmall bizTaxInstanceCategoryVatSmall = Optional.ofNullable(bizTaxInstanceCategoryVatSmallService.findByBizTaxInstanceCategoryId(bizTaxInstanceCategory.getId()))
                .orElseThrow(()->BizTaxException.build(BizTaxException.Codes.BizTaxException,"增值税实例不存在"));
        //修改代开自开无票金额
        HashMap<String,BigDecimal> amounts = bizBillInvoiceService.countAmountByCompanyId(bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId(),accountCycleIds);

        if (StringUtils.isNotEmpty(bizTaxInstanceCategoryVatSmall.getPrepayTaxSource()) && bizTaxInstanceCategoryVatSmall.getPrepayTaxSource().equals(BizTaxInstanceCategoryVatSmall.PrepayTaxSource.agent)) {
            if (amounts.get("service").setScale(2, BigDecimal.ROUND_HALF_UP).compareTo(bizTaxInstanceCategoryVatSmall.getPrepayTaxServiceTotalamount().setScale(2, BigDecimal.ROUND_HALF_UP)) == 1) {
                throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "申报预缴不能大于累计预缴!");
            }
            if (amounts.get("cargo").setScale(2, BigDecimal.ROUND_HALF_UP).compareTo(bizTaxInstanceCategoryVatSmall.getPrepayTaxCargoTotalamount().setScale(2, BigDecimal.ROUND_HALF_UP)) == 1) {
                throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "申报预缴不能大于累计预缴!");
            }
        } else if(StringUtils.isNotEmpty(bizTaxInstanceCategoryVatSmall.getPrepayTaxSource()) && bizTaxInstanceCategoryVatSmall.getPrepayTaxSource().equals(BizTaxInstanceCategoryVatSmall.PrepayTaxSource.taxoffice)){
            if (bizTaxInstanceCategoryVatSmall.getPrepayTaxServiceDeclareamount().setScale(2, BigDecimal.ROUND_HALF_UP).compareTo(bizTaxInstanceCategoryVatSmall.getPrepayTaxServiceTotalamount().setScale(2, BigDecimal.ROUND_HALF_UP)) == 1) {
                throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "申报预缴不能大于累计预缴!");
            }
            if (bizTaxInstanceCategoryVatSmall.getPrepayTaxCargoDeclareamount().setScale(2, BigDecimal.ROUND_HALF_UP).compareTo(bizTaxInstanceCategoryVatSmall.getPrepayTaxCargoTotalamount().setScale(2, BigDecimal.ROUND_HALF_UP)) == 1) {
                throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "申报预缴不能大于累计预缴!");
            }
        } else {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "预缴类型不正确!" + bizTaxInstanceCategoryVatSmall.getPrepayTaxSource());
        }
        /*
        if (bizTaxInstanceCategoryVatSmall.getPrepayTaxServiceTotalamount().compareTo(amounts.get("service"))
                == -1) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "申报预缴不能大于累计预缴!");
        }

        if (bizTaxInstanceCategoryVatSmall.getPrepayTaxCargoTotalamount().compareTo(amounts.get("cargo")) == -1) {
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "申报预缴不能大于累计预缴!");
        }
        */

        //审核自开发票(暂时注释没影响)
//        if (bizTaxInstanceCategory.getBizTaxInstance().getInvoiceSystem() != null &&
//                bizTaxInstanceCategory.getBizTaxInstance().getInvoiceSystem() != InvoiceSystem.none) {
//            BizBillCollect bizBillCollect = bizBillCollectService.findByMdCompanyIdAndMdAccountCycleIds(bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId(),
//                    accountCycleIds);
//            if (bizBillCollect == null){
//                logger.error("发票审核失败,发票采集时间为空");
//                throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "发票审核失败,发票采集时间为空");
//
//            }
//            if(bizBillCollect.getCollectDate() <= bizTaxInstanceCategory.getEndDate()) {
//                logger.error("发票审核失败,发票采集时间小于申报结束时间");
//                throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "发票审核失败,发票采集时间小于申报结束时间");
//            }
//        }

        //审核代开发票
        HashMap<String, Object> agentResult = bizBillInvoiceService.audits(bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId(),
                BizBillInvoice.BillType.agent,
                accountCycleIds);
        if (agentResult != null && !agentResult.get("valid").toString().equals("success")) {
            logger.error("代开发票审核失败:{}"+JSON.toJSONString(agentResult.get("error")));
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "代开发票审核失败："+JSON.toJSONString(agentResult.get("error")));
        }


        //审核自开
        HashMap<String, Object> outPutResult = bizBillInvoiceService.audits(bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId(),
                BizBillInvoice.BillType.output,
                accountCycleIds);
        if (outPutResult != null && !outPutResult.get("valid").toString().equals("success")) {
            logger.error("自开发票审核失败:{}"+JSON.toJSONString(outPutResult.get("error")));
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "自开发票审核失败："+JSON.toJSONString(outPutResult.get("error")));
        }

        //审核无票
        HashMap<String, Object> noBillResult = bizBillInvoiceService.audits(bizTaxInstanceCategory.getBizTaxInstance().getMdCompanyId(),
                BizBillInvoice.BillType.nobill,
                accountCycleIds);
        if (noBillResult != null && !noBillResult.get("valid").toString().equals("success")) {
            logger.error("无票发票审核失败:{}"+JSON.toJSONString(noBillResult.get("error")));
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "无票发票审核失败："+JSON.toJSONString(noBillResult.get("error")));
        }


    }

    @Override
    public void cancelOfAudit(BizTaxInstanceCategory bizTaxInstanceCategory) {

    }


}
