package com.yun9.service.tax.core.ft.ops;

import com.yun9.biz.tax.BizTaxInstanceCategoryFzItemService;
import com.yun9.biz.tax.BizTaxInstanceCategoryFzService;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryFz;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryFzItem;
import com.yun9.biz.tax.domain.entity.properties.BizTaxMdCategory;
import com.yun9.biz.tax.enums.TaxSn;
import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.service.tax.core.ft.TaxCategoryMapping;
import com.yun9.service.tax.core.ft.handler.AuditHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @Author: putao
 * @Description: TODO
 * @Date: Created in 2018-06-11 16:30
 */
@TaxCategoryMapping(sn = {TaxSn.m_fz,TaxSn.q_fz})
public class TaxFzOperation implements AuditHandler {
    
    @Autowired
    BizTaxInstanceCategoryFzService bizTaxInstanceCategoryFzService;

    @Autowired
    BizTaxInstanceCategoryFzItemService bizTaxInstanceCategoryFzItemService;
    //验证
   private static final Map<String,String> params = new HashMap<String, String>(){{
        put("saleAmountVatNormal","增值税(一般增税)");
        put("saleAmountVatFree","增值税(免抵税额)");
        put("saleAmountSoq","消费税");
        put("saleAmountBusiness","营业税");
        put("saleAmountTotal","合计");
        put("taxRate","税率(征收率)");
        put("taxPayAmount","本期应纳税(费)额");
        put("taxRemitAmount","减免额");
        put("taxAlreadyPayAmount","本期已缴税(费)额");
        put("taxShouldPayAmount","本期应补(退(税(费)额");
    }};
   private static final String csjss_zzsfz = "10109_101090101";
   private static final String jyfj_zzsjyfj = "30203_302030100";
   private static final String dfjyfj_zzsdfjyfj = "30216_302160100";
   private static final String csjss_xfsfz = "10109_101090103";
   private static final String jyfj_xfsjyfj = "30203_302030300";
   private static final String dfjyfj_xfsdfjyfj = "30216_302160300";
   private static final Map<String,String> pinlei = new HashMap<String, String>(){{
       put(csjss_zzsfz,"城市维护建设税_市区(增值税附征)");
       put(jyfj_zzsjyfj,"教育费附加_增值税教育费附加");
       put(dfjyfj_zzsdfjyfj,"地方教育附加_增值税地方教育附加");
       put(csjss_xfsfz,"城市维护建设税_市区(消费税附征)");
       put(jyfj_xfsjyfj,"教育费附加_消费税教育费附加");
       put(dfjyfj_xfsdfjyfj,"地方教育附加_消费税地方教育附加");
   }};

    @Override
    public boolean isNeedAudit(BizTaxInstanceCategory bizTaxInstanceCategory, BizTaxMdCategory bizTaxMdCategory) {
        return false;
    }

    @Override
    public void audit(BizTaxInstanceCategory bizTaxInstanceCategory) {
//        if (null == bizTaxInstanceCategory){
//            throw BizTaxException.build(BizTaxException.Codes.IllegalArgumentError,"税种实例为空");
//        }
//        if (BizTaxInstanceCategory.ProcessState.exception.equals(bizTaxInstanceCategory.getProcessState()) ||
//                BizTaxInstanceCategory.ProcessState.process.equals(bizTaxInstanceCategory.getProcessState())){
//            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "审核失败,税种实例处于异常或办理状态!");
//        }
        //todo 蒲涛这边代码biz和service不同步造成程序无法运行，暂时注释
        BizTaxInstanceCategoryFz bizTaxInstanceCategoryFz = bizTaxInstanceCategoryFzService.
                findByBizTaxInstanceCategoryId(bizTaxInstanceCategory.getId());
        if (null == bizTaxInstanceCategoryFz){
            throw BizTaxException.build(BizTaxException.Codes.IllegalArgumentError,"未找到附征税税种实例");
        }
        //判断可用
        if ( !bizTaxInstanceCategoryFz.isEnable()){
            BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "附征税申报实例不可用");
        }
       
        //校验数据明细
        List<BizTaxInstanceCategoryFzItem> itemList = Optional.ofNullable(bizTaxInstanceCategoryFzItemService.
                findByBizTaxInstanceCategoryFzId(bizTaxInstanceCategoryFz.getId())).orElse(null);
        if (null != itemList && itemList.size() > 0){
            
            itemList.forEach(v -> {
                //校验disabled
                //if ( !v.isEnable()){
                //    BizTaxException
                //            .throwException(BizTaxException.Codes.BizTaxException, "该条附征税明细为不可用状态"); 
                //}
                //验证不小于0
                try {
                    checkIfLessZero(v);
                }catch (IllegalAccessException e){
                    BizTaxException
                            .throwException(BizTaxException.Codes.BizTaxException, "校验出错");
                }
                //验证数据规则
                checkFzItemsData(v); 
            });
        }else {
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "未找到附征税征收项目明细");
        }
    }
    //验证数据规则
    public static void checkFzItemsData(BizTaxInstanceCategoryFzItem item){
        /**
         if (Math.abs(data.e1 - (data.a1 + data.b1 + data.c1 + data.d1)) > 0.06) {
         throw Error("地税附征税合计计算金额错误！e1 = a1 + b1 + c1 + d1");
         } else if (Math.abs(data.e2 - (data.a2 + data.b2 + data.c2 + data.d2)) > 0.06) {
         throw Error("地税附征税合计计算金额错误！e2 = a2 + b2 + c2 + d2");
         } else if (Math.abs(data.e3 - (data.a3 + data.b3 + data.c3 + data.d3)) > 0.06) {
         throw Error("地税附征税合计计算金额错误！e3 = a3 + b3 + c3 + d3");
         } else if (Math.abs(data.g1 - (data.e1 * data.f1)) > 0.06) {
         throw Error("地税附征税本期应纳税额计算金额错误！g1 = e1 * f1");
         } else if (Math.abs(data.g2 - (data.e2 * data.f2)) > 0.06) {
         throw Error("地税附征税本期应纳税额计算金额错误！g2 = e2 * f2");
         } else if (Math.abs(data.g3 - (data.e3 * data.f3)) > 0.06) {
         throw Error("地税附征税本期应纳税额计算金额错误！g3 = e3 * f3");
         } else if (Math.abs(data.k1 - (data.g1 - data.i1 - data.j1)) > 0.06) {
         throw Error("地税附征税本期应补退税额计算金额错误！k1 = g1 - i1 - j1");
         } else if (Math.abs(data.k2 - (data.g2 - data.i2 - data.j2)) > 0.06) {
         throw Error("地税附征税本期应补退税额计算金额错误！k1 = g2 - i2 - j2");
         } else if (Math.abs(data.k3 - (data.g3 - data.i3 - data.j3)) > 0.06) {
         throw Error("地税附征税本期应补退税额计算金额错误！k3 = g3 - i3 - j3");
         */
       
        if (null == item){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "校验数据不能为空");
        }
        //计算校验
        String key = item.getItemCode() +"_"+ item.getItemDetailCode();
        //合计校验
        BigDecimal total = item.getSaleAmountVatNormal().setScale(2,BigDecimal.ROUND_HALF_UP).add(item.getSaleAmountVatFree().setScale(2,BigDecimal.ROUND_HALF_UP)).add(item.getSaleAmountSoq().setScale(2,BigDecimal.ROUND_HALF_UP)).add(item.getSaleAmountBusiness().setScale(2,BigDecimal.ROUND_HALF_UP));
        BigDecimal finalTotal = total.setScale(2,BigDecimal.ROUND_HALF_UP);
        //BigDecimal subtract = item.getSaleAmountTotal().subtract(total);
        //int totalState = subtract.abs().compareTo(BigDecimal.valueOf(0.06));
        //if (totalState ==1){
        //    BizTaxException
        //            .throwException(BizTaxException.Codes.BizTaxException, "征收品目"+pinlei.get(key)+":"+ "合计计算金额错误！");
        //}
        if (finalTotal.compareTo(item.getSaleAmountTotal().setScale(2,BigDecimal.ROUND_HALF_UP)) != 0){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "征收品目"+pinlei.get(key)+":"+ "合计计算金额(結果"+finalTotal+")错误！");
        }
        //应纳税额校验
        //BigDecimal taxPay = item.getTaxPayAmount().subtract(item.getSaleAmountTotal().multiply(item.getTaxRate()));
        //int taxPayState = taxPay.abs().compareTo(BigDecimal.valueOf(0.06));
        //if (taxPayState == 1){
        //    BizTaxException
        //            .throwException(BizTaxException.Codes.BizTaxException, "征收品目"+pinlei.get(key)+":"+ "本期应纳税额计算金额错误！");
        //}
        BigDecimal taxPayAount = total.setScale(2,BigDecimal.ROUND_HALF_UP).multiply(item.getTaxRate().setScale(2,BigDecimal.ROUND_HALF_UP));
        BigDecimal finalTaxPay = taxPayAount.setScale(2,BigDecimal.ROUND_HALF_UP);
        if (finalTaxPay.compareTo(item.getTaxPayAmount().setScale(2,BigDecimal.ROUND_HALF_UP)) != 0){
            BizTaxException
                       .throwException(BizTaxException.Codes.BizTaxException, "征收品目"+pinlei.get(key)+":"+ "本期应纳税额计算金额(結果"+finalTaxPay+")错误！");
        }
        //应补退税额检验
        //BigDecimal shouldPay = item.getTaxPayAmount().subtract(item.getTaxRemitAmount()).subtract(item.getTaxAlreadyPayAmount());
        //int shouldPayState = shouldPay.abs().compareTo(BigDecimal.valueOf(0.06));
        //if (shouldPayState == 1){
        //    BizTaxException
        //            .throwException(BizTaxException.Codes.BizTaxException, "征收品目"+pinlei.get(key)+":"+ "本期应补退税额计算金额错误！");
        //}
        BigDecimal shouldPay = taxPayAount.setScale(2,BigDecimal.ROUND_HALF_UP).subtract(item.getTaxRemitAmount().setScale(2,BigDecimal.ROUND_HALF_UP)).subtract(item.getTaxAlreadyPayAmount().setScale(2,BigDecimal.ROUND_HALF_UP));
        BigDecimal finalShouldPay = shouldPay.setScale(2,BigDecimal.ROUND_HALF_UP);
        if (finalShouldPay.compareTo(item.getTaxShouldPayAmount().setScale(2,BigDecimal.ROUND_HALF_UP)) != 0){
            BizTaxException
                       .throwException(BizTaxException.Codes.BizTaxException, "征收品目"+pinlei.get(key)+":"+ "本期应补退税额计算金额(結果"+finalShouldPay+")错误！");
        }
        //减免额小于应纳税额
        if (item.getTaxRemitAmount().compareTo(item.getTaxPayAmount().setScale(2,BigDecimal.ROUND_HALF_UP)) == 1){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "减免额不能大于应纳税额！");
        }
        //0检验
        /**
         "itemCode":"302030100",
         "itemName":"增值税教育费附加"
         "itemCode":"101090101",
         "itemName":"市区（增值税附征）",
         "itemCode":"302160100",
         "itemName":"增值税地方教育附加"
         "itemCode":"302030300",
         "itemName":"消费税教育费附加
         "itemCode":"302160300",
         "itemName":"消费税地方教育附加",
         "itemCode":"101090103",
         "itemName":"市区（消费税附征）
         教育费附加30203
         地方教育附加30216
         城市建设维护10109
         */
        if (csjss_zzsfz.equals(key) || jyfj_zzsjyfj.equals(key) || dfjyfj_zzsdfjyfj.equals(key)){
           //"10109_101090101":城市维护建设税_市区(增值税附征)
           // case "30203_302030100":教育费附加_增值税教育费附加
            // case "30216_302160100":地方教育附加_增值税地方教育附加
            //消费税为0
           if (item.getSaleAmountSoq().setScale(2,BigDecimal.ROUND_HALF_UP).compareTo(BigDecimal.ZERO) != 0 ){
                BizTaxException
                        .throwException(BizTaxException.Codes.BizTaxException, "征收品目"+pinlei.get(key)+":"+ params.get("saleAmountSoq")+"只能为0");
           }
           //营业税为0
            if (item.getSaleAmountBusiness().setScale(2,BigDecimal.ROUND_HALF_UP).compareTo(BigDecimal.ZERO) != 0){
                BizTaxException
                        .throwException(BizTaxException.Codes.BizTaxException, "征收品目"+pinlei.get(key)+":"+ params.get("saleAmountBusiness")+"只能为0");
            }
        }else if (csjss_xfsfz.equals(key) || jyfj_xfsjyfj.equals(key) || dfjyfj_xfsdfjyfj.equals(key)){
            /*case "10109_101090103"://城市维护建设税_市区(消费税附征)
            case "30203_302030300"://教育费附加_消费税教育费附加
            case "30216_302160300"://地方教育附加_消费税地方教育附加*/
            if (item.getSaleAmountVatNormal().setScale(2,BigDecimal.ROUND_HALF_UP).compareTo(BigDecimal.ZERO) != 0){
                
                BizTaxException
                        .throwException(BizTaxException.Codes.BizTaxException, "征收品目"+pinlei.get(key)+":"+ params.get("saleAmountVatNormal")+"只能为0");
            }
            if (item.getSaleAmountVatFree().setScale(2,BigDecimal.ROUND_HALF_UP).compareTo(BigDecimal.ZERO) != 0){
                
                BizTaxException
                        .throwException(BizTaxException.Codes.BizTaxException, "征收品目"+pinlei.get(key)+":"+ params.get("saleAmountVatFree")+"只能为0");
            }
            if (item.getSaleAmountBusiness().setScale(2,BigDecimal.ROUND_HALF_UP).compareTo(BigDecimal.ZERO) != 0){
                 
                BizTaxException.throwException(BizTaxException.Codes.BizTaxException, "征收品目"+pinlei.get(key)+":"+ params.get("saleAmountBusiness")+"只能为0");
            }
        }
        
    }
    //验证数据类型不小于0
    public static void checkIfLessZero(BizTaxInstanceCategoryFzItem item) throws IllegalAccessException{
        if (null == item){
            BizTaxException
                    .throwException(BizTaxException.Codes.BizTaxException, "校验数据不能为空");
        }
        Map<String,BigDecimal> checkData = new HashMap<>();

        Field[] declaredFields = item.getClass().getDeclaredFields();
        for (Field f : declaredFields){
            f.setAccessible(true);
            if (f.get(item) instanceof BigDecimal){
                checkData.put(f.getName(),(BigDecimal)f.get(item));
            }
        }
       
        checkData.keySet().forEach(v -> {
            if (checkData.get(v).compareTo(BigDecimal.ZERO) == -1){
                BizTaxException
                        .throwException(BizTaxException.Codes.BizTaxException, params.get(v)+"不能小于0");
            }
        });

    }

    @Override
    public void cancelOfAudit(BizTaxInstanceCategory bizTaxInstanceCategory) {

    }
   
}
