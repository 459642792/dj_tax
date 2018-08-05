package com.yun9.service.tax.core.report.sz.vat_small.report;

import com.yun9.biz.tax.exception.BizTaxException;
import com.yun9.commons.exception.BizException;
import com.yun9.service.tax.core.exception.ServiceTaxException;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by werewolf on  2018/5/23.
 */
@Data
public class Report001 implements Serializable{

    private Map<String, BigDecimal> a = new HashMap<>();
    private Map<String, BigDecimal> b = new HashMap<>();
    private Map<String, BigDecimal> lja = new HashMap<>();
    private Map<String, BigDecimal> ljb = new HashMap<>();

    public Report001() {
        for (int i = 1; i <= 24; i++) {
            a.put("a" + i, new BigDecimal("0.00"));
            b.put("b" + i, new BigDecimal("0.00"));
            lja.put("lja" + i, new BigDecimal("0.00"));
            ljb.put("ljb" + i, new BigDecimal("0.00"));
        }

    }

    public Report001 generate(CalculationValue calculationValue, Map<String, String> history) {
        if (calculationValue.agentAmount.doubleValue()  - calculationValue.agentTotalAmount.doubleValue() > 1) {
            ServiceTaxException.throwException(ServiceTaxException.Codes.agent_error, "代开发票金额(" + calculationValue.agentAmount + ")与税局(" + calculationValue.agentTotalAmount + ")不一致,请检查!");
        }
        this.a.put("a23", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
        this.a.put("a24", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
        this.a.put("b23", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
        this.a.put("b24", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
        //a2(劳务专票金额0.03) = 代开劳务专票金额0.03 + 自开劳务专票金额0.03(TODO 暂无)
        this.a.put("a2", calculationValue.lstAgentAmount).setScale(2, BigDecimal.ROUND_HALF_UP);
        if (calculationValue.personalstate) {
            //a11(劳务未达起征点销售额)(TODO 暂时默认为0)
            this.a.put("a10", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP)); //Number((items.ltOtherAmount || 0.00).fixedTo(2));
        } else {
            //a11(劳务未达起征点销售额)(TODO 暂时默认为0)
            this.a.put("a11", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP)); //Number((items.ltOtherAmount || 0.00).fixedTo(2));
        }
        //a12(劳务其他免税销售额)(TODO 暂时默认为0)
        this.a.put("a12", calculationValue.lodfOtherAmount).setScale(2, BigDecimal.ROUND_HALF_UP);
        //a13(劳务出口免税金额) = 自开劳务免税 + 代开劳务免税(TODO 暂无)
        this.a.put("a13", calculationValue.lnOutputAmount).setScale(2, BigDecimal.ROUND_HALF_UP);
        //a14(劳务税控器开具出口免税金额) = 自开劳务免税 + 代开劳务免税(TODO 暂无)
        this.a.put("a14", calculationValue.lnOutputAmount).setScale(2, BigDecimal.ROUND_HALF_UP);
        //a16(劳务本期劳务应纳税减少征额)(TODO 暂时默认为0)
        this.a.put("a16", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
        if (calculationValue.personalstate) {
            //a19(劳务未达起征点免税额)(TODO 暂时默认为0)
            this.a.put("a18", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
        } else {
            //a19(劳务未达起征点免税额)(TODO 暂时默认为0)
            this.a.put("a19", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
        }
        //a21(劳务本期劳务预缴税额)
        this.a.put("a21", calculationValue.ltPrepaidAmount).setScale(2, BigDecimal.ROUND_HALF_UP);

        //===========================================
        //b2(服务专票金额0.03) = 代开服务专票金额0.03 + 自开服务专票金额0.03(TODO 暂无)

        this.b.put("b2", calculationValue.sstAgentAmount).setScale(2, BigDecimal.ROUND_HALF_UP);
        //b5(服务专票金额0.05) = 代开服务专票金额0.03 + 自开服务专票金额0.05(TODO 暂无)
        this.b.put("b5", calculationValue.ssfAgentAmount).setScale(2, BigDecimal.ROUND_HALF_UP);

        if (calculationValue.personalstate) {
            this.b.put("b10", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
        } else {
            this.b.put("b11", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
        }
        //b12(服务其他免税金额)(TODO 暂时默认为0)
        this.b.put("b12", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));

        //b13(服务出口免税金额) = 自开服务免税 + 代开服务免税(TODO 暂无)
        this.b.put("b13", calculationValue.snOutputAmount).setScale(2, BigDecimal.ROUND_HALF_UP);
        //b14(服务出口免税金额) = 自开服务免税 + 代开服务免税(TODO 暂无)
        this.b.put("b14", calculationValue.snOutputAmount).setScale(2, BigDecimal.ROUND_HALF_UP);
        //b16(服务本期劳务应纳税减少征额)(TODO 暂时默认为0)
        this.b.put("b16", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
        if (calculationValue.personalstate) {
            //b19(服务未达起征点免税额)(TODO 暂时默认为0)
            this.b.put("b18", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
        } else {
            //b19(服务未达起征点免税额)(TODO 暂时默认为0)
            this.b.put("b19", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
        }
        //b21(服务本期服务预缴税额)
        this.b.put("b21", calculationValue.stPrepaidAmount).setScale(2, BigDecimal.ROUND_HALF_UP);

        //if (劳务专票金额0.03 + 自开劳务普票金额0.03(不包含免税发票) + 自开劳务普票金额(0.03) + 不开票劳务0.03金额 + 劳务销售使用过的固定资产金额 + 劳务其他免税金额 + 劳务出口免税金额 > 90000(notTaxAmount)起征点)
        if (this.a.get("a2").add(calculationValue.ltOutputAmount).add(calculationValue.lptAgentAmount).add(calculationValue.lnNobillAmount).add(calculationValue.lfaOtherAmount)
                .add(calculationValue.lodfOtherAmount).add(this.a.get("a13")).compareTo(calculationValue.notTaxAmount) > 0) {
            //a3(劳务普票金额0.03) =  代开劳务专票金额0.03 + 自开劳务专票金额0.03
            this.a.put("a3", (calculationValue.lptAgentAmount.add(calculationValue.ltOutputAmount)).setScale(2, BigDecimal.ROUND_HALF_UP));
            //a7(劳务销售使用过的固定资产) = 直接取值Other;
            this.a.put("a7", calculationValue.lfaOtherAmount.setScale(2, BigDecimal.ROUND_HALF_UP));
            //a1(劳务应征增值税金额) = a2(劳务专票金额0.03) + a3(劳务普票金额0.03) + 不开平收入劳务
            this.a.put("a1", (this.a.get("a2").add(this.a.get("a3")).add(calculationValue.lnNobillAmount)).setScale(2, BigDecimal.ROUND_HALF_UP));

            if (calculationValue.personalstate) {
                //a10(劳务小微企业免税销售额) = 0.00
                this.a.put("a11", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
            } else {
                //a10(劳务小微企业免税销售额) = 0.00
                this.a.put("a10", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
            }
            this.a.put("a9", (this.a.get("a10").add(this.a.get("a11")).add(this.a.get("a12"))).setScale(2, BigDecimal.ROUND_HALF_UP));

            //a15(劳务本期应纳税额) = (a1(劳务应征增值税金额) + a7(劳务销售使用过的固定资产)) * 0.03(税率)
            this.a.put("a15", (this.a.get("a1").add(this.a.get("a17")).multiply(new BigDecimal("0.03"))).setScale(2, BigDecimal.ROUND_HALF_UP));
            if (calculationValue.personalstate) {
                //a18(小微企业免税额) = 0.00
                this.a.put("a19", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
            } else {
                //a18(小微企业免税额) = 0.00
                this.a.put("a18", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
            }
        } else {
            //a1(劳务应征增值税金额) = a2(劳务专票金额0.03)

            this.a.put("a1", this.a.get("a2").setScale(2, BigDecimal.ROUND_HALF_UP));
            if (calculationValue.personalstate) {
                //a11(劳务小微企业免税销售额) = 自开劳务普票金额0.03 + 代开劳务普票金额0.03 + 劳务销售使用过的固定资产 + 劳务不开票
                this.a.put("a11", (calculationValue.ltOutputAmount.add(calculationValue.lptAgentAmount).add(calculationValue.lfaOtherAmount).add(calculationValue.lnNobillAmount)).setScale(2, BigDecimal.ROUND_HALF_UP));
            } else {
                //a10(劳务小微企业免税销售额) = 自开劳务普票金额0.03 + 代开劳务普票金额0.03 + 劳务销售使用过的固定资产 + 劳务不开票
                this.a.put("a10", (calculationValue.ltOutputAmount.add(calculationValue.lptAgentAmount).add(calculationValue.lfaOtherAmount).add(calculationValue.lnNobillAmount)).setScale(2, BigDecimal.ROUND_HALF_UP));
            }
            //a9(劳务免税销售额) = a10(劳务小微企业免税销售额) + a11(劳务未达起征点销售额) + a12(劳务其他免税销售额)
            this.a.put("a9", (this.a.get("a10").add(this.a.get("a11")).add(this.a.get("a12"))).setScale(2, BigDecimal.ROUND_HALF_UP));
            //a15(劳务本期应纳税额) = a1(劳务应征增值税金额) * 0.03(税率)
            this.a.put("a15", (this.a.get("a1").multiply(new BigDecimal("0.03"))).setScale(2, BigDecimal.ROUND_HALF_UP));
            if (calculationValue.personalstate) {
                //a19(劳务小微企业免税额) = a9(劳务免税销售额) * 0.03(税率)

                this.a.put("a19", (this.a.get("a9").multiply(new BigDecimal("0.03"))).setScale(2, BigDecimal.ROUND_HALF_UP));
            } else {
                //a18(劳务小微企业免税额) = a9(劳务免税销售额) * 0.03(税率)
                this.a.put("a18", (this.a.get("a9").multiply(new BigDecimal("0.03"))).setScale(2, BigDecimal.ROUND_HALF_UP));
            }
        }

        //a8(劳务销售使用过的固定资产税控器开具) =  a7(劳务销售使用过的固定资产)
        this.a.put("a8", this.a.get("a7").setScale(2, BigDecimal.ROUND_HALF_UP));
        //a17(劳务本期免税额) = a18(劳务小微企业免税额) + a19(服务未达起征点免税额)

        this.a.put("a17", (this.a.get("a18").add(this.a.get("a19"))).setScale(2, BigDecimal.ROUND_HALF_UP));

        this.a.put("a20", (this.a.get("a15").add(this.a.get("a16"))).setScale(2, BigDecimal.ROUND_HALF_UP));

        if (Math.abs((this.a.get("a20").subtract(this.a.get("a21"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()) <= 0.1) {
            this.a.put("a21", this.a.get("a20").setScale(2, BigDecimal.ROUND_HALF_UP));
        }
        //a22(劳务本期应补（退）税额) = a20(劳务应纳税额合计) - a21(劳务本期劳务预缴税额)
        this.a.put("a22", (this.a.get("a20").subtract(this.a.get("a21"))).setScale(2, BigDecimal.ROUND_HALF_UP));

        //if (服务专票金额0.03 + 自开服务普票金额0.03 + 代开服务普票金额0.03 + 服务专票金额0.05 + 自开服务普票0.05 + 代开服务普票金额0.05 +
        // 不开票服务金额0.03 + 不开票服务金额0.05 + 服务其他免税金额 + 服务出口免税金额 - 服务销售不动产专票 - 服务销售不动产普票>90000(notTaxAmount)

        if (this.b.get("b2") //1000
                .add(calculationValue.stOutputAmount) //2000
                .add(calculationValue.sptAgentAmount) //500
                //1500  + 3000+1000
                .add(this.b.get("b5")).add(calculationValue.sfOutputAmount).add(calculationValue.spfAgentAmount)
                // 4005+ 2007+0+4005
                .add(calculationValue.sntNobillAmount).add(calculationValue.snfNobillAmount).add(this.b.get("b12")).add(this.b.get("b13"))
                //
                .subtract(calculationValue.sresOtherAmount).subtract(calculationValue.srepOtherAmount).compareTo(calculationValue.notTaxAmount) > 0) {

            //b6(服务普票金额0.05) = 自开服务普票金额0.05 + 代开服务普票金额0.05
            this.b.put("b6", (calculationValue.sfOutputAmount.add(calculationValue.spfAgentAmount)).setScale(2, BigDecimal.ROUND_HALF_UP));
            //b3(服务普票金额0.03) = 自开服务普票金额0.03 + 代开服务普票金额0.03
            this.b.put("b3", (calculationValue.stOutputAmount.add(calculationValue.sptAgentAmount)).setScale(2, BigDecimal.ROUND_HALF_UP));
            //b1(服务应征增值税金额0.03) = b2(服务专票金额0.03) + b3(服务普票金额0.03) + 不开票服务金额0.03
            this.b.put("b1", (this.b.get("b2").add(this.b.get("b3").add(calculationValue.sntNobillAmount))).setScale(2, BigDecimal.ROUND_HALF_UP));
            //b4(服务应征增值税金额0.05) = b2(服务专票金额0.05) + b3(服务普票金额0.05) + 不开票服务金额0.05
            this.b.put("b4", (this.b.get("b5").add(this.b.get("b6").add(calculationValue.snfNobillAmount))).setScale(2, BigDecimal.ROUND_HALF_UP));

            if (calculationValue.personalstate) {
                //b11(服务小微企业免税销售额) = 0.00;
                this.b.put("b11", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
            } else {
                //b11(服务小微企业免税销售额) = 0.00;
                this.b.put("b10", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
            }

        } else {
            //b6(服务普票金额0.05) = 0.00;
            this.b.put("b6", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
            //b3(服务普票金额0.03) = 0.00;
            this.b.put("b3", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
            //b1(服务应征增值税金额0.03) = b2(服务专票金额0.03)
            this.b.put("b1", this.b.get("b2").setScale(2, BigDecimal.ROUND_HALF_UP));
            //b4(服务应征增值税金额0.05) = b5(服务专票金额0.05)
            this.b.put("b4", this.b.get("b5").setScale(2, BigDecimal.ROUND_HALF_UP));

            if (calculationValue.personalstate) {
                //b11(服务小微企业免税销售额) = 自开服务普票金额0.03 + 代开服务普票金额0.03 + 不开平服务普票金额0.03 + 不开平服务普票金额0.05 +  自开服务普票金额0.05 + 代开服务普票金额0.05
                this.b.put("b11", calculationValue.stOutputAmount.add(calculationValue.sptAgentAmount).add(calculationValue.sntNobillAmount).add(calculationValue.snfNobillAmount).add(calculationValue.sfOutputAmount).add(calculationValue.spfAgentAmount).setScale(2, BigDecimal.ROUND_HALF_UP));
            } else {
                //b11(服务小微企业免税销售额) = 自开服务普票金额0.03 + 代开服务普票金额0.03 + 不开平服务普票金额0.03 + 不开平服务普票金额0.05 +  自开服务普票金额0.05 + 代开服务普票金额0.05
                this.b.put("b10", calculationValue.stOutputAmount. //2000
                        add(calculationValue.sptAgentAmount.setScale(2, BigDecimal.ROUND_HALF_UP)). //500
                        add(calculationValue.sntNobillAmount.setScale(2, BigDecimal.ROUND_HALF_UP)) //4005
                        .add(calculationValue.snfNobillAmount.setScale(2, BigDecimal.ROUND_HALF_UP)) //2007
                        .add(calculationValue.sfOutputAmount.setScale(2, BigDecimal.ROUND_HALF_UP)) // 3000
                        .add(calculationValue.spfAgentAmount).setScale(2, BigDecimal.ROUND_HALF_UP)); //1000
            }
        }
        //b9(服务免税销售额) = b10(服务小微企业免税销售额) + b11(服务未达起征点销售额) + b12(服务其他免税销售额)
        this.b.put("b9", (this.b.get("b10").add(this.b.get("b11")).add(this.b.get("b12"))).setScale(2, BigDecimal.ROUND_HALF_UP));
        //b15(服务本期应纳税额) = b1(服务应征增值税金额0.03) * 0.03 + b4(服务应征增值税金额0.05) * 0.05
        this.b.put("b15", (this.b.get("b1").multiply(new BigDecimal("0.03")).add(this.b.get("b4").multiply(new BigDecimal("0.05")))).setScale(2, BigDecimal.ROUND_HALF_UP));

        //b18(服务小微企业免税额) = b9(服务免税销售额) * 0.03(税率)
        if (calculationValue.personalstate) {
            if (this.b.get("b11").compareTo(new BigDecimal("0.00")) > 0) {
                this.b.put("b19", (((calculationValue.stOutputAmount.add(calculationValue.sptAgentAmount).add(calculationValue.sntNobillAmount).add(this.b.get("b10")).add(this.b.get("b12"))).multiply(new BigDecimal(0.03))).add((calculationValue.snfNobillAmount).add(calculationValue.sfOutputAmount).add(calculationValue.spfAgentAmount).multiply(new BigDecimal("0.05")))).setScale(2, BigDecimal.ROUND_HALF_UP));

            } else {
                this.b.put("b19", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
            }
        } else {
            if (this.b.get("b10").compareTo(new BigDecimal("0.00")) > 0) {

                BigDecimal _t03 = calculationValue.stOutputAmount.add(calculationValue.sptAgentAmount).add(calculationValue.sntNobillAmount).add(this.b.get("b11")).add(this.b.get("b12"));
                BigDecimal _t05 = calculationValue.snfNobillAmount.add(calculationValue.sfOutputAmount).add(calculationValue.spfAgentAmount);
                this.b.put("b18", (_t03.multiply(new BigDecimal("0.03")).add(_t05.multiply(new BigDecimal("0.05")))).setScale(2, BigDecimal.ROUND_HALF_UP));


            } else {
                this.b.put("b18", new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
            }
        }
        //b17(服务本期免税额) = b18(服务小微企业免税额) + b19(服务未达起征点免税额)

        this.b.put("b17", (this.b.get("b18").add(this.b.get("b19"))).setScale(2, BigDecimal.ROUND_HALF_UP));
        this.b.put("b20", (this.b.get("b15").add(this.b.get("b16"))).setScale(2, BigDecimal.ROUND_HALF_UP));
        if (Math.abs(this.b.get("b20").subtract(this.b.get("b21")).doubleValue()) <= 0.1) {
            this.b.put("b21", this.b.get("b20").setScale(2, BigDecimal.ROUND_HALF_UP));
        }
        //b22(服务本期应补（退）税额) = b20(服务本期应纳税额) - b21(本期服务预缴税额)
        this.b.put("b22", (this.b.get("b20").subtract(this.b.get("b21"))).setScale(2, BigDecimal.ROUND_HALF_UP));

        //历史数据
        for (int i = 1; i <= 22; i++) {
            this.lja.put("lja" + i, (this.a.get("a" + i).add(new BigDecimal(history.get("" + i)))).setScale(2, BigDecimal.ROUND_HALF_UP));
        }
        this.lja.put("lja23", (this.a.get("a23").add(new BigDecimal(history.get("45")))).setScale(2, BigDecimal.ROUND_HALF_UP));
        this.lja.put("lja24", (this.a.get("a24").add(new BigDecimal(history.get("46")))).setScale(2, BigDecimal.ROUND_HALF_UP));

        for (int i = 1; i <= 22; i++) {
            this.ljb.put("ljb" + i, (this.b.get("b" + i).add(new BigDecimal(history.get("" + (i + 22))))).setScale(2, BigDecimal.ROUND_HALF_UP));
        }
        this.ljb.put("ljb23", (this.b.get("b23").add(new BigDecimal(history.get("47")))).setScale(2, BigDecimal.ROUND_HALF_UP));
        this.ljb.put("ljb24", (this.b.get("b24").add(new BigDecimal(history.get("48")))).setScale(2, BigDecimal.ROUND_HALF_UP));
        return this;
    }

    public Map<String, Object> toMap() {
        return new HashMap<String, Object>() {{
            putAll(a);
            putAll(b);
            putAll(lja);
            putAll(ljb);
        }};
    }

    public static class CalculationValue {
        public BigDecimal sstAgentAmount = new BigDecimal("0.00");//代开服务专票0.03
        public BigDecimal sptAgentAmount = new BigDecimal("0.00");//代开服务普票0.03
        public BigDecimal ssfAgentAmount = new BigDecimal("0.00");//代开服务专票0.05
        public BigDecimal spfAgentAmount = new BigDecimal("0.00");//代开服务普票0.05
        public BigDecimal lstAgentAmount = new BigDecimal("0.00");//代开劳务专票0.03
        public BigDecimal lptAgentAmount = new BigDecimal("0.00");//代开劳务普票0.03
        public BigDecimal lnNobillAmount = new BigDecimal("0.00");//不开平收入劳务
        public BigDecimal sntNobillAmount = new BigDecimal("0.00");//不开收入服务0.03
        public BigDecimal snfNobillAmount = new BigDecimal("0.00");//不开收入服务0.05
        public BigDecimal lnOutputAmount = new BigDecimal("0.00");//自开劳务免税
        public BigDecimal ltOutputAmount = new BigDecimal("0.00");//自开劳务普票0.03
        public BigDecimal snOutputAmount = new BigDecimal("0.00");//自开服务免税
        public BigDecimal stOutputAmount = new BigDecimal("0.00");//自开服务普票0.03
        public BigDecimal sfOutputAmount = new BigDecimal("0.00");//自开服务普票0.05
        public BigDecimal ltPrepaidAmount = new BigDecimal("0.00");//劳务预缴税款
        public BigDecimal stPrepaidAmount = new BigDecimal("0.00");//服务预缴税款
        public BigDecimal notTaxAmount = new BigDecimal("90000.00");//起征点
        public BigDecimal lfaOtherAmount = new BigDecimal("0.00");//劳务销售使用过的固定资产(其他)(TODO 暂时默认为0)
        public BigDecimal lodfOtherAmount = new BigDecimal("0.00");//劳务其他免税销售额(其他)(TODO 暂时默认为0)
        public BigDecimal sresOtherAmount = new BigDecimal("0.00");//服务销售不动产专票(其他)(TODO 暂时默认为0)
        public BigDecimal srepOtherAmount = new BigDecimal("0.00");//服务销售不动产普票(其他)(TODO 暂时默认为0)
        public BigDecimal ltOtherAmount = new BigDecimal("0.00");//劳务未达起征点销售额(其他)(TODO 暂时默认为0)
        public BigDecimal stOtherAmount = new BigDecimal("0.00");//服务未达起征点销售额(其他)(TODO 暂时默认为0)
        public BigDecimal lttOtherAmount = new BigDecimal("0.00");//劳务未达起征点免税额(其他)(TODO 暂时默认为0)
        public BigDecimal sttOtherAmount = new BigDecimal("0.00");//服务未达起征点免税额(其他)(TODO 暂时默认为0)
        public BigDecimal ltPrepaidAmounttotal = new BigDecimal("0.00");//累计劳务预缴
        public BigDecimal stPrepaidAmounttotal = new BigDecimal("0.00");//累计服务预缴
        public BigDecimal agentTotalAmount = new BigDecimal(0.00);//税局代开总金额
        public BigDecimal agentAmount = new BigDecimal(0.00);//税局代开总金额
        public boolean labourstate = false;//劳务认定状态
        public boolean servicestate = false;//服务认定状态
        public boolean personalstate = false;//个体户标志
    }

    public static void main(String[] args) {
//        System.out.println(JSON.toJSONString(new Report001().generate(new Report001.CalculationValue(),null)));
    }
}
