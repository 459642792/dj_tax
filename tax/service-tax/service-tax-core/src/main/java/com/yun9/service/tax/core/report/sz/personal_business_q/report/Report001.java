package com.yun9.service.tax.core.report.sz.personal_business_q.report;

import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategoryPersonalBusinessItem;
import com.yun9.commons.utils.StringUtils;

import java.math.BigDecimal;
import java.util.*;

public class Report001 {

    private Map<String, Object> all = new HashMap<>();

    public Report001() {
        for (int i = 1; i <= 18; i++) {
            all.put("a" + i, new BigDecimal("0.00"));
        }

    }

    public Report001 generate(CalculationValue calculationValue, BizTaxInstanceCategoryPersonalBusinessItem personalBusinessItem,
                              Map<String, String> cardTypes, Map<String, String> countryTypes, Map<String, String> currents) {
        currents.entrySet().stream().forEach((item) -> {
        if ("skssqq".equals(item)) {
            calculationValue.startdata = currents.get(item);
        } else if ("skssqz".equals(item)) {
            calculationValue.enddate = currents.get(item);
        } else if ("yssdl".equals(item)) {
            calculationValue.yssdl = ifNullBigDecimal(currents.get(item));
        } else if ("fpbl".equals(item)) {
            calculationValue.fpbl = ifNullBigDecimal(currents.get(item));
        } else if ("qcyjsdse".equals(item)) {
            calculationValue.qcyjsdse = ifNullBigDecimal(currents.get(item));
        } else if ("xm".equals(item)) {
            calculationValue.xm = currents.get(item);
        } else if ("sfzjlxdm".equals(item)) {
            calculationValue.sfzjlxdm = currents.get(item);
        } else if ("zjhm".equals(item)) {
            calculationValue.zjhm = currents.get(item);
        } else if ("gjDm".equals(item)) {
            calculationValue.gjDm = currents.get(item);
        } else if ("nsrsbh".equals(item)) {
            calculationValue.nsrsbh = currents.get(item);
        } else if ("bdbh".equals(item)) {
            calculationValue.bdbh = currents.get(item);
        } else if ("zszmDm".equals(item)) {
            calculationValue.zszmDm = currents.get(item);
        } else if ("nsrmc".equals(item)) {
            calculationValue.nsrmc = currents.get(item);
        } else if ("gsdjxh".equals(item)) {
            calculationValue.gsdjxh = currents.get(item);
        } else if ("zgswjgDm".equals(item)) {
            calculationValue.zgswjgDm = currents.get(item);
        } else if ("zgswksDm".equals(item)) {
            calculationValue.zgswksDm = currents.get(item);
        } else if ("zsfsDm".equals(item)) {
            calculationValue.zsfsDm = currents.get(item);
        } else if ("zgswjgDm".equals(item)) {
            calculationValue.zgswjgDm = currents.get(item);
        } else if ("zgswjgDm".equals(item)) {
            calculationValue.zgswjgDm = currents.get(item);
        } else if ("zgswjgDm".equals(item)) {
            calculationValue.zgswjgDm = currents.get(item);
        } else if ("djzclxDm".equals(item)) {
            calculationValue.djzclxDm = currents.get(item);
        }
      });

        calculationValue.declarenum = Number(ifNullDouble(calculationValue.enddate.substring(5, 7)) - ifNullDouble(calculationValue.startdata.substring(5, 7)) + 1).intValue();
        Set<String> zsfsdlAry1 = new HashSet<>(Arrays.asList("400", "401", "402", "403", "404", "499", "500", "600", "900"));
        if (zsfsdlAry1.contains(calculationValue.zsfsDm)) { // 核定征收
            calculationValue.zsfsdldm = "1";
            //先默认是按收入征收
            calculationValue.zsfsxldm = "03";
        } else { // 查账征收
            calculationValue.zsfsdldm = "0";
            //默认据实预缴
            calculationValue.zsfsxldm = "01";
        }
        Set<String> lxAry0 = new HashSet<>(Arrays.asList("410", "411", "412", "413"));
        Set<String> lxAry2 = new HashSet<>(Arrays.asList("171", "523"));
        Set<String> lxAry3 = new HashSet<>(Arrays.asList("420", "421", "422", "423", "172", "522"));
        if (lxAry0.contains(calculationValue.djzclxDm)) {
            calculationValue.djzclxDm = "0";
        } else if (lxAry2.contains(calculationValue.djzclxDm)) {
            calculationValue.djzclxDm = "2";
        } else if (lxAry3.contains(calculationValue.djzclxDm)) {
            calculationValue.djzclxDm = "3";
        } else {
            calculationValue.djzclxDm = "1";
        }
        all.put("bdbh", calculationValue.bdbh);
        all.put("xm", calculationValue.xm);
        all.put("sfzjlxmc", cardTypes.get(calculationValue.sfzjlxdm));
        all.put("sfzjlxDm", calculationValue.sfzjlxDm);
        all.put("sfzjhm", calculationValue.sfzjhm);
        all.put("gjdqmc", countryTypes.get(calculationValue.gjDm));
        all.put("gjdqdm", calculationValue.gjdqdm);
        all.put("djxh", personalBusinessItem.getShareholderId());
        all.put("sdxmDm", "0200");
        all.put("grsdssdxmMc", "生产经营所得");
        all.put("zspmDm", "101060200");
        all.put("zszmDm", calculationValue.zszmDm);
        all.put("mc", calculationValue.mc);
        all.put("nsrsbh", calculationValue.nsrsbh);
        all.put("gsdjxh", calculationValue.gsdjxh);
        all.put("zgswjgDm", calculationValue.zgswjgDm);
        all.put("zgswksDm", calculationValue.zgswksDm);
        all.put("zsfsdldm", calculationValue.zsfsdldm);
        all.put("lx", calculationValue.lx);
        all.put("jmsxdm", "");
        all.put("jmxzdm", "");
        all.put("bh", "");
        all.put("jmse", "0.00");
        all.put("zsfsxldm", calculationValue.zsfsxldm);
        all.put("fpbl", calculationValue.fpbl);
        all.put("declarenum", calculationValue.declarenum);
        all.put("yssdl", calculationValue.yssdl);
        all.put("qcyjsdse", calculationValue.qcyjsdse);
        //计算逻辑*********start
        //利润总额
        all.put("a3", Number(parseFloat(ifNullDouble(all.get("a1")) - ifNullDouble(all.get("a2")))));
        //合伙人分配比例
        all.put("a5", (ifNullDouble(calculationValue.fpbl) * 0.01 >= 0 && ifNullDouble(calculationValue.fpbl) * 0.01 <= 1) ? ifNullDouble(calculationValue.fpbl) * 0.01 : 1);

        //投资者减除费用
        all.put("a7", 3500 * calculationValue.declarenum);
        //投资者减除费用及允许扣除的其他费用（7行+8行+9行+10行）
        all.put("a6", Number(parseFloat(ifNullDouble(all.get("a7")) + ifNullDouble(all.get("a8")) + ifNullDouble(all.get("a9")) + ifNullDouble(all.get("a10")))));
        //应税所得率
        all.put("a11", ifNullDouble(calculationValue.yssdl));
        if ("01".equals(calculationValue.zsfsxldm)) {
            //应纳税所得额
            all.put("a12", parseFloat((ifNullDouble(all.get("a3")) - ifNullDouble(all.get("a4"))) * ifNullDouble(all.get("a5")) - ifNullDouble(all.get("a6"))) > 0 ? Number(parseFloat((ifNullDouble(all.get("a1")) - ifNullDouble(all.get("a4"))) * ifNullDouble(all.get("a5")) - ifNullDouble(all.get("a6")))) : 0.00);
        } else if ("03".equals(calculationValue.zsfsxldm)) {
            //应纳税所得额
            all.put("a12", parseFloat(ifNullDouble(all.get("a1")) * ifNullDouble(all.get("a5")) * ifNullDouble(all.get("a16"))) > 0 ? Number(parseFloat(ifNullDouble(all.get("a1")) * ifNullDouble(all.get("a5")) * ifNullDouble(all.get("a16")))) : 0.00);
        } else if ("04".equals(calculationValue.zsfsxldm)) {
            //应纳税所得额
            all.put("a12", Number(parseFloat(parseFloat(parseFloat(ifNullDouble(all.get("a2")) / (1 - ifNullDouble(all.get("a16")))) * ifNullDouble(all.get("a16"))) * ifNullDouble(all.get("a5")))).intValue() > 0 ?
                    Number(parseFloat(parseFloat(parseFloat(ifNullDouble(all.get("a2")) / (1 - ifNullDouble(all.get("a16")))) * ifNullDouble(all.get("a16"))) * ifNullDouble(all.get("a5")))).intValue() :
                    0);
        }
        if (ifNullDouble(all.get("a12")) >= 0 && ifNullDouble(all.get("a12")) <= 15000) {
            //税率
            all.put("a13", 0.05);
            //速算扣除数
            all.put("a14", 0);
        } else if (ifNullDouble(all.get("a12")) > 15000 && ifNullDouble(all.get("a12")) <= 30000) {
            //税率
            all.put("a13", 0.10);
            //速算扣除数
            all.put("a14", 750);
        } else if (ifNullDouble(all.get("a12")) > 30000 && ifNullDouble(all.get("a12")) <= 60000) {
            //税率
            all.put("a13", 0.20);
            //速算扣除数
            all.put("a14", 3750);
        } else if (ifNullDouble(all.get("a12")) > 60000 && ifNullDouble(all.get("a12")) <= 100000) {
            //税率
            all.put("a13", 0.30);
            //速算扣除数
            all.put("a14", 9750);
        } else if (ifNullDouble(all.get("a12")) > 100000) {
            //税率
            all.put("a13", 0.35);
            //速算扣除数
            all.put("a14", 14750);
        }
        //应纳税额
        all.put("a17", ifNullDouble(calculationValue.qcyjsdse));
        all.put("a16", parseFloat(ifNullDouble(all.get("a12")) * ifNullDouble(all.get("a13")) - ifNullDouble(all.get("a14"))) > 0 ? Number(parseFloat(ifNullDouble(all.get("a12")) * ifNullDouble(all.get("a13")) - ifNullDouble(all.get("a14")))) : 0.00);
        //应补退税额
        all.put("a18", parseFloat(ifNullDouble(all.get("a16")) - ifNullDouble(all.get("a16")) - ifNullDouble(all.get("a17"))) > 0 ? Number(parseFloat(ifNullDouble(all.get("a16")) - ifNullDouble(all.get("a16")) - ifNullDouble(all.get("a17")))) : 0.00);

        return this;
    }

    private BigDecimal ifNullBigDecimal(Object obj) {
        if (StringUtils.isEmpty(obj)) {
            return new BigDecimal("0.00");
        }
        return new BigDecimal(obj.toString()).setScale(3,BigDecimal.ROUND_HALF_UP).setScale(2,BigDecimal.ROUND_HALF_UP);
    }

    private Double ifNullDouble(Object obj) {
        if (obj == null) {
            return 0.00;
        }
        return new BigDecimal(obj.toString()).setScale(3,BigDecimal.ROUND_HALF_UP).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    private BigDecimal ifNullBigDecimal(Object obj, double def) {
        if (StringUtils.isEmpty(obj)) {
            return new BigDecimal(def);
        }
        return new BigDecimal(obj.toString());
    }

    private BigDecimal Number(double d) {
        return new BigDecimal(d).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private double parseFloat(double d) {
        return d > 0 ? d : 0.00;
    }


    public Map<String, Object> toMap() {
        return new HashMap<String, Object>() {{
            putAll(all);
        }};
    }

    public static class CalculationValue {

        //营业收入
        public BigDecimal incomeProfitAmount = new BigDecimal("0.00");
        //营业成本
        public BigDecimal costProfitAmount = new BigDecimal("0.00");
        //利润总额
        public BigDecimal profitProfitAmount = new BigDecimal("0.00");
        // 减除费用
        public BigDecimal deductionAmount = new BigDecimal("0.00");

        public int declarenum = 0;
        public String startdata = "";
        public String enddate = "";
        public BigDecimal yssdl = new BigDecimal(0);
        public BigDecimal fpbl = new BigDecimal(0);
        public BigDecimal qcyjsdse = new BigDecimal(0);
        public String zsfsDm = "";
        public String sfzjlxdm = "";
        public String zjhm = "";
        public String gjDm = "";
        public String nsrsbh = "";
        public String bdbh = "";
        public String zszmDm = "";
        public String nsrmc = "";
        public String gsdjxh = "";
        public String zgswjgDm = "";
        public String zgswksDm = "";
        public String djzclxDm = "";
        public String zsfsdldm = "";
        public String zsfsxldm = "";
        public String sfzjlxDm = "";
        public String sfzjhm = "";
        public String mc = "";
        public String lx = "";
        public String xm = "";
        public String gjdqdm = "";

    }
}
