package com.yun9.service.tax.core.taxrouter.response;

import com.yun9.biz.report.domain.dto.ReportDataDTO;
//import com.yun9.biz.report.enums.ReportBizSn;
import com.yun9.biz.tax.enums.AlreadyDeclared;
import com.yun9.biz.tax.enums.CycleType;
import com.yun9.biz.tax.enums.DeclareType;
import lombok.Data;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 财务报表回调类
 */
@Data
public class DeclareFrResponse {
    /**
     * yun9, taxOffice 申报的方式是通过税局还是我们自己
     */
    private DeclareType declareType;

    /**
     * 发送申报的时间
     */
    private long sendDate;
    /**
     * 应征凭证序号
     */
    private String taxPaySn;
    /**
     * 应税税款
     */
    private BigDecimal taxPayAmount;
    /**
     * 关联税种信息
     */
    private List<RelationTax> linkedHistory;


    @Data
     public class RelationTax {
        /**
         * 是否已申报
         */
        private AlreadyDeclared alreadyDeclared;
        /**
         * taxCode
         */
        private String taxCode;
        /**
         * 申报所属开始月份时间
         */
        private Long beginDate;
        /**
         * 申报所属结束月份时间
         */
        private Long endDate;
        /**
         * 应税凭证序号
         */
        private String taxPaySn;
        /**
         * 应税税款
         */
        private BigDecimal taxPayAmount;

        /**
         * 申报周期
         */
        private CycleType cycleType;
        /**
         * 申报截止日期
         */
        private Long closeDate;
        /**
         * 发送申报的时间
         */
        private Long sendDate;
        /**
         * 税种名称
         */
        private String name;
        /**
         * 历史数据
         */
        private DeclareResponse.History history;
    }


    @Data
    public static class History {
        /**
         * 主报表唯一标识
         */
//        private ReportBizSn reportBizSn;
        /**
         * 主报表名称
         */
        private String name;
        /**
         * 主报表名称
         */
        private List<ReportDTO> reports;

        public Map<String, List<ReportDataDTO>> convertToReportDataMap() {
            Map<String, List<ReportDataDTO>> map = new HashMap<>();
            if (null != this.reports) {
                this.reports.forEach(v -> {
                    map.put(v.getSheetSn(), v.getReportDataDTOS());
                });
            }
            return map;
        }
    }


    /**
     * 类转Map
     *
     * @param obj 对象
     * @return map
     */
    public static Map<String, String> convertObjToMap(Object obj) {
        Map<String, String> reMap = new HashMap<>();
        if (null == obj) {
            return null;
        }
        Arrays.stream(obj.getClass().getDeclaredFields()).forEach(v -> {
            try {
                if (!"linkedHistory".equals(v.getName()) && !"history".equals(v.getName())) {
                    Field f = obj.getClass().getDeclaredField(v.getName());
                    f.setAccessible(true);
                    Object o = f.get(obj);
                    reMap.put(v.getName(), null != o ? o.toString() : null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
        return reMap;
    }

}
