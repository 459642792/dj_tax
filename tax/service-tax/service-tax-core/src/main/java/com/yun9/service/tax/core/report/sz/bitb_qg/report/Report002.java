package com.yun9.service.tax.core.report.sz.bitb_qg.report;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 居民企业参股外国企业信息报告表
 */
public class Report002 {
    private Map<String, BigDecimal> all = new HashMap<>();

    public Report002() {
        for (int i = 1; i <= 18; i++) {
            all.put("a" + i, new BigDecimal("0.00"));
            all.put("b" + i, new BigDecimal("0.00"));
            all.put("c" + i, new BigDecimal("0.00"));
            all.put("d" + i, new BigDecimal("0.00"));
            all.put("e" + i, new BigDecimal("0.00"));
            all.put("f" + i, new BigDecimal("0.00"));
            all.put("g" + i, new BigDecimal("0.00"));
            all.put("h" + i, new BigDecimal("0.00"));
        }

        all.put("a5",null);
        all.put("a6",null);
        all.put("a7",null);
        all.put("a8",null);
        all.put("a9",null);
        all.put("a10",null);
        all.put("a11",null);
        all.put("a12",null);
        all.put("a13",null);
        all.put("a14",null);
        all.put("a15",null);
        all.put("a16",null);
        all.put("a17",null);

        all.put("b5",null);
        all.put("b6",null);
        all.put("b7",null);
        all.put("b8",null);
        all.put("b9",null);
        all.put("b10",null);
        all.put("b11",null);

        all.put("c1",null);
        all.put("c2",null);
        all.put("c3",null);
        all.put("c5",null);
        all.put("c6",null);
        all.put("c7",null);
        all.put("c8",null);
        all.put("c12",null);
        all.put("c13",null);
        all.put("c14",null);
        all.put("c15",null);
        all.put("c16",null);
        all.put("c17",null);

        all.put("d5",null);
        all.put("d6",null);
        all.put("d7",null);
        all.put("d8",null);
        all.put("d9",null);
        all.put("d10",null);
        all.put("d11",null);

        all.put("e5",null);
        all.put("e6",null);
        all.put("e7",null);
        all.put("e8",null);
        all.put("e9",null);
        all.put("e10",null);
        all.put("e11",null);
        all.put("e12",null);
        all.put("e13",null);
        all.put("e14",null);
        all.put("e15",null);
        all.put("e16",null);
        all.put("e17",null);

        all.put("f1",null);
        all.put("f2",null);
        all.put("f3",null);
        all.put("f9",null);
        all.put("f10",null);
        all.put("f11",null);

        all.put("g5",null);
        all.put("g6",null);
        all.put("g7",null);
        all.put("g8",null);
        all.put("g9",null);
        all.put("g10",null);
        all.put("g11",null);

        all.put("h9",null);
        all.put("h10",null);
        all.put("h11",null);

    }

    public Report002 generate() {
        return this;
    }

    public Map<String, Object> toMap() {
        return new HashMap<String, Object>() {{
            putAll(all);
        }};
    }

    public static void main(String[] args) {
//        System.out.println(JSON.toJSONString(new Report001().generate(new Report001.CalculationValue(),null)));
    }
}
