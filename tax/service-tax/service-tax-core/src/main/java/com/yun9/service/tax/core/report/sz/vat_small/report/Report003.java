package com.yun9.service.tax.core.report.sz.vat_small.report;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by werewolf on  2018/5/23.
 */
@Data
public class Report003 implements Serializable {


    private Map<String, BigDecimal> C = new HashMap<>();
    private Map<String, BigDecimal> D = new HashMap<>();
    private Map<String, BigDecimal> E = new HashMap<>();
    private Map<String, BigDecimal> F = new HashMap<>();
    private Map<String, BigDecimal> G = new HashMap<>();

    public Report003() {
        for (int i = 1; i < 17; i++) {
            C.put("C" + i, new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
            D.put("D" + i, new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
            E.put("E" + i, new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
            F.put("F" + i, new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
            G.put("G" + i, new BigDecimal("0.00").setScale(2, BigDecimal.ROUND_HALF_UP));
        }
    }


    public Report003 generate(BigDecimal FB4JZEQCYE) {
        this.C.put("C2", FB4JZEQCYE.setScale(2, BigDecimal.ROUND_HALF_UP));
        this.C.put("C1", this.C.get("C1").add(this.C.get("C2")).add(this.C.get("C3")).add(this.C.get("C4")).add(this.C.get("C5")).add(this.C.get("C6")).setScale(2, BigDecimal.ROUND_HALF_UP));
        this.D.put("D1", this.D.get("D1").add(this.D.get("D2")).add(this.D.get("D3")).add(this.D.get("D4")).add(this.D.get("D5")).add(this.D.get("D6")).setScale(2, BigDecimal.ROUND_HALF_UP));
        this.E.put("E1", this.E.get("E1").add(this.E.get("E2")).add(this.E.get("E3")).add(this.E.get("E4")).add(this.E.get("E5")).add(this.E.get("E6")).setScale(2, BigDecimal.ROUND_HALF_UP));
        this.F.put("F1", this.F.get("F1").add(this.F.get("F2")).add(this.F.get("F3")).add(this.F.get("F4")).add(this.F.get("F5")).add(this.F.get("F6")).setScale(2, BigDecimal.ROUND_HALF_UP));
        this.G.put("G1", this.G.get("G1").add(this.G.get("G2")).add(this.G.get("G3")).add(this.G.get("G4")).add(this.G.get("G5")).add(this.G.get("G6")).setScale(2, BigDecimal.ROUND_HALF_UP));
        return this;
    }

    public Map<String, Object> toMap() {
        return new HashMap() {{
            putAll(C);
            putAll(D);
            putAll(E);
            putAll(F);
            putAll(G);
        }};
    }

}
