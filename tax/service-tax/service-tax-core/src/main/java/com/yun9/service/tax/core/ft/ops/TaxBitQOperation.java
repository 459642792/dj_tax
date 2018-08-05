package com.yun9.service.tax.core.ft.ops;

import com.yun9.biz.tax.BizTaxInstanceCategoryBitService;
import com.yun9.biz.tax.BizTaxInstanceService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryBit;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.ft.TaxCategoryMapping;
import com.yun9.service.tax.core.ft.handler.AuditHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;

/**
 * @Author: putao
 * @Description: TODO
 * @Date: Created in 2018-06-08 11:39
 */
@TaxCategoryMapping(sn = {TaxSn.q_bit})
public class TaxBitQOperation implements AuditHandler {

    @Autowired
    BizTaxInstanceCategoryBitService bizTaxInstanceCategoryBitService;

    @Autowired
    BizTaxInstanceService bizTaxInstanceService;


    @Override
    public boolean isNeedAudit(BizTaxInstanceCategory bizTaxInstanceCategory, BizTaxMdCategory bizTaxMdCategory) {
        return false;
    }

    @Override
    public void audit(BizTaxInstanceCategory bizTaxInstanceCategory) {
//        if (null == bizTaxInstanceCategory) {
//            throw BizTaxException.build(BizTaxException.Codes.IllegalArgumentError, "税种实例不能为空");
//        }
//        if (BizTaxInstanceCategory.ProcessState.exception.equals(bizTaxInstanceCategory.getProcessState()) ||
//                BizTaxInstanceCategory.ProcessState.process.equals(bizTaxInstanceCategory.getProcessState())){
//            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "审核失败,税种实例处于异常或办理状态!");
//        }

        BizTaxInstanceCategoryBit bizTaxInstanceCategoryBit = Optional.ofNullable(
                bizTaxInstanceCategoryBitService.findByTaxInstanceId(bizTaxInstanceCategory.getBizTaxInstanceId()))
                .orElseThrow(() -> BizTaxException.throwException(BizTaxException.Codes.IllegalArgumentError, "企业所得税不存在"));
        //检查是否为disable
        if (bizTaxInstanceCategoryBit.getDisabled() != 0) {
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "税种状态只能为可用状态");
        }

        //检查A类B类
        checkBitAOrB(bizTaxInstanceCategoryBit);
    }

    @Override
    public void cancelOfAudit(BizTaxInstanceCategory bizTaxInstanceCategory) {

    }

    //检查A类B类
    public static void checkBitAOrB(BizTaxInstanceCategoryBit bizTaxInstanceCategoryBit) {
        List<String> errList = new ArrayList<>();
        //检查A类B类
        if (BizTaxInstanceCategoryBit.Type.A.equals(bizTaxInstanceCategoryBit.getType())) {
            //A类检查数据
            if (bizTaxInstanceCategoryBit.getBuyAmount().compareTo(BigDecimal.ZERO) == -1||
                    bizTaxInstanceCategoryBit.getSaleAmount().compareTo(BigDecimal.ZERO) == -1) {
                errList.add("企税A类营业成本,营业收入不能小于0");
            }
            //A类核定类型为none
            if (!BizTaxInstanceCategoryBit.AuditType.none
                    .equals(bizTaxInstanceCategoryBit.getAuditType())) {
                errList.add("企税A类核定类型只能为none");
            }
           //A类核定应纳税额为0
           // if (bizTaxInstanceCategoryBit.getAuditPaytax().compareTo(BigDecimal.ZERO) != 0){
           //         errList.add("企税A类核定应纳税额只能为0"); 
           // }
            //暂时撤销新需求,等前端做好恢复
            if (StringUtils.isEmpty(bizTaxInstanceCategoryBit.getHighTechnologyCompany())){
                errList.add("企税A类是否高新技术企业标识不能为空!");
            }
            if ( !"N".equals(bizTaxInstanceCategoryBit.getHighTechnologyCompany()) && !"Y".equals(bizTaxInstanceCategoryBit.getHighTechnologyCompany())){
                errList.add("企税A类是否高新技术企业标识只能为(Y/N)");
            }
            if (StringUtils.isEmpty(bizTaxInstanceCategoryBit.getTechnologyAdmissionMatter())){
                errList.add("企税A类是否技术入股递延纳税事项标识不能为空!");
            }
            if (!"N".equals(bizTaxInstanceCategoryBit.getTechnologyAdmissionMatter()) && !"Y".equals(bizTaxInstanceCategoryBit.getTechnologyAdmissionMatter())){
                errList.add("企税A类是否技术入股递延纳税事项标识只能为(Y/N)");
            }
            if (StringUtils.isEmpty(bizTaxInstanceCategoryBit.getTechnologySmallCompany())){
                errList.add("企税A类是否科技型中小企业标识不能为空!");
            }
            if (!"N".equals(bizTaxInstanceCategoryBit.getTechnologySmallCompany()) && !"Y".equals(bizTaxInstanceCategoryBit.getTechnologySmallCompany())){
                errList.add("企税A类是否科技型中小企业标识只能为(Y/N)");
            }
            if (bizTaxInstanceCategoryBit.getEmployeeNumber() < 0){
                errList.add("企税A类期末从业人数不能小于0");
            }
        } else if (BizTaxInstanceCategoryBit.Type.B.equals(bizTaxInstanceCategoryBit.getType())) {
            //B类检查
            if (BizTaxInstanceCategoryBit.AuditType.none.equals(bizTaxInstanceCategoryBit.getAuditType())) {
                errList.add("企税B类核定类型不能为none");
            } else if (BizTaxInstanceCategoryBit.AuditType.cost.equals(bizTaxInstanceCategoryBit.getAuditType())) {
                //只能成本
                if (bizTaxInstanceCategoryBit.getSaleAmount().compareTo(BigDecimal.ZERO) != 0 ) {
                    errList.add("企税B类核定类型为cost营业收入只能为0");
                }
                if (bizTaxInstanceCategoryBit.getBuyAmount().compareTo(BigDecimal.ZERO) == -1) {
                    errList.add("企税B类核定类型为cost营业成本不能小于0");
                }
            } else if (BizTaxInstanceCategoryBit.AuditType.income.equals(bizTaxInstanceCategoryBit.getAuditType())) {
                //只能收入
                if (bizTaxInstanceCategoryBit.getBuyAmount().compareTo(BigDecimal.ZERO) != 0 ) {
                    errList.add("企税B类核定类型为income营业成本只能为0");
                }
                if (bizTaxInstanceCategoryBit.getSaleAmount().compareTo(BigDecimal.ZERO) == -1) {
                    errList.add("企税B类核定类型为income营业收入不能小于0");
                }
            //} else if (BizTaxInstanceCategoryBit.AuditType.paytax.equals(bizTaxInstanceCategoryBit.getAuditType())) {
            //    //只能应纳税额
            //    if (bizTaxInstanceCategoryBit.getBuyAmount().compareTo(BigDecimal.ZERO) != 0  ||
            //            bizTaxInstanceCategoryBit.getSaleAmount().compareTo(BigDecimal.ZERO) != 0 ){
            //        errList.add("企税B类核定类型为paytax营业成本和营业收入只能为0");
            //    }
            //    if (bizTaxInstanceCategoryBit.getAuditPaytax().compareTo(BigDecimal.ZERO) == -1){
            //        errList.add("企税B类核定类型为paytax核定应纳税额不能小于0");
            //    }
            } else {
                errList.add("非法的企税B类核定类型:" + bizTaxInstanceCategoryBit.getAuditType());
            }
            //暂时撤销新需求,等前端做好恢复
            //判断高兴技术企业
            if (StringUtils.isNotEmpty(bizTaxInstanceCategoryBit.getTechnologySmallCompany())){
                errList.add("企税B类无需是否科技型中小企业标识");
            }
            if (StringUtils.isNotEmpty(bizTaxInstanceCategoryBit.getTechnologyAdmissionMatter())){
                errList.add("企税B类无需是否技术入股递延纳税事项标识");
            }
            if (StringUtils.isNotEmpty(bizTaxInstanceCategoryBit.getHighTechnologyCompany())){
                errList.add("企税B类无需是否高新技术企业标识");
            }
            if (bizTaxInstanceCategoryBit.getEmployeeNumber() < 0){
                errList.add("企税B类期末从业人数不能小于0");
            }
        } else {
            errList.add("操作失败,非法的企税类型"+bizTaxInstanceCategoryBit.getType());
        }
        if (errList.size()>0){
            String e = "";
            for (String err : errList ){
                e += err + ",";
            }
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, e);
        }
    }
}
