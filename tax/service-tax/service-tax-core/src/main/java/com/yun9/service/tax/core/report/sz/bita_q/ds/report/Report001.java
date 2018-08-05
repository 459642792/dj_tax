package com.yun9.service.tax.core.report.sz.bita_q.ds.report;

import com.alibaba.fastjson.JSON;
import com.yun9.commons.utils.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Report001{

    private Map<String, Object> a = new HashMap<>();

    public Report001() {
        for (int i = 1; i <= 33; i++) {
            a.put("a" + i, new BigDecimal("0.00"));
            a.put("b" + i, new BigDecimal("0.00"));
        }

    }

    public Report001 generate(Report001CalculationValue calculationValue, Map<String, String> history, Map<String, String> currents) {
        Map<Integer,Double> historys = new ConcurrentHashMap<>();
        Map<Integer,Double> historys005 = new ConcurrentHashMap<>();
        if (history.size() > 0) {
            history.keySet().stream().forEach((item) -> {
                int code = Integer.valueOf(item);
                if (code <= 16) {
                    historys.put(code + 1,StringUtils.isNotEmpty(history.get(item)) ? Double.valueOf(history.get(item)) : 0.00);
                } else if (16 < code && code <= 22) {
                    historys.put(code + 2,StringUtils.isNotEmpty(history.get(item)) ? Double.valueOf(history.get(item)) : 0.00);
                } else if (22 < code && code <= 23) {
                    historys.put(code + 3,StringUtils.isNotEmpty(history.get(item)) ? Double.valueOf(history.get(item)) : 0.00);
                } else if (23 < code && code <= 29) {
                    historys.put(code + 4,StringUtils.isNotEmpty(history.get(item)) ? Double.valueOf(history.get(item)) : 0.00);
                } else if (110 < code && code < 141) {
                    historys005.put(code - 110,StringUtils.isNotEmpty(history.get(item)) ? Double.valueOf(history.get(item)) : 0.00);
                }

            });
        }

        if (currents.size() > 0) {
            currents.keySet().stream().forEach((item) -> {
                // "季度汇缴比例"
                if ("HJBLJD".equals(item)) {
                    calculationValue.taxRate = StringUtils.isNotEmpty(currents.get(item)) ? new BigDecimal(currents.get(item)) : new BigDecimal(0.25);
                } else if ("YQSB".equals(item)) {
                    //小微类型
                    calculationValue.xwtype = StringUtils.isNotEmpty(currents.get(item)) ?  Integer.valueOf(currents.get(item)): 3;
                } else if ("XXWL_BZ".equals(item)) {
                    //企业类型
                    calculationValue.qytype = StringUtils.isNotEmpty(currents.get(item)) ? Integer.valueOf(currents.get(item)) : 1;
                } else if ("NDMBYQNDKS".equals(item)) {
                    //弥补以前年度损益
                    calculationValue.recoup = StringUtils.isNotEmpty(currents.get(item)) ? new BigDecimal(currents.get(item)) : new BigDecimal(0);
                } else if ("ZJGYJBL".equals(item)) {
                    //预缴比例
                    calculationValue.prequelRete = StringUtils.isNotEmpty(currents.get(item)) ? new BigDecimal(currents.get(item)) : new BigDecimal(0.25);
                } else if ("YJFSDM".equals(item)) {
                    //预缴类型
                    calculationValue.prequelType = StringUtils.isNotEmpty(currents.get(item)) ? currents.get(item) : "1";
                } else if ("ZFJGLB".equals(item)) {
                    //预缴类型
                    calculationValue.instType = StringUtils.isNotEmpty(currents.get(item)) ? currents.get(item) : "0";
                } else if ("KDSANDKXQFLAG".equals(item)) {
                    //跨地市和跨县区标志
                    calculationValue.spanType = StringUtils.isNotEmpty(currents.get(item)) ? currents.get(item) : "0";
                } else if ("JDFTFZJGBZ".equals(item)) {
                    //跨就地分摊分支机构标志
                    calculationValue.spanInstType = StringUtils.isNotEmpty(currents.get(item)) ? currents.get(item) : "0";
                } else if ("ZYCZYJBL".equals(item)) {
                    //中央财政预缴比例
                    calculationValue.centralRate = StringUtils.isNotEmpty(currents.get(item)) ? new BigDecimal(currents.get(item)) : new BigDecimal(0.25);
                } else if ("FZJGYJBL".equals(item)) {
                    //分支机构预缴比例
                    calculationValue.branchRate = StringUtils.isNotEmpty(currents.get(item)) ? new BigDecimal(currents.get(item)) : new BigDecimal(0.25);
                }

            });
        }

        //判断是否小微企业
        if (calculationValue.xwtype == 4) {
            a.put("smallProfitStatus","N");
        } else {
            if (calculationValue.qytype == 1 || calculationValue.qytype == 2 || calculationValue.qytype == 3) {
                a.put("smallProfitStatus","Y");
            }
        }

        //a2(营业收入)**********************************************************strat
        // if (找不到利润核算的营业收入有取值)
//        if (!"Y".equals(calculationValue.incomestate)) {
//            //a2(营业收入) = 代开收入金额 + 自开收入金额 + 不开平收入金额
//            a.put("a2", calculationValue.agentAmount.add(calculationValue.outputAmount).add(calculationValue.nobillAmount));
//        } else {
        //a2(营业收入) = 利润核算的营业收入
        a.put("a2", calculationValue.incomeProfitAmount);
//        }
        //a2(营业收入)**********************************************************end
        //a3(营业成本)********************************************************strat
        // if (利润核算的营业成本或者成本率有取值)
//        if (calculationValue.costProfitAmount.doubleValue() > 0 || calculationValue.costRate.doubleValue() > 0) {
//            //if (利润核算的成本率有取值)
//            if (calculationValue.costRate.doubleValue() > 0) {
//                //a3(营业成本) = a2(营业收入) * 成本率(利润核算表) + 营业成本(利润核算表)
//                a.put("a3", calculationValue.costProfitAmount);
//            } else {
//                //a3(营业成本) = 营业成本(利润核算表)
//                a.put("a3", calculationValue.costProfitAmount);
//            }
//        } else {
//            //a3(营业成本) = a2(营业收入) * 成本率(客户资料表)
//            a.put("a3", ifNullDouble(a.get("a2")));
//        }
        a.put("a3", calculationValue.costProfitAmount);
        //a3(营业成本)**********************************************************end
        //a4(利润总额)********************************************************start
        // if (润核算的利润总额货和利润率有取值)
//        if (calculationValue.profitProfitAmount.doubleValue() > 0 || calculationValue.profitRate.doubleValue() > 0) {
//            if (calculationValue.profitRate.doubleValue() > 0) {
//                //a4(利润总额) = a2(营业收入) * 利润率(利润核算表) + 利润总额(利润核算表)
//                a.put("a4", calculationValue.profitProfitAmount);
//            } else {
//                //a4(利润总额) = 利润总额(利润核算表)
//                a.put("a4", calculationValue.profitProfitAmount);
//            }
//        } else {
//            //a4(利润总额) = 0.00;
//            a.put("a4", calculationValue.profitProfitAmount);
//            // a.put("a4", 0.00;
//        }
        a.put("a4", calculationValue.profitProfitAmount);
        //a4(利润总额)**********************************************************end

//        a.put("a5", 0.00;
        //减：不征税收入和税基减免应纳税所得额（请填附表1）
//        a.put("a6", 0.00;
        //固定资产加速折旧（扣除）调减额（请填附表2）
//        a.put("a7", 0.00;
        a.put("b2", Number(parseFloat((ifNullDouble(a.get("a2"))) + ifNullDouble(historys.get(2)))));
        a.put("b3", Number(parseFloat((ifNullDouble(a.get("a3"))) + ifNullDouble(historys.get(3)))));
        a.put("b4", Number(parseFloat((ifNullDouble(a.get("a4"))) + ifNullDouble(historys.get(4)))));
        a.put("b5", Number(parseFloat((ifNullDouble(a.get("a5"))) + ifNullDouble(historys.get(5)))));
        a.put("b6", Number(parseFloat((ifNullDouble(a.get("a6"))) + ifNullDouble(historys.get(6)))));
        a.put("b7", Number(parseFloat((ifNullDouble(a.get("a7"))) + ifNullDouble(historys.get(7)))));
        //弥补以前年度损益
        BigDecimal recup = calculationValue.recoup;
        a.put("b8", (ifNullDouble(a.get("b4")) + ifNullDouble(a.get("b5")) - ifNullDouble(a.get("b6")) - ifNullDouble(a.get("b7"))) > 0 ?
                (ifNullDouble(a.get("b4")) + ifNullDouble(a.get("b5")) - ifNullDouble(a.get("b6")) - ifNullDouble(a.get("b7"))) > recup.doubleValue() ? recup : (ifNullDouble(a.get("b4")) + ifNullDouble(a.get("b5")) - ifNullDouble(a.get("b6")) - ifNullDouble(a.get("b7"))) :
                0.00);
        a.put("a8", Number(parseFloat(ifNullDouble(a.get("b8")) - ifNullDouble(historys.get(8)))));
        //实际利润额
        a.put("b9", Number(parseFloat(ifNullDouble(a.get("b4")) + ifNullDouble(a.get("b5")) - ifNullDouble(a.get("b6")) - ifNullDouble(a.get("b7")) - ifNullDouble(a.get("b8")))));
        a.put("a9", Number(parseFloat(ifNullDouble(a.get("a4")) + ifNullDouble(a.get("a5")) - ifNullDouble(a.get("a6")) - ifNullDouble(a.get("a7")) - ifNullDouble(a.get("a8")))));
        //税率
        a.put("a10",new BigDecimal(0.25));
        a.put("b10", new BigDecimal(0.25));
        //应纳所得税额
        a.put("b11", ifNullDouble(a.get("b9")) > 0 ? Number(parseFloat(ifNullDouble(a.get("b9")) * ifNullDouble(a.get("b10")))) : 0.00);
        //TODO 备用
        a.put("a11", ifNullDouble(a.get("a9")) > 0 ? Number(parseFloat(ifNullDouble(a.get("a9")) * ifNullDouble(a.get("a10")))) : 0.00);
        //免所得税额
        if (Math.max(ifNullDouble(a.get("a9")), ifNullDouble(a.get("b9"))) <= 500000 && "Y".equals(a.get("smallProfitStatus"))) {
            a.put("b12", new BigDecimal(ifNullDouble(a.get("b9")) * 0.15 > 0 ? ifNullDouble(a.get("b9")) * 0.15 : 0.00) );
        } else {
            a.put("smallProfitStatus","N");
            a.put("b12",new BigDecimal("0.00"));
        }
        a.put("a12", Number(parseFloat((ifNullDouble(a.get("b12"))) - ifNullDouble(historys.get(12)) - (ifNullDouble(historys005.get(4))))));
        //实际已预缴所得税额
        a.put("b13", Number(parseFloat(ifNullDouble(historys.get(13)))));
        //特定业务预缴（征）所得税额
        a.put("a14",new BigDecimal("0.00"));
        a.put("b14", Number(parseFloat(ifNullDouble(a.get("a14")) + ifNullDouble(historys.get(14)))));
        //应补（退）所得税额
        a.put("b15", new BigDecimal(ifNullDouble(a.get("b11")) - ifNullDouble(a.get("b12")) - ifNullDouble(a.get("b13")) - ifNullDouble(a.get("b14")) > 0 ? ifNullDouble(a.get("b11")) - ifNullDouble(a.get("b12")) - ifNullDouble(a.get("b13")) - ifNullDouble(a.get("b14")) : 0.00));
        //减：以前年度多缴在本期抵缴所得税额 TODO 深圳不能填写
        a.put("a16",new BigDecimal("0.00"));
        a.put("b16", new BigDecimal("0.00"));
        //本月（季）实际应补（退）所得税额
        a.put("b17", new BigDecimal(ifNullDouble(a.get("b15")) - ifNullDouble(a.get("b16")) > 0 ? ifNullDouble(a.get("b15")) - ifNullDouble(a.get("b16")) : 0.00));
        a.put("paytax",a.get("b17"));
        //上一纳税年度应纳税所得额
        a.put("b19", Number(parseFloat(ifNullDouble(historys.get(19)))));
        //本月（季）应纳税所得额（19行×1/4或1/12)
        a.put("a20", Number(parseFloat(ifNullDouble(a.get("b19")) / 4)));
        a.put("b20", Number(parseFloat(ifNullDouble(a.get("a20")) + ifNullDouble(historys.get(20)))));
        //税率
        a.put("a21",new BigDecimal("0.25"));
        a.put("b21", new BigDecimal("0.25"));
        //本月（季）应纳所得税额（20行×21行）
        a.put("a22", Number(parseFloat(ifNullDouble(a.get("a20")) * ifNullDouble(a.get("a21")))));
        a.put("b22", Number(parseFloat(ifNullDouble(a.get("a22")) + ifNullDouble(historys.get(22)))));
        //减：减免所得税额(请填附表3
        a.put("b23", new BigDecimal((ifNullDouble(a.get("b20"))) * 0.15 >= 0 ? (ifNullDouble(a.get("b20"))) * 0.15 : 0.00));
        a.put("a23", Number(parseFloat((ifNullDouble(a.get("b23"))) - ifNullDouble(historys.get(23)))));
        //本月（季）实际应纳所得税额（22行-23行）
        a.put("b24", Number(parseFloat(ifNullDouble(a.get("b22")) - ifNullDouble(a.get("b23")))));
        a.put("a24", Number(parseFloat(ifNullDouble(a.get("b24")) - ifNullDouble(historys.get(24)))));
        //本月（季）税务机关确定的预缴所得税额
        a.put("a26",new BigDecimal("0.00"));
        a.put("b26", Number(parseFloat(ifNullDouble(a.get("a26")) + ifNullDouble(historys.get(26)))));
        //总机构分摊所得税额(15行或24行或26行×总机构分摊预缴比例)
        if ("1".equals(calculationValue.instType) && !"1".equals(calculationValue.spanType) && !"1".equals(calculationValue.spanInstType)) {
            if ("01".equals(calculationValue.prequelType)) {
                // 01：按实预缴 （默认）
                a.put("a28", Number(parseFloat(ifNullDouble(a.get("b15")) * calculationValue.prequelRete.doubleValue())));
            } else if ("04".equals(calculationValue.prequelType) || "05".equals(calculationValue.prequelType) || "06".equals(calculationValue.prequelType)) {
                // 04:按照上一纳税年度应纳税所得额的平均额预缴
                a.put("a28", Number(parseFloat(ifNullDouble(a.get("b24")) * calculationValue.prequelRete.doubleValue())));
            } else if ("99".equals(calculationValue.prequelType)) {
                // 99:按照税务机关确定的其他方法预缴
                a.put("a28", Number(parseFloat(ifNullDouble(a.get("b26")) * calculationValue.prequelRete.doubleValue())));
            }
        }
        a.put("b28", Number(parseFloat(ifNullDouble(a.get("a28")) + ifNullDouble(historys.get(28)))));
        //财政集中分配所得税额
        if ("1".equals(calculationValue.instType) && !"1".equals(calculationValue.spanType)) {
            if ("01".equals(calculationValue.prequelType)) {
                // 01：按实预缴 （默认）
                a.put("a29", Number(parseFloat(ifNullDouble(a.get("b15")) * calculationValue.centralRate.doubleValue())));
            } else if ("04" == calculationValue.prequelType || "05" == calculationValue.prequelType || "06" == calculationValue.prequelType) {
                // 04:按照上一纳税年度应纳税所得额的平均额预缴
                a.put("a29", Number(parseFloat(ifNullDouble(a.get("b24")) * calculationValue.centralRate.doubleValue())));
            } else if ("99" == calculationValue.prequelType) {
                // 99:按照税务机关确定的其他方法预缴
                a.put("a29", Number(parseFloat(ifNullDouble(a.get("b26")) * calculationValue.centralRate.doubleValue())));
            }
        }
        a.put("b29", Number(parseFloat(ifNullDouble(a.get("a29")) + ifNullDouble(historys.get(29)))));
        //分支机构分摊所得税额(15行或24行或26行×分支机构分摊比例)
        if ("1".equals(calculationValue.instType) && !"1".equals(calculationValue.spanType) && !"1".equals(calculationValue.spanInstType)) {
            if ("01".equals(calculationValue.prequelType)) {
                // 01：按实预缴 （默认）
                a.put("a30", Number(parseFloat(ifNullDouble(a.get("b15")) * calculationValue.branchRate.doubleValue())));
            } else if ("04".equals(calculationValue.prequelType) || "05".equals(calculationValue.prequelType) || "06".equals(calculationValue.prequelType)) {
                // 04:按照上一纳税年度应纳税所得额的平均额预缴
                a.put("a30", Number(parseFloat(ifNullDouble(a.get("b24")) * calculationValue.branchRate.doubleValue())));
            } else if ("99".equals(calculationValue.prequelType)) {
                // 99:按照税务机关确定的其他方法预缴
                a.put("a30", Number(parseFloat(ifNullDouble(a.get("b26")) * calculationValue.branchRate.doubleValue())));
            }
        }
        a.put("b30", Number(parseFloat(ifNullDouble(a.get("a30")) + ifNullDouble(historys.get(30)))));
        //其中：总机构独立生产经营部门应分摊所得税额
        a.put("a31",new BigDecimal("0.00"));
        a.put("b31", new BigDecimal("0.00"));
        //分配比例
        a.put("a32",new BigDecimal("0.00"));
        a.put("b32", new BigDecimal("0.00"));
        //分配所得税额
        a.put("a33", Number(parseFloat(ifNullDouble(a.get("a30")) * ifNullDouble(a.get("a32")))));
        a.put("b33", Number(parseFloat(ifNullDouble(a.get("a33")) + ifNullDouble(historys.get(33)))));

        //返回的历史数据
        a.put("historysb12",historys.get(12) != null ? new BigDecimal(historys.get(12)) : new BigDecimal("0.00"));
        a.put("historysb5",historys.get(5) != null ? new BigDecimal(historys.get(5)) : new BigDecimal("0.00"));
        a.put("historys005b4",historys005.get(4) != null ? new BigDecimal(historys005.get(4)) : new BigDecimal("0.00"));
        a.put("historysb8",historys.get(8) != null ? new BigDecimal(historys.get(8)) : new BigDecimal("0.00"));
        a.put("historys14",historys.get(14) != null ? new BigDecimal(historys.get(14)) : new BigDecimal("0.00"));
        a.put("recup",new BigDecimal("0.00"));

        return this;
    }

    private BigDecimal ifNullBigDecimal(Object obj) {
        if (StringUtils.isEmpty(obj)) {
            return new BigDecimal("0.00");
        }
        return new BigDecimal(obj.toString()).setScale(3,BigDecimal.ROUND_HALF_UP).setScale(2,BigDecimal.ROUND_HALF_UP);
    }

    private Double ifNullDouble(Object obj) {
        if (obj == null) {
            return 0.00;
        }
        return new BigDecimal(obj.toString()).setScale(3,BigDecimal.ROUND_HALF_UP).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    private BigDecimal Number(double d) {
        return new BigDecimal(d).setScale(3,BigDecimal.ROUND_HALF_UP).setScale(2,BigDecimal.ROUND_HALF_UP);
    }

    private double parseFloat(double d) {
        return d > 0 ? d : 0.00;
    }

    public Map<String, Object> toMap() {
        return new HashMap<String, Object>() {{
            putAll(a);
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
        //利润率
        public BigDecimal profitRate = new BigDecimal("0.00");
        //汇缴税率
        public BigDecimal taxRate = new BigDecimal("0.25");
        //弥补年度损益
        public BigDecimal recoup = new BigDecimal("0.00");
        //总机构预缴比例
        public BigDecimal prequelRete = new BigDecimal("0.25");
        //中央财政预缴比例
        public BigDecimal centralRate = new BigDecimal("0.25");
        //分支机构预缴比例
        public BigDecimal branchRate = new BigDecimal("0.25");
        //总机构预缴类型
        public String prequelType = "1";
        //总分机构类别
        public String instType = "0";
        //跨地市和跨县区标志
        public String spanType = "0";
        //跨就地分摊分支机构标志
        public String spanInstType = "0";
        //是否使用系统收入
        public String incomestate = "N";
        // 小微类型
        public int xwtype = -1;
        // 企业类型
        public int qytype = -1;

        // report特殊，不知道什么意思的字段
        //实际利润
        public BigDecimal a9 = new BigDecimal("0.00");
        public BigDecimal b4 = new BigDecimal("0.00");
        public BigDecimal b5 = new BigDecimal("0.00");
        public BigDecimal b6 = new BigDecimal("0.00");
        public BigDecimal b7 = new BigDecimal("0.00");
        public BigDecimal b8 = new BigDecimal("0.00");
        public BigDecimal b9 = new BigDecimal("0.00");

    }

    public static void main(String[] args) {
        System.out.println(JSON.toJSONString(new Report001().generate(new Report001.Report001CalculationValue(),new HashMap<>(),new HashMap<>())));
    }
}
