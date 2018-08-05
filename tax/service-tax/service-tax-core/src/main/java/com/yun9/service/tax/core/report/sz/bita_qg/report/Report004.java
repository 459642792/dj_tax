package com.yun9.service.tax.core.report.sz.bita_qg.report;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 固定资产加速折旧(扣除)优惠明细表
 */
public class Report004 {
    private Map<String, Object> all = new HashMap<>();

    public Report004() {
        for (int i = 1; i <= 6; i++) {
            all.put("a" + i, new BigDecimal("0.00"));
            all.put("b" + i, new BigDecimal("0.00"));
            all.put("c" + i, new BigDecimal("0.00"));
            all.put("d" + i, new BigDecimal("0.00"));
            all.put("e" + i, new BigDecimal("0.00"));
        }

    }

    public Report004 generate(Map<String, String> history) {
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
