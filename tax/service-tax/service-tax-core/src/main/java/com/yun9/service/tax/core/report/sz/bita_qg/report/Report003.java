package com.yun9.service.tax.core.report.sz.bita_qg.report;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 免税收入、减计收入、所得减免等优惠明细表
 */
public class Report003 {
    private Map<String, Object> all = new HashMap<>();

    public Report003() {
        for (int i = 1; i <= 41; i++) {
            all.put("a" + i, new BigDecimal("0.00"));
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
}
