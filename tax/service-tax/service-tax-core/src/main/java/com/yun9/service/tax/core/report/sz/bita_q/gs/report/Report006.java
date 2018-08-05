package com.yun9.service.tax.core.report.sz.bita_q.gs.report;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class Report006 {
    private Map<String, Object> all = new HashMap<>();

    public Report006() {
        for (int i = 1; i <= 16; i++) {
            all.put("a" + i, new BigDecimal("0.00"));
            all.put("b" + i, new BigDecimal("0.00"));
            all.put("c" + i, new BigDecimal("0.00"));
            all.put("d" + i, new BigDecimal("0.00"));
            all.put("e" + i, new BigDecimal("0.00"));
            all.put("f" + i, new BigDecimal("0.00"));
            all.put("g" + i, new BigDecimal("0.00"));
        }

        //取值***************************************************************start
        all.put("a4","");
        all.put("a5","");
        all.put("a6","");
        all.put("a7","");
        all.put("a8","");
        all.put("a9","");
        all.put("a10","");

        all.put("b1","");
        all.put("b2","");
        all.put("b3","");
        all.put("b4","");
        all.put("b5","");
        all.put("b6","");
        all.put("b7","");
        all.put("b8","");
        all.put("b9","");
        all.put("b10","");

        all.put("c4","");
        all.put("c5","");
        all.put("c6","");
        all.put("c7","");

        all.put("d4","");
        all.put("d5","");
        all.put("d6","");
        all.put("d7","");
        all.put("d4","");
        all.put("d5","");
        all.put("d6","");
        all.put("d7","");
        all.put("d8","");
        all.put("d9","");
        all.put("d10","");

        all.put("e8","");
        all.put("e9","");
        all.put("e10","");
        all.put("e11","");
        all.put("e12","");
        all.put("e13","");
        all.put("e14","");
        all.put("e15","");
        all.put("e16","");

        all.put("f11",new BigDecimal("0.00"));
        all.put("f12",new BigDecimal("0.00"));
        all.put("f13",new BigDecimal("0.00"));
        all.put("f14",new BigDecimal("0.00"));
        all.put("f15",new BigDecimal("0.00"));
        all.put("f16",new BigDecimal("0.00"));

        all.put("g1","");
        all.put("g2","");
        all.put("g3","");
        all.put("g11",new BigDecimal("0.00"));
        all.put("g12",new BigDecimal("0.00"));
        all.put("g13",new BigDecimal("0.00"));
        all.put("g14",new BigDecimal("0.00"));
        all.put("g15",new BigDecimal("0.00"));
        all.put("g16",new BigDecimal("0.00"));
        //取值*****************************************************************end

    }

    public Report006 generate() {
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
