package com.yun9.service.tax.core.taxrouter.response;

import com.yun9.biz.report.domain.dto.ReportDataDTO;
import com.yun9.biz.tax.domain.entity.BizTaxInstanceCategory;
import com.yun9.biz.tax.enums.TaxRecord;
import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by werewolf on  2018/3/21.
 * 申报清册返回数据
 */
@Data
public class DeclareResponse {
    public enum AlreadyDeclared {
        Y, N;
    }

    private String taxCode;
    //    private TaxCategory taxCategory;
    private AlreadyDeclared alreadyDeclared;
    private Long startDate;
    private Long endDate;
    private Long closeDate;
    private String cycleType;
    private String taxPaySn;
    private TaxRecord taxRecord;
    private BigDecimal taxPayAmount;
    private Map<String, String> taxCustomData;
    private History history;


    public Map<String, String> toBasicMap() {
        Map<String, String> map = new HashMap<>();
        map.put("taxCode", taxCode);
        map.put("alreadyDeclared", alreadyDeclared.toString());
        map.put("startDate", startDate.toString());
        map.put("endDate", endDate.toString());
        map.put("closeDate", closeDate.toString());
        map.put("taxPaySn", taxPaySn == null ? null : taxPaySn);
        map.put("taxPayAmount", taxPayAmount == null ? null : taxPayAmount.toString());
        if (taxCustomData != null) {
            taxCustomData.keySet().forEach(v -> {
                map.put(v, taxCustomData.get(v));
            });
        }
        return map;
    }

    @Data
    public static class History {
        //        private ReportBizSn reportBizSn;
        private String name;
        private List<ReportDTO> data;

        public Map<String, List<ReportDataDTO>> convertToReportDataMap() {
            Map<String, List<ReportDataDTO>> map = new HashMap<>();
            this.data.stream().forEach(v -> {
                map.put(v.getSheetSn(), v.getReportDataDTOS());
            });
            return map;
        }
    }

/*
    {

        "historyList":
        [
            {
                bizSn:"GD_GS_XQY_FR",
                name:"财务报表"
                list:[
                       {
                            reportSn:"GD_GS_XQY_FR_ZCB"
                            name:"广东国税小企业财务报表"
                            data:[
                                 {
                                     "name":"",
                                      "key":"a1",
                                      "value":"111"
                                 }
                              ]
                       }
                 ]
            }
        ]
    }
    */

    public BizTaxInstanceCategory buildTaxInstanceCategory() {
//        BizTaxInstanceCategory bizTaxInstanceCategory = new BizTaxInstanceCategory();
//        //todo
////        bizTaxInstanceCategory.setState(BizTaxInstanceCategory.State.start);
//        bizTaxInstanceCategory.setCloseDate(this.closeDate);
//        bizTaxInstanceCategory.setTaxPaySn(this.taxPaySn);
////        bizTaxInstanceCategory.setTaxOffice(this.taxOffice);
//        bizTaxInstanceCategory.setBeginAccountCycleId(this.startDate);
//        bizTaxInstanceCategory.setEndAccountCycleId(this.endDate);
//
//        return bizTaxInstanceCategory;
//    }
//
//    public static void main(String[] args) {
//        DeclareResponse declareDTO = new DeclareResponse();
//        declareDTO.setAlreadyDeclared(AlreadyDeclared.Y);
//        declareDTO.setCloseDate(12233L);
//        declareDTO.setEndDate(12233L);
//        declareDTO.setStartDate(12233L);
//        declareDTO.setTaxPaySn("1243141");
//        declareDTO.setCycleType(CycleType.y);
//
//
//        List<ReportDTO> reportDTOS = new ArrayList<>();
//        List<ReportDataDTO> reportDataDTOS = new ArrayList<>();
//        ReportDataDTO reportDataDTO = new ReportDataDTO();
//        reportDataDTO.setKey("单元格key");
//        reportDataDTO.setValue("值");
//        reportDataDTOS.add(reportDataDTO);
//
//
//        ReportDTO reportDTO = new ReportDTO();
//        reportDTO.setReportDataDTOS(reportDataDTOS);
//        reportDTO.setName("报表名称");
//        reportDTOS.add(reportDTO);
//        History history = new History();
//        history.setReportBizSn(ReportBizSn.GD_DS_XQY_FR);
//        history.setName("历史数据名称");
//        history.setData(reportDTOS);
//        declareDTO.setHistory(history);
//
//        System.out.println(JSON.toJSONString(declareDTO));
//    }
        return null;

    }
}
