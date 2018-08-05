package com.yun9.service.tax.core.report.sz.vat_small.report;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by werewolf on  2018/5/23.
 */
public class Report002 implements Serializable {
    private Map<String, BigDecimal> c = new HashMap<>();

    public Report002() {
        for (int i = 1; i < 18; i++) {
            c.put("c" + i, new BigDecimal("0.00"));
        }

    }

    public Report002 generate() {
        return this;
    }

    public Map<String, Object> toMap() {
        return new HashMap<String, Object>() {{
            putAll(c);
        }};
    }
}
