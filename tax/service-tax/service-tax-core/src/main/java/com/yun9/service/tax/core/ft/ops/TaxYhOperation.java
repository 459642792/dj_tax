package com.yun9.service.tax.core.ft.ops;

import com.yun9.biz.tax.BizTaxInstanceCategoryYhItemService;
import com.yun9.biz.tax.BizTaxInstanceCategoryYhService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryYh;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryYhItem;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.utils.StringUtils;
import com.yun9.service.tax.core.ft.TaxCategoryMapping;
import com.yun9.service.tax.core.ft.handler.AuditHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: putao
 * @Description: TODO
 * @Date: Created in 2018-06-14 14:57
 */
@TaxCategoryMapping(sn = {TaxSn.m_yh})
public class TaxYhOperation implements AuditHandler {
    @Autowired
    BizTaxInstanceCategoryYhService bizTaxInstanceCategoryYhService;

    @Autowired
    BizTaxInstanceCategoryYhItemService bizTaxInstanceCategoryYhItemService;

    @Override
    public boolean isNeedAudit(BizTaxInstanceCategory bizTaxInstanceCategory, BizTaxMdCategory bizTaxMdCategory) {
        return false;
    }

    @Override
    public void audit(BizTaxInstanceCategory bizTaxInstanceCategory) {

//        if (null == bizTaxInstanceCategory){
//            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "该公司无申报实例!");
//        }
//        if (BizTaxInstanceCategory.ProcessState.exception.equals(bizTaxInstanceCategory.getProcessState()) ||
//                BizTaxInstanceCategory.ProcessState.process.equals(bizTaxInstanceCategory.getProcessState())){
//            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "审核失败,税种实例处于异常或办理状态!");
//        }

        BizTaxInstanceCategoryYh bizTaxInstanceCategoryYh = 
                bizTaxInstanceCategoryYhService.findByInstanceCategoryId(bizTaxInstanceCategory.getId());
        if (null == bizTaxInstanceCategoryYh){
            throw BizTaxException.build(BizTaxException.Codes.BizTaxException, "印花税申报实例不存在!");  
        }
      
        if ( !bizTaxInstanceCategoryYh.isEnable()){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "该印花税申报实例不可用!");
        }
        List<BizTaxInstanceCategoryYhItem> itemList = 
                bizTaxInstanceCategoryYhItemService.findByBizTaxInstanceCategoryYhId(bizTaxInstanceCategoryYh.getId());
        if (null != itemList && itemList.size() > 0) {
            itemList.forEach(v -> {
                try{
                    //非空校验
                    checkIfNullAndLessZero(v); 
                    //逻辑校验
                    checkDataCount(v);
                }catch (IllegalAccessException e){
                    BizTaxException
                            .throwException(BizTaxException.Codes.BizTaxException, "校验出错!");
                }
            });
        }else {
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "没有找到印花税申报明细!"); 
        }
                
    }

    @Override
    public void cancelOfAudit(BizTaxInstanceCategory bizTaxInstanceCategory) {
        
    }
    //非空校验
    public static void checkIfNullAndLessZero(BizTaxInstanceCategoryYhItem item) throws IllegalAccessException{
        List<String> errors = new ArrayList<>();
        if (null == item){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "参数为空!");
        }
        //减免代码为空,减免额为0
        if (StringUtils.isEmpty(item.getTaxRemitCode())){
            //减免额只能为0
            if (StringUtils.isNotEmpty(item.getTaxRemitAmount()) && 
                    item.getTaxRemitAmount().compareTo(BigDecimal.ZERO) != 0){
                BizTaxException
                        .throwException(BizTaxException.Codes.BizTaxException, "减免性质代码为空,减免额只能为0!");
            }
        }
        //减免性质代码不为空,减免额大于0
        if (StringUtils.isNotEmpty(item.getTaxRemitCode())){
            if (StringUtils.isEmpty(item.getTaxRemitAmount()) ||
                    item.getTaxRemitAmount().compareTo(BigDecimal.ZERO) != 1){
                BizTaxException
                        .throwException(BizTaxException.Codes.BizTaxException, "减免性质代码不为空,减免额需大于0!");
            }
        }
        Field[] fields = item.getClass().getDeclaredFields();
     
        if (item.getBizTaxInstanceCategoryYhId() == 0 ){
            errors.add("关联印花税id为空");
        }
       /* if (item.getId() == 0){
            errors.add("印花税明细id为空"); 
        }*/
        for (Field f : fields
                ) {
            f.setAccessible(true);
            if (null == f.get(item) || "".equals(f.get(item))){
                switch (f.getName()){
                    case "itemCode":
                        errors.add("征税项目代码为空");
                        break;
                    case "taxBase":
                        errors.add("计税依据为空");
                        break;
                    case "approvAmount":
                        errors.add("核定金额为空");
                        break;
                    case "approvRate":
                        errors.add("核定比率为空");
                        break;
                    case "taxRate":
                        errors.add("适用税率为空");
                        break;
                    case "taxPayAmount":
                        errors.add("本期应纳税(费)额为空");
                        break;
                    case "taxAlreadyPayAmount":
                        errors.add("本期已缴税(费)额为空");
                        break;
                        //case "taxRemitCode":
                        //errors.add("减免性质代码为空");
                    case "taxRemitAmount":
                        errors.add("减免额为空");
                        break;
                    case "taxShouldPayAmount":
                        errors.add("本期应补(退(税(费)额为空");
                        break;
                }
            }
            if (f.get(item) instanceof BigDecimal){
                if (((BigDecimal) f.get(item)).compareTo(BigDecimal.ZERO) == -1){
                    switch (f.getName()){
                        case "taxBase":
                            errors.add("计税依据小于0");
                            break;
                        case "approvAmount":
                            errors.add("核定金额小于0");
                            break;
                        case "approvRate":
                            errors.add("核定比率小于0");
                            break;
                        case "taxRate":
                            errors.add("适用税率小于0");
                            break;
                        case "taxPayAmount":
                            errors.add("本期应纳税(费)额小于0");
                            break;
                        case "taxAlreadyPayAmount":
                            errors.add("本期已缴税(费)额小于0");
                            break;
                        case "taxRemitAmount":
                            errors.add("减免额小于0");
                            break;
                        case "taxShouldPayAmount":
                            errors.add("本期应补(退(税(费)额小于0");
                            break;
                    }
                }
            }
        }
        if (errors.size() > 0){
            String result = "";
            for (String error : errors
                    ) {
                result += error + ",";
            }
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, result);
        }
    }
    //数据逻辑校验
    public static void checkDataCount(BizTaxInstanceCategoryYhItem item){
         /*if (Math.abs(Number(data['f' + str[i]]) - (data['b' + str[i]] * data['e' + str[i]])) > 0.06) {
                        throw Error("印花税应纳税额计算金额错误！f1 = b1 * e1");
                    } else if (Math.abs(data['j' + str[i]] - (data['f' + str[i]] - data['g' + str[i]] - data['i' + str[i]])) > 0.06) {
                        throw Error("印花税应补退税额计算金额错误！j1 = f1 - g1 - i1");
                    }*/
        if (null == item){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "参数为空!");
        }
        //减免额小于应纳税额
        if (item.getTaxRemitAmount().compareTo(item.getTaxPayAmount()) == 1){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "减免额不能大于应纳税额！");
        }
        //应纳税额 - 计税依据 * 适用税率 <= 0.06
        //BigDecimal shouldPay = item.getTaxPayAmount().subtract(item.getTaxBase().multiply(item.getTaxRate()));
        //if (shouldPay.abs().compareTo(new BigDecimal(0.06)) == 1){
        //    BizTaxException
        //            .throwException(BizTaxException.Codes.BizTaxException, "印花税应纳税额计算错误!");
        //}
        //应补退税额 - (应纳税额 - 已交税额 - 减免额) = 0
      
        //BigDecimal result = item.getTaxShouldPayAmount().subtract(shouldRemit);
        //if (result.abs().compareTo(new BigDecimal(0.06)) == 1){
        //    BizTaxException
        //            .throwException(BizTaxException.Codes.BizTaxException, "印花税应补退税额计算错误!");
        //}
        //应纳税额四舍五入验证
        BigDecimal amount = item.getTaxBase().multiply(item.getTaxRate());
        BigDecimal shouldPayTax = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        if (shouldPayTax.compareTo(item.getTaxPayAmount().setScale(2,BigDecimal.ROUND_HALF_UP)) != 0){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "印花税应纳税额(結果"+shouldPayTax+")计算错误!");
        }
        //应补退税额四舍五入验证
        BigDecimal shouldRemit = shouldPayTax.subtract(item.getTaxAlreadyPayAmount()).subtract(item.getTaxRemitAmount());
        BigDecimal ybtse = shouldRemit.setScale(1,BigDecimal.ROUND_HALF_UP);
        if (ybtse.compareTo(item.getTaxShouldPayAmount().setScale(1,BigDecimal.ROUND_HALF_UP)) != 0){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "印花税应补退税额(結果"+ybtse+")计算错误!");
        }
    }
}
