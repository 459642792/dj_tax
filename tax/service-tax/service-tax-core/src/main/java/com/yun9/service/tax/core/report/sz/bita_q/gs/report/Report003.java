package com.yun9.service.tax.core.report.sz.bita_q.gs.report;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class Report003 {
    private Map<String, Object> all = new HashMap<>();

    public Report003() {
        for (int i = 1; i <= 15; i++) {
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
            all.put("l" + i, new BigDecimal("0.00"));
            all.put("m" + i, new BigDecimal("0.00"));
            all.put("n" + i, new BigDecimal("0.00"));
            all.put("o" + i, new BigDecimal("0.00"));
            all.put("p" + i, new BigDecimal("0.00"));
            all.put("q" + i, new BigDecimal("0.00"));
        }
    }

    public Report003 generate() {
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
