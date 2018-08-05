package com.yun9.service.tax.core.report.sz.personal_business_q.report;

import java.util.HashMap;
import java.util.Map;

public class Report002 {
    private Map<String, Object> all = new HashMap<>();

    public Report002() {
        all.put("a1","");
        all.put("a2","");
        all.put("a3","0.00");
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
