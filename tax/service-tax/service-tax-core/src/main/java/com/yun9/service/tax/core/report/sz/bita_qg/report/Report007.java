package com.yun9.service.tax.core.report.sz.bita_qg.report;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 技术成果投资入股企业所得税递延纳税备案表
 */
public class Report007 {
    private Map<String, Object> all = new HashMap<>();

    public Report007() {
        for (int i = 1; i <= 7; i++) {
            all.put("a" + i, new BigDecimal("0.00"));
            all.put("b" + i, new BigDecimal("0.00"));
            all.put("c" + i, new BigDecimal("0.00"));
            all.put("d" + i, new BigDecimal("0.00"));
            all.put("e" + i, new BigDecimal("0.00"));
            all.put("f" + i, new BigDecimal("0.00"));
            all.put("g" + i, new BigDecimal("0.00"));
            all.put("h" + i, new BigDecimal("0.00"));
            all.put("i" + i, new BigDecimal("0.00"));
            all.put("j" + i, new BigDecimal("0.00"));
            all.put("k" + i, new BigDecimal("0.00"));
        }

        //A
        all.put("a1", "");
        all.put("a2", "");
        all.put("a3", "");
        all.put("a4", "");
        all.put("a5", "");
        all.put("a6", "");
        //B
        all.put("b1", "");
        all.put("b2", "");
        all.put("b3", "");
        all.put("b4", "");
        all.put("b5", "");
        all.put("b6", "");
        //C
        all.put("c1", "");
        all.put("c2", "");
        all.put("c3", "");
        all.put("c4", "");
        all.put("c5", "");
        all.put("c6", "");
        //D
        all.put("d1", new BigDecimal("0.00"));
        all.put("d2", new BigDecimal("0.00"));
        all.put("d3", new BigDecimal("0.00"));
        all.put("d4", new BigDecimal("0.00"));
        all.put("d5", new BigDecimal("0.00"));
        all.put("d6", new BigDecimal("0.00"));
        //E
        all.put("e1", new BigDecimal("0.00"));
        all.put("e2", new BigDecimal("0.00"));
        all.put("e3", new BigDecimal("0.00"));
        all.put("e4", new BigDecimal("0.00"));
        all.put("e5", new BigDecimal("0.00"));
        all.put("e6", new BigDecimal("0.00"));
        //F
        all.put("f1", "");
        all.put("f2", "");
        all.put("f3", "");
        all.put("f4", "");
        all.put("f5", "");
        all.put("f6", "");
        //G
        all.put("g1", new BigDecimal("0.00"));
        all.put("g2", new BigDecimal("0.00"));
        all.put("g3", new BigDecimal("0.00"));
        all.put("g4", new BigDecimal("0.00"));
        all.put("g5", new BigDecimal("0.00"));
        all.put("g6", new BigDecimal("0.00"));
        //H
        all.put("h1", "");
        all.put("h2", "");
        all.put("h3", "");
        all.put("h4", "");
        all.put("h5", "");
        all.put("h6", "");
        //I
        all.put("i1", "");
        all.put("i2", "");
        all.put("i3", "");
        all.put("i4", "");
        all.put("i5", "");
        all.put("i6", "");
        //J
        all.put("j1", "");
        all.put("j2", "");
        all.put("j3", "");
        all.put("j4", "");
        all.put("j5", "");
        all.put("j6", "");
        //K
        all.put("k1", "");
        all.put("k2", "");
        all.put("k3", "");
        all.put("k4", "");
        all.put("k5", "");
        all.put("k6", "");

        //计算

        all.put("d7", new BigDecimal("0.00"));
        all.put("e7", new BigDecimal("0.00"));
        all.put("g7", new BigDecimal("0.00"));
//        reportData.d7.value = Number(parseFloat((reportData.d1.value || 0.00) + (reportData.d2.value || 0.00) + (reportData.d3.value || 0.00) + (reportData.d4.value || 0.00) + (reportData.d5.value || 0.00) + (reportData.d6.value || 0.00)).fixedTo(2));
//        reportData.e7.value = Number(parseFloat((reportData.e1.value || 0.00) + (reportData.e2.value || 0.00) + (reportData.e3.value || 0.00) + (reportData.e4.value || 0.00) + (reportData.e5.value || 0.00) + (reportData.e6.value || 0.00)).fixedTo(2));
//        reportData.g7.value = Number(parseFloat((reportData.g1.value || 0.00) + (reportData.g2.value || 0.00) + (reportData.g3.value || 0.00) + (reportData.g4.value || 0.00) + (reportData.g5.value || 0.00) + (reportData.g6.value || 0.00)).fixedTo(2));
        //取值******************************************************************end


    }

    public Report007 generate() {
        return this;
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
