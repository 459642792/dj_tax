package com.yun9.service.tax.core.report.sz.bita_qg.report;

import com.yun9.commons.utils.DateUtils;
import com.yun9.commons.utils.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 中华人民共和国企业所得税月（季）度预缴纳税申报表（A类）
 */
public class Report001 {

    private Map<String, Object> all = new HashMap<>();

    public Report001() {
        for (int i = 1; i <= 21; i++) {
            all.put("a" + i, new BigDecimal("0.00"));
        }
    }

    public Report001 generate(Report001CalculationValue calculationValue,
                              Map<String, String> currents, Map<String, String> clientDataMap,
                              Map<String,Object> report005) {

        // 总机构分摊比例 zjgftbl
        calculationValue.zjgftbl = ifNullBigDecimal(currents.get("ZJGFTBL"));
        // 财政集中分配比例  czjzfpbl
        calculationValue.czjzfpbl = ifNullBigDecimal(currents.get("ZJGCZJZFPBL"));
        // 全部分支机构分摊比例  qbfzjgftbl
        calculationValue.qbfzjgftbl = ifNullBigDecimal(currents.get("FZJGFTBL"));
        //总机构具有主体生产经营职能部门分摊比例  zjgjyztscjyznbmftbl
        calculationValue.zjgjyztscjyznbmftbl = ifNullBigDecimal(currents.get("SCJYBMBL"));

        // 营业收入
        all.put("a1", calculationValue.incomeProfitAmount);
        // 营业成本
        all.put("a2", calculationValue.costProfitAmount);
        // 利润总额
        all.put("a3", calculationValue.profitProfitAmount);

        // 加：特定业务计算的应纳税所得额
        all.put("sqa4", currents.get("SQTDYWJSDYNSSDELJ"));
        all.put("a4", all.get("sqa4"));
        // 14  减：特定业务预缴（征）所得税额的本年上期申报的金额
        all.put("sqa14", currents.get("SQTDYWYJZSDSELJ"));
        all.put("a14", all.get("sqa14"));

        // 就地预缴比例
        all.put("jdyjbl", new BigDecimal("0.00"));
        // 总机构分摊比例 zjgftbl
        all.put("zjgftbl", new BigDecimal("0.00"));
        // 财政集中分配比例
        all.put("czjzfpbl", new BigDecimal("0.00"));
        // 全部分支机构分摊比例
        all.put("qbfzjgftbl", new BigDecimal("0.00"));
        // 总机构具有主体生产经营职能部门分摊比例
        all.put("zjgjyztscjyznbmftbl", new BigDecimal("0.00"));

        // 减：弥补以前年度亏损,(A3+A4-A5-A6-A7<=0)?0:A8
        BigDecimal a8 = ifNullBigDecimal(all.get("a3")).add(ifNullBigDecimal(all.get("a4")))
                .subtract(ifNullBigDecimal(all.get("a5"))).subtract(ifNullBigDecimal(all.get("a6")))
                .subtract(ifNullBigDecimal(all.get("a7")));
        a8 = a8.doubleValue() > 0 ? ifNullBigDecimal(currents.get("MBNDKS")) : new BigDecimal("0.00");
        a8 = notMinus(a8);
        all.put("a8", a8);

        // 实际利润额（3+4-5-6-7-8） \ 按照上一纳税年度应纳税所得额平均额确定的应纳税所得额  YJFS.equals('AZSJLREYJ')?(A3+A4-A5-A6-A7-A8):A9
        BigDecimal a9 = ifNullBigDecimal(all.get("a3")).add(ifNullBigDecimal(all.get("a4")))
                .subtract(ifNullBigDecimal(all.get("a5")))
                .subtract(ifNullBigDecimal(all.get("a6")))
                .subtract(ifNullBigDecimal(all.get("a7")))
                .subtract(ifNullBigDecimal(all.get("a8")));
        all.put("a9", a9.setScale(3, BigDecimal.ROUND_HALF_UP).setScale(2, BigDecimal.ROUND_HALF_UP));
        // 税率(25%)
        all.put("a10", calculationValue.taxRate);
        // 应纳所得税额（9×10
        BigDecimal a11 = ifNullBigDecimal(all.get("a9")).multiply(ifNullBigDecimal(all.get("a10"))).setScale(3, BigDecimal.ROUND_HALF_UP).setScale(2, BigDecimal.ROUND_HALF_UP);
        a11 = notMinus(a11);
        all.put("a11", a11);
        // 减免所得税额
        BigDecimal a12 = notMinus(new BigDecimal(a9.doubleValue() * 0.15)).setScale(3, BigDecimal.ROUND_HALF_UP).setScale(2, BigDecimal.ROUND_HALF_UP);
        all.put("a12",a12);
        report005.put("a1",all.get("a12"));
        report005.put("a30",report005.get("a1"));
        //13  实际已预缴所得税额
        all.put("a13", currents.get("YJJE"));
        // 本期应补（退）所得税额（11-12-13-14） \ 税务机关确定的本期应纳所得税额
        BigDecimal a15 = ifNullBigDecimal(all.get("a11")).subtract(ifNullBigDecimal(all.get("a12")))
                .subtract(ifNullBigDecimal(all.get("a13")))
                .subtract(ifNullBigDecimal(all.get("a14")));
        a15 = notMinus(a15).setScale(3, BigDecimal.ROUND_HALF_UP).setScale(2, BigDecimal.ROUND_HALF_UP);
        all.put("a15", a15);
        calculationValue.ynsdse = new BigDecimal(all.get("a15").toString());
        all.put("paytax", calculationValue.ynsdse);
        if ("1".equals(currents.get("SBQYLX"))) {
            // 总机构本期分摊应补（退）所得税额（17+18+19）
            BigDecimal a16 = ifNullBigDecimal(all.get("a17")).add(ifNullBigDecimal(all.get("a18")))
                    .add(ifNullBigDecimal(all.get("a19")));
            all.put("a16", a16.setScale(3, BigDecimal.ROUND_HALF_UP).setScale(2, BigDecimal.ROUND_HALF_UP));
            // 其中：总机构分摊应补（退）所得税额（15×总机构分摊比例__%）
            BigDecimal a17 = ifNullBigDecimal(all.get("a15")).multiply(calculationValue.zjgftbl);
            all.put("a17", a17.setScale(3, BigDecimal.ROUND_HALF_UP).setScale(2, BigDecimal.ROUND_HALF_UP));
            // 财政集中分配应补（退）所得税额（15×财政集中分配比例__%）
            BigDecimal a18 = ifNullBigDecimal(all.get("a15")).multiply(calculationValue.czjzfpbl);
            all.put("a18", a18.setScale(3, BigDecimal.ROUND_HALF_UP).setScale(2, BigDecimal.ROUND_HALF_UP));
            // 总机构具有主体生产经营职能的部门分摊所得税额（15×全部分支机构分摊比例__%×总机构具有主体生产经营职能部门分摊比例__%)
            BigDecimal a19 = ifNullBigDecimal(all.get("a15")).multiply(calculationValue.qbfzjgftbl).multiply(calculationValue.zjgjyztscjyznbmftbl);
            all.put("a19", a19.setScale(3, BigDecimal.ROUND_HALF_UP).setScale(2, BigDecimal.ROUND_HALF_UP));

        } else if ("2".equals(currents.get("SBQYLX"))) {
            all.put("a20",ifNullBigDecimal(currents.get("FZJGFTBL")));
        }
        all.put("smallProfitStatus", "Y");
        if ("2".equals(currents.get("SBQYLX")) || "Y".equals(currents.get("BYHBZ"))) {
            all.put("smallProfitStatus", "N");
        } else {
            if ("1".equals(currents.get("XXWLBZ"))) {
                if (("1".equals(currents.get("YJFS")) || "2".equals(currents.get("YJFS"))) && calculationValue.ynsdse.doubleValue() <= 1000000) {
                    all.put("smallProfitStatus", "Y");
                } else if ((calculationValue.ynsdse.doubleValue() > 1000000 && ("1".equals(currents.get("YJFS")) || "2".equals(currents.get("YJFS")))) || "3".equals(currents.get("YJFS"))) {
                    all.put("smallProfitStatus", "N");
                }
            } else if ("2".equals(currents.get("XXWLBZ")) || "0".equals(currents.get("XXWLBZ"))) {
                if (calculationValue.ynsdse.doubleValue() > 1000000) {
                    all.put("smallProfitStatus", "N");
                } else {
                    if ("1".equals(currents.get("YJFS")) || "2".equals(currents.get("YJFS"))) {
                        all.put("smallProfitStatus", "Y");
                    }
                }
            }
        }

        // 预缴方式1.按照实际利润额预缴;2.按照上一纳税年度应纳税所得额平均额预缴;3.按照税务机关确定的其他方法预缴
        all.put("yjfs", currents.get("YJFS"));
        //企业类型0.一般企业;1.跨地区经营汇总纳税企业总机构;2.跨地区经营汇总纳税企业分支机构
        all.put("qylx", currents.get("SBQYLX"));
        // 小型微利企业
        all.put("sfxxwlqy", all.get("smallProfitStatus"));
        // 科技型中小企业
        all.put("sfkjxzxqy",calculationValue.technologyAdmissionMatter);
        // 高新技术企业
        all.put("sfgxjsqy", calculationValue.highTechnologyCompany);
        // 技术入股递延纳税事项
        all.put("sfjsrkdynssx", calculationValue.technologyAdmissionMatter);
        // 期末从业人数
        all.put("qmcyrs", calculationValue.employeeNumber);
        // 公司名称
        all.put("companyName", clientDataMap.get("clientFullName"));
        // 税期开始时间
        all.put("taxBeginDate", calculationValue.taxStartDate);
        // 税期结束时间
        all.put("taxEndDate", calculationValue.taxStartDate);
        // 税号
        all.put("taxNo", clientDataMap.get("taxNo"));
        // 税期创建时间
        all.put("taxCreatedDate", DateUtils.currentTimeSecs());
        return this;
    }

    private BigDecimal notMinus(BigDecimal num) {
        if (num.doubleValue() < 0) {
            return new BigDecimal("0.00");
        }
        return num;
    }

    private String checkExist(String val) {
        if (StringUtils.isEmpty(val)) {
            return "N";
        }
        if ("1".equals(val)) {
            return "Y";
        }
        return "N";
    }

    private BigDecimal ifNullBigDecimal(Object obj) {
        if (StringUtils.isEmpty(obj)) {
            return new BigDecimal("0.00");
        }
        return new BigDecimal(obj.toString()).setScale(3, BigDecimal.ROUND_HALF_UP).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private Double ifNullDouble(Object obj) {
        if (obj == null) {
            return 0.00;
        }
        return new BigDecimal(obj.toString()).setScale(3, BigDecimal.ROUND_HALF_UP).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
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

    public static class Report001CalculationValue {
        //代开总金额
        public BigDecimal agentAmount = new BigDecimal("0.00");
        //自开总金额
        public BigDecimal outputAmount = new BigDecimal("0.00");
        //不开票金额
        public BigDecimal nobillAmount = new BigDecimal("0.00");
        //营业收入
        public BigDecimal incomeProfitAmount = new BigDecimal("0.00");
        //营业成本
        public BigDecimal costProfitAmount = new BigDecimal("0.00");
        //利润总额
        public BigDecimal profitProfitAmount = new BigDecimal("0.00");
        //汇缴税率
        public BigDecimal taxRate = new BigDecimal("0.25");
        // 总机构分摊比例 zjgftbl
        public BigDecimal zjgftbl = new BigDecimal("0.00");
        // 财政集中分配比例  czjzfpbl
        public BigDecimal czjzfpbl = new BigDecimal("0.00");
        // 全部分支机构分摊比例  qbfzjgftbl
        public BigDecimal qbfzjgftbl = new BigDecimal("0.00");
        //总机构具有主体生产经营职能部门分摊比例  zjgjyztscjyznbmftbl
        public BigDecimal zjgjyztscjyznbmftbl = new BigDecimal("0.00");
        // 应纳所得税额
        public BigDecimal ynsdse = new BigDecimal("0.00");
        // 税期开始时间
        public long taxStartDate = System.currentTimeMillis() / 1000;
        // 税期结束时间
        public long taxEndDate = System.currentTimeMillis() / 1000;

        public int employeeNumber = 0;    //期末从业人数

        public String technologySmallCompany = "N";    // 是否科技型中小企业[Y,N]

        public String highTechnologyCompany = "N";    //是否高新技术企业[Y,N]

        public String technologyAdmissionMatter = "N";    //是否技术入股递延纳税事项[Y,N]

    }
}
