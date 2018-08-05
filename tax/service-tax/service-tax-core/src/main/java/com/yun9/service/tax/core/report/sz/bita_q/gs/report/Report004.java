package com.yun9.service.tax.core.report.sz.bita_q.gs.report;

import com.yun9.commons.utils.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Report004 {
    private Map<String, Object> all = new HashMap<>();

    public Report004() {
        for (int i = 1; i <= 34; i++) {
            all.put("a" + i, new BigDecimal("0.00"));
            all.put("b" + i, new BigDecimal("0.00"));
        }

    }

    public Report004 generate(Map<String, String> history) {
        Map<Integer,Double> historys = new ConcurrentHashMap<>();
        if (history.size() > 0) {
            history.keySet().stream().forEach((item) -> {
                int code = Integer.valueOf(item);
                if (29 < code &&  code < 64) {
                    historys.put(code - 29,StringUtils.isNotEmpty(history.get(item)) ? Double.valueOf(history.get(item)) : 0.00);
                }
            });
        }

        BigDecimal historyVal = null,valInAll = null;
        for (int i = 1; i <= 34; i++) {
            historyVal = historys.get(i) == null ? new BigDecimal("0.00") : new BigDecimal(historys.get(1));
            valInAll = StringUtils.isEmpty(all.get("a"+i)) ? new BigDecimal("0.00") : new BigDecimal(all.get("a"+i).toString());
            all.put("b"+i,historyVal.add(valInAll));
        }

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
