package com.yun9.service.tax.core.report.sz.bitb_q.report;

import com.yun9.commons.utils.StringUtils;
import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
public class Report001{

    private Map<String, Object> a = new HashMap<>();

    public Report001() {
        for (int i = 1; i <= 21; i++) {
            a.put("a" + i, new BigDecimal("0.00"));
        }

    }

    public Report001 generate(CalculationValue calculationValue, Map<String, String> currents) {
        if ("3".equals(currents.get("HDZSFS"))) {
            a.put("type","3");
            a.put("a1",new BigDecimal("0.00"));
            a.put("a2",new BigDecimal("0.00"));
            a.put("a3",new BigDecimal("0.00"));
            a.put("a4",new BigDecimal("0.00"));
            a.put("a5",new BigDecimal("0.00"));
            a.put("a6",new BigDecimal("0.00"));
            a.put("a7",new BigDecimal("0.00"));
            a.put("a8",new BigDecimal("0.00"));
            a.put("a9",new BigDecimal("0.00"));
            a.put("a10",new BigDecimal("0.00"));
            a.put("a11",new BigDecimal("0.00"));
            //成本费用总额
            a.put("a12",calculationValue.costProfitAmount);
            a.put("a13",StringUtils.isEmpty(currents.get("HDYSSDL")) ? (StringUtils.isNotEmpty(currents.get("HDYSSDL")) ? new BigDecimal(currents.get("HDYSSDL")) : new BigDecimal("0.06")) : new BigDecimal("0.06"));
            double a14 = ((BigDecimal)a.get("a12")).doubleValue() / (1 - ((BigDecimal)a.get("a13")).doubleValue())
                    * ((BigDecimal)a.get("a13")).doubleValue() >= 0 ?
                    ((BigDecimal)a.get("a12")).doubleValue()
                            / (1 - ((BigDecimal)a.get("a13")).doubleValue()) * (((BigDecimal)a.get("a13")).doubleValue())
                    : 0.00;
            a.put("a14",a14);
            if (((BigDecimal)a.get("a14")).doubleValue() <= 500000) {
                a.put("smallProfitStatus","Y");
                a.put("a17",((BigDecimal)a.get("a14")).multiply(new BigDecimal(0.15)));
            } else {
                a.put("smallProfitStatus","N");
                a.put("a17",new BigDecimal("0.00"));
            }
            a.put("a15",new BigDecimal(0.25));
            a.put("a16",((BigDecimal)a.get("a14")).multiply(((BigDecimal)a.get("a15"))));
            a.put("a18",a.get("a17"));
            a.put("a19",StringUtils.isNotEmpty(currents.get("YYJSDSE"))? new BigDecimal(currents.get("YYJSDSE")): (StringUtils.isNotEmpty(currents.get("BQYJ"))) ? new BigDecimal(currents.get("BQYJ")) : new BigDecimal("0.00"));
            double a20 = (((BigDecimal)a.get("a16")).doubleValue()
                    - ((BigDecimal)a.get("a17")).doubleValue()  - ((BigDecimal)a.get("a19")).doubleValue() >= 0)
                    ? ((BigDecimal)a.get("a16")).doubleValue()  - ((BigDecimal)a.get("a17")).doubleValue()  - ((BigDecimal)a.get("a19")).doubleValue()  :0.00;
            a.put("a20",a20);
            a.put("paytax",a20);
            a.put("a21",new BigDecimal("0.00"));
        } else if ("2".equals(currents.get("HDZSFS"))) {
            a.put("type","2");
            if (StringUtils.isNotEmpty(calculationValue.incomestate) || !calculationValue.incomestate.equals("Y")) {
                //a2(营业收入) = 代开收入金额 + 自开收入金额 + 不开平收入金额
                a.put("a1",calculationValue.agentAmount.add(calculationValue.outputAmount).add(calculationValue.nobillAmount));
            } else {
                //a2(营业收入) = 利润核算的营业收入
                a.put("a1",calculationValue.incomeProfitAmount);
            }
            //a3(营业成本) = 营业成本(利润核算表)
            a.put("a2",new BigDecimal("0.00"));
            a.put("a4",new BigDecimal("0.00"));
            a.put("a5",new BigDecimal("0.00"));
            a.put("a6",new BigDecimal("0.00"));
            a.put("a7",new BigDecimal("0.00"));
            a.put("a8",new BigDecimal("0.00"));
            a.put("a10",StringUtils.isNotEmpty(currents.get("HDYSSDL")) ? new BigDecimal(currents.get("HDYSSDL")) : new BigDecimal("0.10"));
            BigDecimal a3 = ((BigDecimal)a.get("a4")).add(((BigDecimal)a.get("a5")))
                    .add(((BigDecimal)a.get("a6")))
                    .add(((BigDecimal)a.get("a7")))
                    .add(((BigDecimal)a.get("a8")));
            a.put("a3",a3);
            a.put("a9",((BigDecimal)a.get("a1"))
            .subtract(((BigDecimal)a.get("a2"))
            .subtract(((BigDecimal)a.get("a3")))));
            BigDecimal a11 = ((BigDecimal)a.get("a9")).multiply(((BigDecimal)a.get("a10")));
            a.put("a11",a11.doubleValue() > 0 ? a11 : new BigDecimal("0.00"));
            if (((BigDecimal)a.get("a11")).doubleValue() <= 500000) {
                a.put("smallProfitStatus","Y");
                a.put("a17",((BigDecimal)a.get("a11")).multiply(new BigDecimal(0.15)));
            } else {
                a.put("smallProfitStatus","N");
                a.put("a17",new BigDecimal("0.00"));
            }
            a.put("a12",new BigDecimal("0.00"));
            a.put("a13",new BigDecimal("0.00"));
            a.put("a14",new BigDecimal("0.00"));
            a.put("a15",new BigDecimal(0.25));
            a.put("a16",((BigDecimal)a.get("a11")).multiply(((BigDecimal)a.get("a15"))));
            a.put("a18",a.get("a17"));
            a.put("a19",StringUtils.isNotEmpty(currents.get("YYJSDSE"))? new BigDecimal(currents.get("YYJSDSE")): (StringUtils.isNotEmpty(currents.get("BQYJ"))) ? new BigDecimal(currents.get("BQYJ")) : new BigDecimal("0.00"));
            BigDecimal a20 = ((BigDecimal)a.get("a16"))
                    .subtract(((BigDecimal)a.get("a17")))
                    .subtract(((BigDecimal)a.get("a19")));
            a.put("a20",a20.doubleValue() > 0 ? a20 : new BigDecimal("0.00"));
            a.put("a21",new BigDecimal("0.00"));
        }
        return this;
    }

    public Map<String, Object> toMap() {
        return new HashMap<String, Object>() {{
            putAll(a);
        }};
    }

    public static class CalculationValue {
        //代开总金额
        public BigDecimal agentAmount = new BigDecimal("0.00");
        //自开总金额
        public BigDecimal outputAmount = new BigDecimal("0.00");
        //不开票金额
        public BigDecimal nobillAmount = new BigDecimal("0.00");
        //营业收入
        public BigDecimal incomeProfitAmount = new BigDecimal("0.00");
        //营业成本
        public BigDecimal costProfitAmount = new BigDecimal("0.00");
        //成本率
        public BigDecimal costRate = new BigDecimal("0.00");
        //利润总额
        public BigDecimal profitProfitAmount = new BigDecimal("0.00");
        //利润率
        public BigDecimal profitRate = new BigDecimal("0.00");
        public String incomestate = "N";
        public BigDecimal taxRate = new BigDecimal("0.25");

    }

    public static void main(String[] args) {
//        System.out.println(JSON.toJSONString(new Report001().generate(new Report001.CalculationValue(),null)));
    }
}
