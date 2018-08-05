package com.yun9.service.tax.core.report.sz.bita_q.ds.report;

import com.yun9.commons.utils.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Report005 {
    private Map<String, Object> all = new HashMap<>();

    public Report005() {
        for (int i = 1; i <= 30; i++) {
            all.put("a" + i, new BigDecimal("0.00"));
            all.put("b" + i, new BigDecimal("0.00"));
        }
    }

    public Report005 generate(Report001.Report001CalculationValue calculationValue, Map<String, String> history, Map<String, String> currents) {
        Map<Integer,Double> historys = new ConcurrentHashMap<>();
        Map<Integer,Double> historys001 = new ConcurrentHashMap<>();
        if (history.size() > 0) {
            history.keySet().stream().forEach((item) -> {
                int code = Integer.valueOf(item);
                if (code <= 16) {
                    historys001.put(code + 1,Double.valueOf(history.get(item)));
                } else if (110 < code && code < 141) {
                    historys.put(code - 110,Double.valueOf(history.get(item)));
                }
            });
        }

        if (currents.size() > 0) {
            currents.entrySet().stream().forEach((item) -> {
                if ("YQSB".equals(item)) {
                    //小微类型
                    calculationValue.xwtype = StringUtils.isNotEmpty(currents.get(item)) ? Integer.valueOf(currents.get(item)) : 3;;
                } else if ("XXWL_BZ".equals(item)) {
                    //企业类型
                    calculationValue.qytype = StringUtils.isNotEmpty(currents.get(item)) ? Integer.valueOf(currents.get(item)) : 1;
                } else if ("NDMBYQNDKS".equals(item)) {
                    //企业类型
                    calculationValue.recoup = ifNullBigDecimal(currents.get(item),0);
                }
            });
        }

        //取值***************************************************************start
        //计算出来的
        // if (润核算的利润总额货和利润率有取值)
        calculationValue.b4 = Number(parseFloat((ifNullDouble(historys001.get(4))) + ifNullDouble(calculationValue.profitProfitAmount)));
        calculationValue.b5 = ifNullBigDecimal(historys001.get(5));
        calculationValue.b6 = ifNullBigDecimal(historys001.get(6));
        calculationValue.b7 = ifNullBigDecimal(historys001.get(7));
        //a8(弥补以前年度亏损)
        BigDecimal recup = calculationValue.recoup;
        double b8 = (calculationValue.b4.doubleValue() + calculationValue.b5.doubleValue() - calculationValue.b6.doubleValue() - calculationValue.b7.doubleValue()) > 0 ?
                ((calculationValue.b4.doubleValue() + calculationValue.b5.doubleValue() - calculationValue.b6.doubleValue() - calculationValue.b7.doubleValue()) > recup.doubleValue() ? recup.doubleValue() : (calculationValue.b4.doubleValue() + calculationValue.b5.doubleValue() - calculationValue.b6.doubleValue() - calculationValue.b7.doubleValue())) :
                0.00;
        calculationValue.b8  = new BigDecimal(b8);
        double a8 = calculationValue.b8.doubleValue() - ifNullDouble(historys001.get(8));
        calculationValue.b9 = Number(parseFloat((ifNullDouble(calculationValue.b4)) + (ifNullDouble(calculationValue.b5)) - ifNullDouble(calculationValue.b6) - ifNullDouble(calculationValue.b7) - ifNullDouble(calculationValue.b8)));
        calculationValue.a9 = calculationValue.profitProfitAmount;
        String smallProfitStatus = "Y";
        if (calculationValue.xwtype == 4) { //分支机构小微标志默认否，且不让填
            smallProfitStatus = "N";
        } else {
            if (calculationValue.qytype == 1 || calculationValue.qytype == 2 || calculationValue.qytype == 3) {
                smallProfitStatus = "Y";
            }
        }
        //免所得税额
        if (Math.max(calculationValue.a9.doubleValue(), calculationValue.b9.doubleValue()) <= 500000 && smallProfitStatus == "Y") {
            all.put("b2",parseFloat(calculationValue.b9.doubleValue() * 0.15) > 0 ? Number(parseFloat(calculationValue.b9.doubleValue() * 0.15)) : 0.00);
        } else {
            all.put("b2","0.00");
        }
        all.put("a2",Number(parseFloat((ifNullDouble(all.get("b2"))) - (ifNullDouble(historys.get(2))))));
        all.put("a3",Number(parseFloat(ifNullDouble(all.get("a2")))));
        all.put("b3",Number(parseFloat(ifNullDouble(all.get("b2")))));
        all.put("a4",new BigDecimal(0).subtract(Number(parseFloat(ifNullDouble(historys.get(4))))));
        all.put("b4",Number(parseFloat((ifNullDouble(historys.get(4))) + (ifNullDouble(all.get("a4"))))));
        all.put("b5",Number(parseFloat((ifNullDouble(historys.get(5))) + (ifNullDouble(all.get("a5"))))));
        all.put("b6",Number(parseFloat((ifNullDouble(historys.get(6))) + (ifNullDouble(all.get("a6"))))));
        all.put("a6",Number(parseFloat((ifNullDouble(all.get("a7"))) + (ifNullDouble(all.get("a8"))) + (ifNullDouble(all.get("a9"))) + (ifNullDouble(all.get("a10"))) + (ifNullDouble(all.get("a11"))) + (ifNullDouble(all.get("a12"))) + (ifNullDouble(all.get("a13"))) + (ifNullDouble(all.get("a14"))) + (ifNullDouble(all.get("a15"))) + (ifNullDouble(all.get("a16"))) + (ifNullDouble(all.get("a17"))) + (ifNullDouble(all.get("a18"))) + (ifNullDouble(all.get("a19"))) + (ifNullDouble(all.get("a20"))) + (ifNullDouble(all.get("a21"))) + (ifNullDouble(all.get("a22"))) + (ifNullDouble(all.get("a23"))) + (ifNullDouble(all.get("a24"))) + (ifNullDouble(all.get("a25"))) + (ifNullDouble(all.get("a26"))) + (ifNullDouble(all.get("a27"))) + (ifNullDouble(all.get("a28"))) + (ifNullDouble(all.get("a29"))) + (ifNullDouble(all.get("a30"))))));
        all.put("a1",Number(parseFloat((ifNullDouble(all.get("a2"))) + (ifNullDouble(all.get("a4"))) + (ifNullDouble(all.get("a5"))) + (ifNullDouble(all.get("a6"))))));
        all.put("b1",Number(parseFloat((ifNullDouble(all.get("b2"))) + (ifNullDouble(all.get("b4"))) + (ifNullDouble(all.get("b5"))) + (ifNullDouble(all.get("b6"))))));
        all.put("b7",Number(parseFloat((ifNullDouble(historys.get(7))) + (ifNullDouble(all.get("a7"))))));
        all.put("b8",Number(parseFloat((ifNullDouble(historys.get(8))) + (ifNullDouble(all.get("a8"))))));
        all.put("b9",Number(parseFloat((ifNullDouble(historys.get(9))) + (ifNullDouble(all.get("a9"))))));
        all.put("b10",Number(parseFloat((ifNullDouble(historys.get(10))) + (ifNullDouble(all.get("a10"))))));
        all.put("b11",Number(parseFloat((ifNullDouble(historys.get(11))) + (ifNullDouble(all.get("a11"))))));
        all.put("b12",Number(parseFloat((ifNullDouble(historys.get(12))) + (ifNullDouble(all.get("a12"))))));
        all.put("b13",Number(parseFloat((ifNullDouble(historys.get(13))) + (ifNullDouble(all.get("a13"))))));
        all.put("b14",Number(parseFloat((ifNullDouble(historys.get(14))) + (ifNullDouble(all.get("a14"))))));
        all.put("b15",Number(parseFloat((ifNullDouble(historys.get(15))) + (ifNullDouble(all.get("a15"))))));
        all.put("b16",Number(parseFloat((ifNullDouble(historys.get(16))) + (ifNullDouble(all.get("a16"))))));
        all.put("b17",Number(parseFloat((ifNullDouble(historys.get(17))) + (ifNullDouble(all.get("a17"))))));
        all.put("b18",Number(parseFloat((ifNullDouble(historys.get(18))) + (ifNullDouble(all.get("a18"))))));
        all.put("b19",Number(parseFloat((ifNullDouble(historys.get(19))) + (ifNullDouble(all.get("a19"))))));
        all.put("b20",Number(parseFloat((ifNullDouble(historys.get(20))) + (ifNullDouble(all.get("a20"))))));
        all.put("b21",Number(parseFloat((ifNullDouble(historys.get(21))) + (ifNullDouble(all.get("a21"))))));
        all.put("b22",Number(parseFloat((ifNullDouble(historys.get(22))) + (ifNullDouble(all.get("a22"))))));
        all.put("b23",Number(parseFloat((ifNullDouble(historys.get(23))) + (ifNullDouble(all.get("a23"))))));
        all.put("b24",Number(parseFloat((ifNullDouble(historys.get(24))) + (ifNullDouble(all.get("a24"))))));
        all.put("b25",Number(parseFloat((ifNullDouble(historys.get(25))) + (ifNullDouble(all.get("a25"))))));
        all.put("b26",Number(parseFloat((ifNullDouble(historys.get(26))) + (ifNullDouble(all.get("a26"))))));
        all.put("b27",Number(parseFloat((ifNullDouble(historys.get(27))) + (ifNullDouble(all.get("a27"))))));
        all.put("b28",Number(parseFloat((ifNullDouble(historys.get(28))) + (ifNullDouble(all.get("a28"))))));
        all.put("b29",Number(parseFloat((ifNullDouble(historys.get(29))) + (ifNullDouble(all.get("a29"))))));
        all.put("b30",Number(parseFloat((ifNullDouble(historys.get(30))) + (ifNullDouble(all.get("a30"))))));
        //取值*****************************************************************end


        return this;
    }

    private BigDecimal ifNullBigDecimal(Object obj,double def) {
        if (StringUtils.isEmpty(obj)) {
            return new BigDecimal(def);
        }
        return new BigDecimal(obj.toString());
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
            putAll(all);
        }};
    }

    public static void main(String[] args) {
//        System.out.println(JSON.toJSONString(new Report001().generate(new Report001.CalculationValue(),"")));
    }
}
