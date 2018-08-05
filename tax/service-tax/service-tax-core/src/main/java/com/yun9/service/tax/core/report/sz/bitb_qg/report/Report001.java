package com.yun9.service.tax.core.report.sz.bitb_qg.report;

import com.yun9.commons.utils.DateUtils;
import com.yun9.commons.utils.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 中华人民共和国企业所得税月（季）度预缴和年度纳税申报表（B类，2018年版）
 */
public class Report001 {

    private Map<String, Object> all = new HashMap<>();

    public Report001() {
        for (int i = 1; i <= 17; i++) {
            all.put("a" + i, new BigDecimal("0.00"));
        }
    }

    public Report001 generate(Report001CalculationValue calculationValue,
                              Map<String, String> history, Map<String, String> currents,
                              Map<String, String> clientDataMap) {
        // 营业收入
        all.put("a1", calculationValue.incomeProfitAmount);
        // 减：免税收入（4+5+8+9）
        BigDecimal a3 = ifNullBigDecimal(all.get("a4"))
                .add(ifNullBigDecimal(all.get("a5")))
                .add(ifNullBigDecimal(all.get("a8")))
                .add(ifNullBigDecimal(all.get("a9")));
        // 应税收入额（1-2-3） \ 成本费用总额
        BigDecimal a10 = ifNullBigDecimal(all.get("a1")).subtract(ifNullBigDecimal(all.get("a2")))
                .subtract(ifNullBigDecimal(all.get("a3")));
        if ("3".equals(currents.get("HDZSFS"))) {
            a10 = calculationValue.costProfitAmount;
        }
        all.put("a10", a10);
        //税务机关核定的应税所得率（%）
        all.put("a11", ifNullBigDecimal(currents.get("YSSDL")));
        // 应纳税所得额（第10×11行） \ [第10行÷（1-第11行）×第11行]
        //核定征收方式
        // 选择“核定应税所得率（能核算收入总额的）”的纳税人，本行＝第10×11行。
        // 核定征收方式选择“核定应税所得率（能核算成本费用总额的）”的纳税人，本行＝第10行÷（1-第11行）×第11行
        BigDecimal a12 = ifNullBigDecimal(all.get("a10")).multiply(ifNullBigDecimal(all.get("a11")));
        if ("3".equals(currents.get("HDZSFS"))) {
            a12 = new BigDecimal(ifNullBigDecimal(all.get("a10")).doubleValue()
                    *  new BigDecimal("1").subtract(ifNullBigDecimal(all.get("a11"))).doubleValue()
                    /ifNullBigDecimal(all.get("a11")).doubleValue()).setScale(3, BigDecimal.ROUND_HALF_UP).setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        a12 = notMinus(a12);
        all.put("a12", a12);
        if (((BigDecimal) all.get("a12")).doubleValue() <= 1000000) {
            all.put("smallProfitStatus", "Y");
            // 减：符合条件的小型微利企业减免企业所得税
            all.put("a15", a12.multiply(new BigDecimal("0.15")));
        } else {
            all.put("smallProfitStatus", "N");
            // 减：符合条件的小型微利企业减免企业所得税
            all.put("a15", "0.00");
        }
        // 税率（25%）
        all.put("a13", calculationValue.taxRate);
        // 应纳所得税额（12×13）
        all.put("a14", ifNullBigDecimal(all.get("a12")).multiply(ifNullBigDecimal(all.get("a13"))));
        // 减：实际已缴纳所得税额
        all.put("a16", currents.get("BQYJ"));
        // 本期应补（退）所得税额（14-15-16） \ 税务机关核定本期应纳所得税额
        //核定征收方式选择“核定应税所得率（能核算收入总额的）”“核定应税所得率（能核算成本费用总额的）”的纳税人，
        // 根据相关行次计算结果填报，本行＝第14-15-16行。月（季）度预缴纳税申报时，
        // 当第14-15-16行＜0，本行填0。核定征收方式选择“核定应纳所得税额”的纳税人，
        // 本行填报税务机关核定的本期应纳所得税额（如果纳税人符合小型微利企业条件，
        // 本行填报的金额应为税务机关按照程序调减定额后的本期应纳所得税额）
        BigDecimal a17 = ifNullBigDecimal(all.get("a14"))
                .subtract(ifNullBigDecimal(all.get("a15")))
                .subtract(ifNullBigDecimal(all.get("a16")));
        if ("1".equals(currents.get("HDZSFS"))) {
            a17 = ifNullBigDecimal(currents.get("HDYNSE"));
        }
        all.put("a17", a17.doubleValue() < 0 ? 0 : a17);
        all.put("paytax", all.get("a17"));
        // 核定征收方式
        // 1 核定应纳所得税额 402
        // 2 核定应税所得率（能核算收入总额的） 403
        // 3 核定应税所得率（能核算成本费用总额的） 404
        String hdzsfs = "";
        if ("1".equals(currents.get("HDZSFS"))) {
            hdzsfs = "402";
        } else if ("2".equals(currents.get("HDZSFS"))) {
            hdzsfs = "403";
        } else if ("3".equals(currents.get("HDZSFS"))) {
            hdzsfs = "404";
        }
        all.put("hdzsfs", hdzsfs);
        //小型微利企业 是 y    否  n
        all.put("sfxxwlqy", all.get("smallProfitStatus"));
        // 期末从业人数
        all.put("qmcyrs", calculationValue.employeeNumber);
        // 公司名称
        all.put("companyName", clientDataMap.get("clientFullName"));
        // 税期开始时间
        all.put("taxBeginDate", calculationValue.taxStartDate);
        // 税期结束时间
        all.put("taxEndDate", calculationValue.taxStartDate);
        // 税号
        all.put("taxNo", clientDataMap.get("taxNo"));
        // 税期创建时间
        all.put("taxCreatedDate", DateUtils.currentTimeSecs());
        return this;
    }

    private BigDecimal notMinus(BigDecimal num) {
        if (num.doubleValue() < 0) {
            return new BigDecimal("0.00");
        }
        return num;
    }

    private BigDecimal ifNullBigDecimal(Object obj) {
        if (StringUtils.isEmpty(obj)) {
            return new BigDecimal("0.00");
        }
        return new BigDecimal(obj.toString()).setScale(3, BigDecimal.ROUND_HALF_UP).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private Double ifNullDouble(Object obj) {
        if (obj == null) {
            return 0.00;
        }
        return new BigDecimal(obj.toString()).setScale(3, BigDecimal.ROUND_HALF_UP).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    private BigDecimal Number(double d) {
        return new BigDecimal(d).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private double parseFloat(double d) {
        return d > 0 ? d : 0.00;
    }

    public Map<String, Object> toMap() {
        return new HashMap<String, Object>() {{
            putAll(all);
        }};
    }

    public static class Report001CalculationValue {
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
        //汇缴税率
        public BigDecimal taxRate = new BigDecimal("0.25");
        // 税期开始时间
        public long taxStartDate = System.currentTimeMillis() / 1000;
        // 税期结束时间
        public long taxEndDate = System.currentTimeMillis() / 1000;

        public int employeeNumber = 0;    //期末从业人数

    }
}
