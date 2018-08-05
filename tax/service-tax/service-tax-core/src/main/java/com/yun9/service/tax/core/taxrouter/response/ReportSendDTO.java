package com.yun9.service.tax.core.taxrouter.response;

import com.yun9.biz.report.domain.entity.BizReportInstanceSheet;
import com.yun9.biz.report.domain.entity.BizReportInstanceSheetData;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReportSendDTO {
    /**
     * 报表唯一标识
     */
    private String sheetSn;

    /**
     * 报表数据
     */
    private List<ReportDataDTO> data;

    @Data
    public static class ReportDataDTO {
        private String key;
        private String value;
    }
    public static ReportSendDTO build(BizReportInstanceSheet bizReportInstanceSheet, List<BizReportInstanceSheetData> datas){
        ReportSendDTO reportDTO = new ReportSendDTO();
//        reportDTO.setSheetSn(bizReportInstanceSheet.getSheetSn());
//        reportDTO.setData(toReportDataDTO(datas));
        return  reportDTO;
    }
//    public static List<ReportDataDTO> toReportDataDTO( List<BizReportInstanceSheetData> datas){
//        List<ReportDataDTO> list = new ArrayList<>();
//        datas.forEach(v -> {
//            ReportDataDTO  reportDataDTO = new ReportDataDTO();
//            reportDataDTO.setKey(v.getCode());
//            if (null != v.getValueNum()) {
//                reportDataDTO.setValue(v.getValueNum().toString());
//            } else if (null != v.getValueStr()) {
//                reportDataDTO.setValue(v.getValueStr());
//            } else {
//                reportDataDTO.setValue(null);
//            }
//        });
//        return list;
//    }
}
