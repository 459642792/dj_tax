package com.yun9.service.tax.core.report.sz.bita_qg.report;

import com.yun9.commons.utils.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 减免所得税优惠明细表
 */
public class Report005 {
    private Map<String, Object> all = new HashMap<>();

    public Report005() {
        for (int i = 1; i <= 30; i++) {
            all.put("a" + i, new BigDecimal("0.00"));
        }
    }

    public Report005 generate(Report001.Report001CalculationValue calculationValue, Map<String, String> history, Map<String, String> currents) {
        all.put("jmLx","");
        all.put("jmFd","0.00");
        return this;
    }

    private BigDecimal ifNullBigDecimal(Object obj) {
        return ifNullBigDecimal(obj,0.00);
    }

    private BigDecimal ifNullBigDecimal(Object obj,double def) {
        if (StringUtils.isEmpty(obj)) {
            return new BigDecimal(def);
        }
        return new BigDecimal(obj.toString());
    }

    private Double ifNullDouble(Object obj) {
        if (obj == null) {
            return 0.00;
        }
        return Double.valueOf(obj.toString());
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
}
